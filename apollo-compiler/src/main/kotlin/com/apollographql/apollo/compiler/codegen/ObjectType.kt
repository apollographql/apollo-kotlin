package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.ir.Field
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

internal fun CodeGenerationAst.ObjectType.typeSpec(
    generateAsInternal: Boolean = false
): TypeSpec {
  return if (kind is CodeGenerationAst.ObjectType.Kind.FragmentDelegate) {
    fragmentDelegateTypeSpec(generateAsInternal)
  } else {
    objectTypeSpec(generateAsInternal)
  }
}

internal fun CodeGenerationAst.ObjectType.objectTypeSpec(
    generateAsInternal: Boolean = false
): TypeSpec {
  val builder = if (abstract) TypeSpec.interfaceBuilder(name) else TypeSpec.classBuilder(name)
  return builder
      .addSuperinterfaces(implements.map { type -> type.asTypeName() })
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .applyIf(!abstract) { addModifiers(KModifier.DATA) }
      .apply { if (description.isNotBlank()) addKdoc("%L\n", description) }
      .applyIf(!abstract) { primaryConstructor(primaryConstructorSpec) }
      .addProperties(
          fields.map { field ->
            field.asPropertySpec(
                initializer = CodeBlock.of(field.name).takeUnless { abstract }
            )
          }
      )
      .applyIf(!abstract) {
        addType(
            TypeSpec
                .companionObjectBuilder()
                .addProperty(responseFieldsPropertySpec(fields))
                .addFunction(fields.toMapperFun(ClassName("", name)))
                .addFunction(ClassName("", name).createMapperFun())
                .build()
        )
      }
      .apply {
        if (kind is CodeGenerationAst.ObjectType.Kind.Fragment) {
          addType(
              fragmentCompanionTypeSpec(
                  defaultImplementation = kind.defaultImplementation,
                  possibleImplementations = kind.possibleImplementations
              )
          )
        }
      }
      .applyIf(!abstract) {
        addFunction(
            fields.marshallerFunSpec(
                thisRef = name,
                // not ideal but there is no better way of doing this
                // we assume if generated class implements any interface, marshaller function is overridden
                override = implements.isNotEmpty()
            )
        )
      }
      .applyIf(abstract) {
        addFunction(
            FunSpec.builder("marshaller")
                .addModifiers(KModifier.ABSTRACT)
                .applyIf(implements.isNotEmpty()) { addModifiers(KModifier.OVERRIDE) }
                .returns(ResponseFieldMarshaller::class)
                .build()
        )
      }
      .build()
}

private fun CodeGenerationAst.ObjectType.fragmentDelegateTypeSpec(
    generateAsInternal: Boolean = false
): TypeSpec {
  val delegateTypeRef = (kind as CodeGenerationAst.ObjectType.Kind.FragmentDelegate).fragmentTypeRef
  val delegateFieldName = "${delegateTypeRef.name.decapitalize()}Delegate"
  val delegateFieldTypeName = delegateTypeRef.asTypeName()
  val primaryConstructorSpec = FunSpec.constructorBuilder()
      .apply {
        addParameter(
            ParameterSpec.builder(name = delegateFieldName, type = delegateFieldTypeName).build()
        )
      }
      .build()
  return TypeSpec.classBuilder(name)
      .addSuperinterfaces(implements.minus(delegateTypeRef).map { type -> type.asTypeName() })
      .addSuperinterface(delegateTypeRef.asTypeName(), delegate = CodeBlock.of("%L", delegateFieldName))
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addModifiers(KModifier.DATA)
      .apply { if (description.isNotBlank()) addKdoc("%L\n", description) }
      .addProperty(
          PropertySpec.builder(name = delegateFieldName, type = delegateFieldTypeName)
              .initializer("%L", delegateFieldName)
              .build()
      )
      .primaryConstructor(primaryConstructorSpec)
      .addType(
          TypeSpec.companionObjectBuilder()
              .addFunction(
                  FunSpec
                      .builder("invoke")
                      .addModifiers(KModifier.OPERATOR)
                      .addParameter(ParameterSpec.builder("reader", ResponseReader::class).build())
                      .returns(ClassName("", name))
                      .addStatement("return·%L(%T(reader))", name, delegateFieldTypeName)
                      .build()
              )
              .build()
      )
      .build()
}

private fun CodeGenerationAst.ObjectType.fragmentCompanionTypeSpec(
    defaultImplementation: CodeGenerationAst.TypeRef,
    possibleImplementations: Map<String, CodeGenerationAst.TypeRef>
): TypeSpec {
  return TypeSpec
      .companionObjectBuilder()
      .addProperty(responseFieldsPropertySpec(listOf(CodeGenerationAst.typenameField)))
      .addFunction(
          FunSpec
              .builder("invoke")
              .addModifiers(KModifier.OPERATOR)
              .addParameter(ParameterSpec.builder("reader", ResponseReader::class).build())
              .returns(ClassName("", name))
              .applyIf(possibleImplementations.isEmpty()) {
                addStatement("return %T(reader)", defaultImplementation.asTypeName())
              }
              .applyIf(possibleImplementations.isNotEmpty()) {
                addStatement("val typename = reader.readString(RESPONSE_FIELDS[0])")
                beginControlFlow("return·when(typename)·{")
                addCode(
                    possibleImplementations
                        .map { (typeCondition, type) -> CodeBlock.of("%S·-> %T(reader)", typeCondition, type.asTypeName()) }
                        .joinToCode(separator = "\n", suffix = "\n")
                )
                addStatement("else·->·%T(reader)", defaultImplementation.asTypeName())
                endControlFlow()
              }
              .build()
      )
      .build()
}

private val CodeGenerationAst.ObjectType.primaryConstructorSpec: FunSpec
  get() {
    return FunSpec.constructorBuilder()
        .addParameters(fields.map { field ->
          ParameterSpec
              .builder(
                  name = field.name,
                  type = field.type.asTypeName()
              )
              .applyIf(
                  field.responseName == Field.TYPE_NAME_FIELD.fieldName &&
                      field.type is CodeGenerationAst.FieldType.Scalar.String &&
                      schemaType != null) {
                defaultValue("%S", schemaType)
              }
              .build()
        })
        .build()
  }
