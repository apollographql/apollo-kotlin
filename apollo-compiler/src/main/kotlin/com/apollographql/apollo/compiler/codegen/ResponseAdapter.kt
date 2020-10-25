package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.CodeGenerationAst
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

internal fun CodeGenerationAst.OperationType.responseAdapterTypeSpec(generateAsInternal: Boolean = false): TypeSpec {
  val responseDataType = checkNotNull(this.dataType.nestedTypes[this.dataType.rootType]) {
    "Failed to resolve operation root data type"
  }
  return TypeSpec.objectBuilder("${this.name}_ResponseAdapter")
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addAnnotation(suppressWarningsAnnotation)
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(this.dataType.rootType.asTypeName()))
      .addProperty(responseFieldsPropertySpec(responseDataType.fields))
      .addFunction(responseDataType.readFromResponseFunSpec())
      .addFunction(responseDataType.writeToResponseFunSpec())
      .addTypes(
        this.dataType
            .nestedTypes
            .minus(this.dataType.rootType)
            .filterNot { (_, type) -> type.abstract && type.kind !is CodeGenerationAst.ObjectType.Kind.Fragment }
            .map { (_, type) -> type.responseAdapterTypeSpec() }
      )
      .build()
}

internal fun CodeGenerationAst.FragmentType.responseAdapterTypeSpec(generateAsInternal: Boolean = false): TypeSpec {
  val defaultImplementationType = checkNotNull(this.nestedTypes[this.defaultImplementation]) {
    "Failed to resolve fragment default implementation type"
  }
  return TypeSpec.objectBuilder(this.rootType.asAdapterTypeName().simpleName)
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addAnnotation(suppressWarningsAnnotation)
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(this.defaultImplementation.asTypeName()))
      .addProperty(responseFieldsPropertySpec(defaultImplementationType.fields))
      .addFunction(defaultImplementationType.readFromResponseFunSpec())
      .addFunction(defaultImplementationType.writeToResponseFunSpec())
      .addTypes(
        this.nestedTypes
            .minus(this.rootType)
            .minus(this.defaultImplementation)
            .filterNot { (_, type) -> type.abstract && type.kind !is CodeGenerationAst.ObjectType.Kind.Fragment }
            .map { (_, type) -> type.responseAdapterTypeSpec() }
      )
      .build()
}

private fun CodeGenerationAst.ObjectType.responseAdapterTypeSpec(): TypeSpec {
  return TypeSpec.objectBuilder("${this.name}_ResponseAdapter")
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(this.typeRef.asTypeName()))
      .applyIf(this.fields.isNotEmpty()) { addProperty(responseFieldsPropertySpec(this@responseAdapterTypeSpec.fields)) }
      .addFunction(readFromResponseFunSpec())
      .addFunction(writeToResponseFunSpec())
      .build()
}

internal fun CodeGenerationAst.TypeRef.asAdapterTypeName(): ClassName {
  return if (enclosingType == null) {
    ClassName(packageName = packageName, "${this.name}_ResponseAdapter")
  } else {
    ClassName(packageName = packageName, enclosingType.asAdapterTypeName().simpleName, "${this.name}_ResponseAdapter")
  }
}

private fun responseFieldsPropertySpec(fields: List<CodeGenerationAst.Field>): PropertySpec {
  val initializer = CodeBlock.builder()
      .addStatement("arrayOf(")
      .indent()
      .add(fields.map { field -> field.responseFieldInitializerCode }.joinToCode(separator = ",\n"))
      .unindent()
      .addStatement("")
      .add(")")
      .build()
  return PropertySpec
      .builder(
        name = "RESPONSE_FIELDS",
        type = Array<ResponseField>::class.asClassName().parameterizedBy(ResponseField::class.asClassName()),
        modifiers = arrayOf(KModifier.PRIVATE)
      )
      .initializer(initializer)
      .build()
}

private val CodeGenerationAst.Field.responseFieldInitializerCode: CodeBlock
  get() {
    val factoryMethod = when (type) {
      is CodeGenerationAst.FieldType.Scalar -> when (type) {
        is CodeGenerationAst.FieldType.Scalar.ID -> "forString"
        is CodeGenerationAst.FieldType.Scalar.String -> "forString"
        is CodeGenerationAst.FieldType.Scalar.Int -> "forInt"
        is CodeGenerationAst.FieldType.Scalar.Boolean -> "forBoolean"
        is CodeGenerationAst.FieldType.Scalar.Float -> "forDouble"
        is CodeGenerationAst.FieldType.Scalar.Enum -> "forEnum"
        is CodeGenerationAst.FieldType.Scalar.Custom -> "forCustomType"
      }
      is CodeGenerationAst.FieldType.Object -> "forObject"
      is CodeGenerationAst.FieldType.Array -> "forList"
    }

    val builder = CodeBlock.builder().add("%T.%L", ResponseField::class, factoryMethod)
    when {
      type is CodeGenerationAst.FieldType.Scalar && type is CodeGenerationAst.FieldType.Scalar.Custom -> {
        builder.add("(%S,·%S,·%L,·%L,·%T,·%L)", responseName, schemaName, arguments.takeIf { it.isNotEmpty() }.toCode(), type.nullable,
          type.customEnumType.asTypeName(), conditionsListCode(conditions))
      }
      else -> {
        builder.add("(%S,·%S,·%L,·%L,·%L)", responseName, schemaName, arguments.takeIf { it.isNotEmpty() }.toCode(), type.nullable,
          conditionsListCode(conditions))
      }
    }
    return builder.build()
  }

private fun conditionsListCode(conditions: Set<CodeGenerationAst.Field.Condition>): CodeBlock {
  return conditions
      .map { condition ->
        when (condition) {
          is CodeGenerationAst.Field.Condition.Directive -> CodeBlock.of("%T.booleanCondition(%S,·%L)",
            ResponseField.Condition::class, condition.variableName, condition.inverted)
        }
      }
      .joinToCode(separator = ",\n")
      .let {
        if (conditions.isEmpty()) {
          CodeBlock.of("null")
        } else {
          CodeBlock.builder()
              .add("listOf(\n")
              .indent().add(it).unindent()
              .add("\n)")
              .build()
        }
      }
}

private fun Any?.toCode(): CodeBlock {
  return when {
    this == null -> CodeBlock.of("null")
    this is Map<*, *> && this.isEmpty() -> CodeBlock.of("emptyMap<%T,·Any>()", String::class.asTypeName())
    this is Map<*, *> -> CodeBlock.builder()
        .add("mapOf<%T,·Any>(\n", String::class.asTypeName())
        .indent()
        .add(map { CodeBlock.of("%S to %L", it.key, it.value.toCode()) }.joinToCode(separator = ",\n"))
        .unindent()
        .add(")")
        .build()
    this is List<*> && this.isEmpty() -> CodeBlock.of("emptyList<Any>()")
    this is List<*> -> CodeBlock.builder()
        .add("listOf<Any>(\n")
        .indent()
        .add(map { it.toCode() }.joinToCode(separator = ",\n"))
        .unindent()
        .add(")")
        .build()
    this is String -> CodeBlock.of("%S", this)
    this is Number -> CodeBlock.of("%L", this)
    this is Boolean -> CodeBlock.of("%L", this)
    else -> throw IllegalStateException("Cannot generate code for $this")
  }
}
