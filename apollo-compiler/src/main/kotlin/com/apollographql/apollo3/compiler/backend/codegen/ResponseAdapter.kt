package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.api.BooleanResponseAdapter
import com.apollographql.apollo3.api.DoubleResponseAdapter
import com.apollographql.apollo3.api.IntResponseAdapter
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.Variable
import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.StringResponseAdapter
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

internal fun CodeGenerationAst.OperationType.responseAdapterTypeSpec(
    generateFragmentsAsInterfaces: Boolean,
): TypeSpec {
  return this.dataType
      .copy(name = "${this.name.escapeKotlinReservedWord()}_ResponseAdapter")
      .rootResponseAdapterTypeSpec(
          generateFragmentsAsInterfaces = generateFragmentsAsInterfaces
      )
}

internal fun CodeGenerationAst.FragmentType.responseAdapterTypeSpec(
    generateFragmentsAsInterfaces: Boolean,
): TypeSpec {
  val dataType = this.implementationType.nestedObjects.single()
  return dataType
      .copy(name = "${this.implementationType.name.escapeKotlinReservedWord()}_ResponseAdapter")
      .rootResponseAdapterTypeSpec(
          generateFragmentsAsInterfaces = generateFragmentsAsInterfaces
      )
}

/**
 * @param generateFragmentsAsInterfaces: whether to generate the rewind() code or not
 */
private fun CodeGenerationAst.ObjectType.rootResponseAdapterTypeSpec(
    generateFragmentsAsInterfaces: Boolean
): TypeSpec {
  val adapterTypeSpec = this.responseAdapterTypeSpec(
      generateFragmentsAsInterfaces = generateFragmentsAsInterfaces
  )
      .toBuilder()
      .addAnnotation(suppressWarningsAnnotation)
      .build()

  return adapterTypeSpec
}

private fun CodeGenerationAst.ObjectType.responseAdapterTypeSpec(generateFragmentsAsInterfaces: Boolean): TypeSpec {
  return TypeSpec.objectBuilder(this.name)
      .applyIf(!isShape) { addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(this@responseAdapterTypeSpec.typeRef.asTypeName())) }
      .apply {
        if (fields.isNotEmpty()) {
          if (kind is CodeGenerationAst.ObjectType.Kind.Object ||
              (kind is CodeGenerationAst.ObjectType.Kind.ObjectWithFragments && !generateFragmentsAsInterfaces)) {
            addProperty(responseFieldsPropertySpec(this@responseAdapterTypeSpec))
            addProperty(responseNamesPropertySpec())
          }
        }
      }
      .addFunction(readFromResponseFunSpec(generateFragmentsAsInterfaces))
      .addFunction(writeToResponseFunSpec(generateFragmentsAsInterfaces))
      .addTypes(
          this.nestedObjects.mapNotNull { nestedObject ->
            if (nestedObject.kind is CodeGenerationAst.ObjectType.Kind.Object ||
                (nestedObject.kind is CodeGenerationAst.ObjectType.Kind.ObjectWithFragments && nestedObject.kind.possibleImplementations.isNotEmpty())) {
              nestedObject.responseAdapterTypeSpec(
                  generateFragmentsAsInterfaces = generateFragmentsAsInterfaces
              )
            } else null
          }
      )
      .build()
}

internal fun CodeGenerationAst.TypeRef.asAdapterTypeName(): ClassName {

  return when {
    // We are called on a root typeRef such as TestQuery, just add '_ResponseAdapter'
    enclosingType == null -> {
      ClassName(packageName = "$packageName.adapter", "${this.name.escapeKotlinReservedWord()}_ResponseAdapter")
    }
    // We are called on a data typeRef such as TestQuery.Data, skip Data
    enclosingType.enclosingType == null -> {
      ClassName(packageName = "$packageName.adapter", "${this.enclosingType.name.escapeKotlinReservedWord()}_ResponseAdapter")
    }
    else -> {
      ClassName(packageName = "$packageName.adapter", enclosingType.asAdapterTypeName().simpleNames + this.name.escapeKotlinReservedWord())
    }
  }
}

internal fun CodeGenerationAst.TypeRef.asEnumAdapterTypeName(): ClassName {
  return ClassName(packageName = packageName, "${this.name.escapeKotlinReservedWord()}_ResponseAdapter")
}

internal fun CodeGenerationAst.TypeRef.asInputAdapterTypeName(): ClassName {
  return ClassName(packageName = "$packageName.adapter", kotlinNameForInputObjectAdapter(this.name))
}

private fun responseFieldsPropertySpec(objectType: CodeGenerationAst.ObjectType): PropertySpec {
  val fields = objectType.fields

  val initializer = CodeBlock.builder()
      .addStatement("arrayOf(")
      .indent()
      .also { builder ->
        builder.add(fields.map { field -> field.responseFieldInitializerCode(objectType) }.joinToCode(separator = ",\n"))
      }
      .unindent()
      .addStatement("")
      .add(")")
      .build()
  return PropertySpec
      .builder(
          name = "RESPONSE_FIELDS",
          type = Array::class.asClassName().parameterizedBy(
              ResponseField::class.asClassName(),
          ),
      )
      .initializer(initializer)
      .build()
}

private fun responseNamesPropertySpec(): PropertySpec {
  return PropertySpec
      .builder(
          name = "RESPONSE_NAMES",
          type = List::class.asClassName().parameterizedBy(
              String::class.asClassName(),
          ),
      )
      .initializer("RESPONSE_FIELDS.map { it.responseName }")
      .build()
}

private fun CodeGenerationAst.FieldType.toTypeCode(): CodeBlock {
  val builder = CodeBlock.Builder()

  when (this) {
    is CodeGenerationAst.FieldType.Object -> {
      builder.add("%T(%S)", ResponseField.Type.Named.Object::class.java, schemaTypeName)
    }
    is CodeGenerationAst.FieldType.Scalar -> {
      // same code as Object but in a separate branch so that type inference can find schemaTypeName
      builder.add("%T(%S)", ResponseField.Type.Named.Other::class.java, schemaTypeName)
    }
    is CodeGenerationAst.FieldType.Array -> {
      builder.add("%T(", ResponseField.Type.List::class.java)
      builder.add(this.rawType.toTypeCode())
      builder.add(")")
    }
  }

  val block = builder.build()

  return if (!nullable) {
    block.toNonNullable()
  } else
    block
}

private fun CodeBlock.toNonNullable(): CodeBlock {
  val builder = CodeBlock.builder()
  builder.add("%T(", ResponseField.Type.NotNull::class.java)
  builder.add(this)
  builder.add(")")
  return builder.build()
}

private fun CodeGenerationAst.Field.responseFieldInitializerCode(objectType: CodeGenerationAst.ObjectType): CodeBlock {
  if (responseName == "__typename" && schemaName == "__typename") {
    return CodeBlock.of("%T.Typename", ResponseField::class.asTypeName())
  }
  val builder = CodeBlock.builder().add("%T(\n", ResponseField::class)
  builder.indent()
  builder.add("type = %L,\n", type.toTypeCode())
  builder.add("fieldName = %S,\n", schemaName)
  if (responseName != schemaName) {
    builder.add("responseName = %S,\n", responseName)
  }
  if (arguments.isNotEmpty()) {
    builder.add("arguments = %L,\n", arguments.takeIf { it.isNotEmpty() }?.let { valueToCode(it) } ?: "emptyMap()")
  }
  if (conditions.isNotEmpty()) {
    builder.add("conditions = %L,\n", conditionsListCode(conditions))
  }
  if (this.type.leafType() is CodeGenerationAst.FieldType.Object) {
    builder.add("fieldSets = %L,\n", fieldSetsCode(this.type, objectType))
  }
  builder.unindent()
  builder.add(")")

  return builder.build()
}

private fun fieldSetsCode(type: CodeGenerationAst.FieldType, objectType: CodeGenerationAst.ObjectType): CodeBlock {
  return when (val leafType = type.leafType()) {
    is CodeGenerationAst.FieldType.Scalar -> CodeBlock.of("emptyList()")
    is CodeGenerationAst.FieldType.Object -> {
      // Find the first nestedObject that has the type of this field.
      val nestedObjectType = objectType.nestedObjects.first { it.typeRef == leafType.typeRef }
      val builder = CodeBlock.Builder()
      builder.add("listOf(\n")
      builder.indent()
      when (val kind = nestedObjectType.kind) {
        is CodeGenerationAst.ObjectType.Kind.Object -> {
          builder.add("%T(null, %T.RESPONSE_FIELDS)\n", ResponseField.FieldSet::class, leafType.typeRef.asAdapterTypeName())
        }
        is CodeGenerationAst.ObjectType.Kind.ObjectWithFragments -> {
          kind.possibleImplementations.forEach { (possibleTypes, typeRef) ->
            possibleTypes.forEach { possibleType ->
              builder.add("%T(%S, %T.RESPONSE_FIELDS),\n", ResponseField.FieldSet::class, possibleType, typeRef.asAdapterTypeName())
            }
          }
          if (kind.defaultImplementation != null) {
            builder.add("%T(null, %T.RESPONSE_FIELDS),\n", ResponseField.FieldSet::class, kind.defaultImplementation.asAdapterTypeName())
          }
        }
      }
      builder.unindent()
      builder.add(")")
      builder.build()
    }
    else -> error("")
  }
}

private fun CodeGenerationAst.FieldType.leafType(): CodeGenerationAst.FieldType = when (this) {
  is CodeGenerationAst.FieldType.Scalar -> this
  is CodeGenerationAst.FieldType.Object -> this
  is CodeGenerationAst.FieldType.InputObject -> this
  is CodeGenerationAst.FieldType.Array -> rawType.leafType()
}

private fun conditionsListCode(conditions: Set<CodeGenerationAst.Field.Condition>): CodeBlock {
  return conditions
      .map { condition ->
        when (condition) {
          is CodeGenerationAst.Field.Condition.Directive -> CodeBlock.of("%T.booleanCondition(%S,·%L)",
              ResponseField.Condition::class, condition.variableName.escapeKotlinReservedWord(), condition.inverted)
        }
      }
      .joinToCode(separator = ",\n")
      .let {
        if (conditions.isEmpty()) {
          CodeBlock.of("emptyList()")
        } else {
          CodeBlock.builder()
              .add("listOf(\n")
              .indent().add(it).unindent()
              .add("\n)")
              .build()
        }
      }
}

/**
 * This is the symmetric call to [com.apollographql.apollo3.compiler.frontend.toKotlinValue]
 *
 * any can be:
 * - Int
 * - Double
 * - Boolean
 * - String
 * - Map
 * - List
 * - String for Enum
 * - [com.apollographql.apollo3.api.Variable] for variables
 */
private fun valueToCode(any: Any?): CodeBlock {
  return with(any) {
    when {
      this == null -> CodeBlock.of("null")
      this is Map<*, *> && this.isEmpty() -> CodeBlock.of("emptyMap<%T,·Any?>()", String::class.asTypeName())
      this is Map<*, *> -> CodeBlock.builder()
          .add("mapOf<%T,·Any?>(\n", String::class.asTypeName())
          .indent()
          .add(map { CodeBlock.of("%S to %L", it.key, valueToCode(it.value)) }.joinToCode(separator = ",\n"))
          .unindent()
          .add(")")
          .build()
      this is List<*> && this.isEmpty() -> CodeBlock.of("emptyList<Any?>()")
      this is List<*> -> CodeBlock.builder()
          .add("listOf<Any?>(\n")
          .indent()
          .add(map { valueToCode(it) }.joinToCode(separator = ",\n"))
          .unindent()
          .add(")")
          .build()
      this is String -> CodeBlock.of("%S", this)
      this is Number -> CodeBlock.of("%L", this)
      this is Boolean -> CodeBlock.of("%L", this)
      this is Variable -> CodeBlock.of("%T(%S)", Variable::class, name)
      else -> throw IllegalStateException("Cannot generate code for $this")
    }
  }
}

private fun nullableScalarAdapter(name: String) = CodeBlock.of("%M", MemberName("com.apollographql.apollo3.api", name))

internal fun adapterInitializer(type: CodeGenerationAst.FieldType, buffered: Boolean): CodeBlock {
  if (type.nullable) {
    return when (type) {
      is CodeGenerationAst.FieldType.Scalar.ID -> nullableScalarAdapter("NullableStringResponseAdapter")
      is CodeGenerationAst.FieldType.Scalar.Boolean -> nullableScalarAdapter("NullableBooleanResponseAdapter")
      is CodeGenerationAst.FieldType.Scalar.String -> nullableScalarAdapter("NullableStringResponseAdapter")
      is CodeGenerationAst.FieldType.Scalar.Int -> nullableScalarAdapter("NullableIntResponseAdapter")
      is CodeGenerationAst.FieldType.Scalar.Float -> nullableScalarAdapter("NullableDoubleResponseAdapter")
      else -> {
        val nullableFun = MemberName("com.apollographql.apollo3.api", "nullable")
        CodeBlock.of("%L.%M()", adapterInitializer(type.nonNullable(), buffered), nullableFun)
      }
    }
  }
  return when (type) {
    is CodeGenerationAst.FieldType.Array -> {
      val nullableFun = MemberName("com.apollographql.apollo3.api", "list")
      CodeBlock.of("%L.%M()", adapterInitializer(type.rawType, buffered), nullableFun)
    }
    is CodeGenerationAst.FieldType.Scalar.Boolean -> CodeBlock.of("%T", BooleanResponseAdapter::class)
    is CodeGenerationAst.FieldType.Scalar.ID -> CodeBlock.of("%T", StringResponseAdapter::class)
    is CodeGenerationAst.FieldType.Scalar.String -> CodeBlock.of("%T", StringResponseAdapter::class)
    is CodeGenerationAst.FieldType.Scalar.Int -> CodeBlock.of("%T", IntResponseAdapter::class)
    is CodeGenerationAst.FieldType.Scalar.Float -> CodeBlock.of("%T", DoubleResponseAdapter::class)
    is CodeGenerationAst.FieldType.Scalar.Enum -> CodeBlock.of("%T", type.typeRef.asEnumAdapterTypeName().copy(nullable = false))
    is CodeGenerationAst.FieldType.Object -> {
      CodeBlock.of("%T", type.typeRef.asAdapterTypeName().copy(nullable = false)).obj(buffered)
    }
    is CodeGenerationAst.FieldType.InputObject -> {
      CodeBlock.of("%T", type.typeRef.asInputAdapterTypeName().copy(nullable = false)).obj(buffered)
    }
    is CodeGenerationAst.FieldType.Scalar.Custom -> CodeBlock.of(
        "responseAdapterCache.responseAdapterFor<%T>(%T)",
        ClassName.bestGuess(type.type),
        type.typeRef.asTypeName()
    )
  }
}

internal fun CodeBlock.obj(buffered: Boolean): CodeBlock {
  return CodeBlock.Builder()
      .add("%L", this)
      .add(
          ".%M(%L)",
          MemberName("com.apollographql.apollo3.api", "obj"),
          if (buffered) "true" else ""
      ).build()
}

internal fun adapterFunSpec(operationResponseAdapter: ClassName, buffered: Boolean): FunSpec {
  return FunSpec.builder("adapter")
      .addModifiers(KModifier.OVERRIDE)
      .returns(ResponseAdapter::class.asClassName().parameterizedBy(ClassName(packageName = "", "Data")))
      .addCode(CodeBlock.of("return·%T", operationResponseAdapter).obj(buffered))
      .build()
}
