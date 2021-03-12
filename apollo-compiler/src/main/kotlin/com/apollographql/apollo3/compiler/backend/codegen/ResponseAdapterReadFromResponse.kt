package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.internal.json.MapJsonReader
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.__typename
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.reader
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.responseAdapterCache
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

/**
 * A top level method that generates the different read functions
 *
 * @param addFromFields: set to true to generate an additional `fromFields`
 * method that will read fields without calling [JsonReader.beginObject]
 */
internal fun CodeGenerationAst.ObjectType.readFromResponseFunSpecs(
    generateFragmentsAsInterfaces: Boolean,
    addFromFields: Boolean,
): List<FunSpec> {
  if (kind is CodeGenerationAst.ObjectType.Kind.FragmentDelegate) {
    return listOf(readFragmentDelegateFromResponseFunSpec())
  }

  val override: Boolean
  val withTypeName: Boolean
  if (isTypeCase && generateFragmentsAsInterfaces) {
    // This is a type case. Since we can't rewind, we have to pass the typename
    // to sub adapter
    override = false
    withTypeName = true
  } else if (addFromFields) {
    // We are generating fromFields, this never overides
    // And we can re-read typename if needed
    override = false
    withTypeName = false
  } else {
    // general case
    override = true
    withTypeName = false
  }

  val body = when (this.kind) {
    is CodeGenerationAst.ObjectType.Kind.ObjectWithFragments -> {
      if (generateFragmentsAsInterfaces) {
        readStreamedPolymorphicObjectCode(this)
      } else {
        readBufferedPolymorphicObjectCode(this)
      }
    }
    else -> {
      readObjectCode(
          fields = fields,
          returnTypeRef = typeRef,
          initializeTypename = withTypeName
      )
    }
  }

  val functionName: String
  val wrappedBody: CodeBlock

  if (addFromFields) {
    functionName = "fromFields"
    wrappedBody = body
  } else {
    functionName = "fromResponse"
    wrappedBody = CodeBlock.builder()
        .addStatement("reader.beginObject()")
        .add(body)
        .addStatement("reader.endObject()")
        .build()
  }

  val list = mutableListOf<FunSpec>()
  if (addFromFields) {
    list.add(readFromResponseWrapperFunSpec(typeRef))
  }
  list.add(FunSpec.builder(functionName)
      .returns(typeRef.asTypeName())
      .addParameter("reader", JsonReader::class)
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .applyIf(withTypeName) {
        addParameter(__typename, String::class.asTypeName().copy(nullable = true))
      }
      .applyIf(override) {
        addModifiers(KModifier.OVERRIDE)
      }
      .addCode(wrappedBody)
      .build()
  )

  return list
}

internal fun readFromResponseWrapperFunSpec(typeRef: CodeGenerationAst.TypeRef): FunSpec {
  return FunSpec.builder("fromResponse")
      .returns(typeRef.asTypeName())
      .addParameter("reader", JsonReader::class)
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addModifiers(KModifier.OVERRIDE)
      .addStatement("reader.beginObject()")
      .addStatement("return·fromFields($reader, $responseAdapterCache).also { reader.endObject() }")
      .build()
}

/**
 * Creates a function body that reads an object from a JsonReader. This is the core of the generated parsers
 *
 * @param functionName: the function name. Most of the times, it's fromResponse except when it's called after a rewind() and it shouldn't
 * @param override: whether this needs the override modifier. This is not always the case. For an example in polymorphic cases
 * @param initializeTypename: whether to generate a method that takes an additional __typename argument from the enclosing type case
 * call readObject()
 * @param readObject: whether to read the object
 */
internal fun readObjectCode(
    fields: List<CodeGenerationAst.Field>,
    returnTypeRef: CodeGenerationAst.TypeRef,
    initializeTypename: Boolean,
): CodeBlock {
  val prefix = prefixCode(fields, initializeTypename)

  val loop = loopCode(fields)

  val suffix = CodeBlock.builder()
      .addStatement("return·%T(", returnTypeRef.asTypeName())
      .indent()
      .add(fields.map { field ->
        CodeBlock.of(
            "%L·=·%L%L",
            field.name.escapeKotlinReservedWord(),
            field.name.escapeKotlinReservedWord(),
            "!!".takeUnless { field.type.nullable } ?: ""
        )
      }.joinToCode(separator = ",\n", suffix = "\n"))
      .unindent()
      .addStatement(")")
      .build()

  return CodeBlock.builder()
      .add(prefix)
      .add(loop)
      .add(suffix)
      .build()
}

internal fun readStreamedPolymorphicObjectCode(objectType: CodeGenerationAst.ObjectType): CodeBlock {
  val (defaultImplementation, possibleImplementations) = with(objectType.kind as CodeGenerationAst.ObjectType.Kind.ObjectWithFragments) {
    defaultImplementation to possibleImplementations
  }
  return CodeBlock.builder()
      .addStatement("check(reader.nextName() == \"__typename\")")
      // calling nextString() directly here certainly breaks the optimization to read fields in order
      .addStatement("val·typename·=·reader.nextString()!!")
      .beginControlFlow("return·when(typename)")
      .add(
          possibleImplementations
              .flatMap { fragmentImplementation ->
                fragmentImplementation.typeConditions.map { typeCondition ->
                  CodeBlock.of(
                      "%S·->·%T.fromResponse(reader,·$responseAdapterCache,·typename)",
                      typeCondition,
                      fragmentImplementation.typeRef.asAdapterTypeName(),
                  )
                }
              }
              .joinToCode(separator = "\n", suffix = "\n")
      )
      .addStatement(
          "else·->·%T.fromResponse(reader,·$responseAdapterCache,·typename)",
          defaultImplementation!!.asAdapterTypeName()
      )
      .endControlFlow()
      .build()
}


private fun prefixCode(fields: List<CodeGenerationAst.Field>, withTypeName: Boolean): CodeBlock {
  return fields.map { field ->
    CodeBlock.of(
        "var·%L:·%T·=·%L",
        field.name.escapeKotlinReservedWord(),
        field.type.asTypeName().copy(nullable = true),
        if (withTypeName && field.responseName == "__typename") "__typename" else "null"
    )
  }.joinToCode(separator = "\n", suffix = "\n")
}

private fun loopCode(fields: List<CodeGenerationAst.Field>): CodeBlock {
  return CodeBlock.builder()
      .beginControlFlow("while(true)")
      .beginControlFlow("when·(reader.selectName(RESPONSE_NAMES))")
      .add(
          fields.mapIndexed { fieldIndex, field ->
            CodeBlock.of(
                "%L·->·%L·=·%L.fromResponse(reader, $responseAdapterCache)",
                fieldIndex,
                field.name.escapeKotlinReservedWord(),
                adapterInitializer(field.type)
            )
          }.joinToCode(separator = "\n", suffix = "\n")
      )
      .addStatement("else -> break")
      .endControlFlow()
      .endControlFlow()
      .build()
}

internal fun readBufferedPolymorphicObjectCode(objectType: CodeGenerationAst.ObjectType): CodeBlock {
  val possibleImplementations = (objectType.kind as CodeGenerationAst.ObjectType.Kind.ObjectWithFragments).possibleImplementations

  val fragmentVariablesCode = possibleImplementations.map { fragmentImplementation ->
    CodeBlock.of(
        "var·%L:·%T·=·null",
        fragmentImplementation.typeRef.fragmentPropertyName(),
        fragmentImplementation.typeRef.asTypeName().copy(nullable = true)
    )
  }.joinToCode(separator = "\n", suffix = "\n")

  val readFragmentsCode = possibleImplementations.map { fragmentImplementation ->
    val possibleTypenamesArray = fragmentImplementation.typeConditions
        .map { CodeBlock.of("%S", it) }
        .joinToCode(separator = ", ")
    CodeBlock.builder()
        .beginControlFlow("if·(__typename·in·arrayOf(%L))", possibleTypenamesArray)
        .addStatement("reader.rewind()")
        .run {
          if (fragmentImplementation.typeRef.isNamedFragmentDataRef) {
            addStatement(
                "%L·=·%T.fromFields(reader,·$responseAdapterCache)",
                fragmentImplementation.typeRef.fragmentPropertyName(),
                fragmentImplementation.typeRef.enclosingType!!.asAdapterTypeName()
            )
          } else {
            addStatement(
                "%L·=·%T.fromFields(reader,·$responseAdapterCache)",
                fragmentImplementation.typeRef.fragmentPropertyName(),
                fragmentImplementation.typeRef.asAdapterTypeName()
            )
          }
        }
        .endControlFlow()
        .build()
  }.joinToCode(separator = "", suffix = "\n")

  val typeConstructorCode = CodeBlock.builder()
      .addStatement("return·%T(", objectType.typeRef.asTypeName())
      .indent()
      .add(
          objectType.fields.map { field ->
            CodeBlock.of(
                "%L·=·%L%L",
                field.name.escapeKotlinReservedWord(),
                field.name.escapeKotlinReservedWord(),
                "!!".takeUnless { field.type.nullable } ?: ""
            )
          }.joinToCode(separator = ",\n", suffix = ",\n")
      )
      .add(
          possibleImplementations.map { fragmentImplementation ->
            val propertyName = fragmentImplementation.typeRef.fragmentPropertyName()
            CodeBlock.of("%L·=·%L", propertyName, propertyName)
          }.joinToCode(separator = ",\n", suffix = "\n")
      )
      .unindent()
      .addStatement(")")
      .build()

  return CodeBlock.builder()
      .addStatement("val·reader·=·reader.%M()", MemberName(MapJsonReader.Companion::class.asClassName(), "buffer"))
      .add(prefixCode(objectType.fields, false))
      .addStatement("")
      .add(fragmentVariablesCode)
      .add(loopCode(objectType.fields))
      .add(readFragmentsCode)
      .add(typeConstructorCode)
      .build()
}

internal fun CodeGenerationAst.ObjectType.readFragmentDelegateFromResponseFunSpec(): FunSpec {
  val fragmentRef = (this.kind as CodeGenerationAst.ObjectType.Kind.FragmentDelegate).fragmentTypeRef
  return FunSpec.builder("fromResponse")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec.builder("reader", JsonReader::class).build())
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addParameter(CodeGenerationAst.typenameField.asOptionalParameterSpec(withDefaultValue = false))
      .returns(this.typeRef.asTypeName())
      .addStatement(
          "return·%T(%T.fromResponse(reader,·%L))",
          this.typeRef.asTypeName(),
          fragmentRef.enclosingType!!.asAdapterTypeName(),
          CodeGenerationAst.typenameField.responseName.escapeKotlinReservedWord()
      )
      .build()
}

