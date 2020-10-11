package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.CodeGenerationAst
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

internal fun CodeGenerationAst.FieldType.asTypeName(): TypeName {
  return when (this) {
    is CodeGenerationAst.FieldType.Scalar -> when (this) {
      is CodeGenerationAst.FieldType.Scalar.ID -> ClassName.bestGuess(type)
      is CodeGenerationAst.FieldType.Scalar.String -> String::class.asClassName()
      is CodeGenerationAst.FieldType.Scalar.Int -> INT
      is CodeGenerationAst.FieldType.Scalar.Boolean -> BOOLEAN
      is CodeGenerationAst.FieldType.Scalar.Float -> DOUBLE
      is CodeGenerationAst.FieldType.Scalar.Enum -> typeRef.asTypeName()
      is CodeGenerationAst.FieldType.Scalar.Custom -> ClassName.bestGuess(type)
    }
    is CodeGenerationAst.FieldType.Object -> typeRef.asTypeName()
    is CodeGenerationAst.FieldType.Array -> List::class.asClassName().parameterizedBy(rawType.asTypeName())
  }.copy(nullable = nullable)
}

internal fun CodeGenerationAst.TypeRef.asTypeName(): TypeName {
  return if (enclosingType == null) {
    ClassName(packageName, name)
  } else {
    ClassName(packageName, enclosingType.name, name)
  }
}

internal fun Any.toDefaultValueCodeBlock(typeName: TypeName, fieldType: CodeGenerationAst.FieldType): CodeBlock {
  return when {
    this is Number -> CodeBlock.of("%L%L", castTo(typeName), if (typeName == LONG) "L" else "")
    fieldType is CodeGenerationAst.FieldType.Scalar.Enum -> CodeBlock.of("%T.safeValueOf(%S)", typeName.copy(nullable = false), this)
    fieldType is CodeGenerationAst.FieldType.Array -> {
      @Suppress("UNCHECKED_CAST")
      (this as List<Any>).toDefaultValueCodeBlock(typeName, fieldType.rawType)
    }
    this !is String -> CodeBlock.of("%L", this)
    else -> CodeBlock.of("%S", this)
  }
}

private fun List<Any>.toDefaultValueCodeBlock(typeName: TypeName, fieldType: CodeGenerationAst.FieldType): CodeBlock {
  return if (isEmpty()) {
    CodeBlock.of("emptyList()")
  } else {
    filterNotNull()
        .map { value ->
          val rawTypeName = (typeName as ParameterizedTypeName).typeArguments.first().copy(nullable = false)
          value.toDefaultValueCodeBlock(rawTypeName, fieldType)
        }
        .joinToCode(prefix = "listOf(", separator = ", ", suffix = ")")
  }
}

internal fun deprecatedAnnotation(message: String) = AnnotationSpec
    .builder(Deprecated::class)
    .apply {
      if (message.isNotBlank()) {
        addMember("message = %S", message)
      }
    }
    .build()


internal fun CodeGenerationAst.Field.asPropertySpec(initializer: CodeBlock? = null): PropertySpec {
  return PropertySpec
      .builder(
          name = name,
          type = type.asTypeName()
      )
      .applyIf(override) { addModifiers(KModifier.OVERRIDE) }
      .applyIf(deprecated) { addAnnotation(deprecatedAnnotation(deprecationReason)) }
      .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
      .applyIf(initializer != null) { initializer(initializer!!) }
      .build()
}

internal fun responseFieldsPropertySpec(fields: List<CodeGenerationAst.Field>): PropertySpec {
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
          modifiers = *arrayOf(KModifier.PRIVATE)
      )
      .initializer(initializer)
      .build()
}

internal fun List<CodeGenerationAst.Field>.toMapperFun(responseTypeName: TypeName): FunSpec {
  val readFieldsCode = mapIndexed { index, field ->
    CodeBlock.of(
        "val %L = %L", field.name, field.type.readCode(field = "RESPONSE_FIELDS[$index]")
    )
  }.joinToCode(separator = "\n", suffix = "\n")
  val mapFieldsCode = map { field ->
    CodeBlock.of("%L = %L", field.name, field.name)
  }.joinToCode(separator = ",\n", suffix = "\n")
  return FunSpec.builder("invoke")
      .addModifiers(KModifier.OPERATOR)
      .addParameter(ParameterSpec.builder("reader", ResponseReader::class).build())
      .returns(responseTypeName)
      .addCode(CodeBlock
          .builder()
          .beginControlFlow("return reader.run")
          .add(readFieldsCode)
          .addStatement("%T(", responseTypeName)
          .indent()
          .add(mapFieldsCode)
          .unindent()
          .addStatement(")")
          .endControlFlow()
          .build()
      )
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
        builder.add("(%S, %S, %L, %L, %T, %L)", responseName, schemaName, arguments.takeIf { it.isNotEmpty() }.toCode(), type.nullable,
            type.customEnumType.asTypeName(), conditionsListCode(conditions))
      }
      else -> {
        builder.add("(%S, %S, %L, %L, %L)", responseName, schemaName, arguments.takeIf { it.isNotEmpty() }.toCode(), type.nullable,
            conditionsListCode(conditions))
      }
    }
    return builder.build()
  }

private fun conditionsListCode(conditions: Set<CodeGenerationAst.Field.Condition>): CodeBlock {
  return conditions
      .map { condition ->
        when (condition) {
          is CodeGenerationAst.Field.Condition.Directive -> CodeBlock.of("%T.booleanCondition(%S, %L)",
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
    this is Map<*, *> && this.isEmpty() -> CodeBlock.of("emptyMap<%T, Any>()", String::class.asTypeName())
    this is Map<*, *> -> CodeBlock.builder()
        .add("mapOf<%T, Any>(\n", String::class.asTypeName())
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

private fun CodeGenerationAst.FieldType.readCode(field: String): CodeBlock {
  val notNullOperator = "!!".takeUnless { nullable } ?: ""
  return when (this) {
    is CodeGenerationAst.FieldType.Scalar -> when (this) {
      is CodeGenerationAst.FieldType.Scalar.ID -> if (field.isNotEmpty()) {
        CodeBlock.of("readCustomType<%T>(%L as %T)%L", ClassName.bestGuess(type), field, ResponseField.CustomTypeField::class,
            notNullOperator)
      } else {
        CodeBlock.of("readCustomType<%T>(%T)%L", ClassName.bestGuess(type), customEnumType.asTypeName(), notNullOperator)
      }
      is CodeGenerationAst.FieldType.Scalar.String -> CodeBlock.of("readString(%L)%L", field, notNullOperator)
      is CodeGenerationAst.FieldType.Scalar.Int -> CodeBlock.of("readInt(%L)%L", field, notNullOperator)
      is CodeGenerationAst.FieldType.Scalar.Boolean -> CodeBlock.of("readBoolean(%L)%L", field, notNullOperator)
      is CodeGenerationAst.FieldType.Scalar.Float -> CodeBlock.of("readDouble(%L)%L", field, notNullOperator)
      is CodeGenerationAst.FieldType.Scalar.Enum -> if (nullable) {
        CodeBlock.of("readString(%L)?.let·{·%T.safeValueOf(it)·}", field, typeRef.asTypeName().copy(nullable = false))
      } else {
        CodeBlock.of("%T.safeValueOf(readString(%L)!!)", typeRef.asTypeName().copy(nullable = false), field)
      }
      is CodeGenerationAst.FieldType.Scalar.Custom -> if (field.isNotEmpty()) {
        CodeBlock.of("readCustomType<%T>(%L as %T)%L", ClassName.bestGuess(type), field, ResponseField.CustomTypeField::class,
            notNullOperator)
      } else {
        CodeBlock.of(
            "readCustomType<%T>(%T)%L", ClassName.bestGuess(type), customEnumType.asTypeName().copy(nullable = false), notNullOperator
        )
      }
    }
    is CodeGenerationAst.FieldType.Object -> {
      val fieldCode = field.takeIf { it.isNotEmpty() }?.let { CodeBlock.of("(%L)", it) } ?: CodeBlock.of("")
      CodeBlock.builder()
          .addStatement("readObject<%T>%L·{·reader·->", typeRef.asTypeName(), fieldCode)
          .indent()
          .addStatement("%T(reader)", typeRef.asTypeName())
          .unindent()
          .add("}%L", notNullOperator)
          .build()
    }
    is CodeGenerationAst.FieldType.Array -> {
      CodeBlock.builder()
          .addStatement("readList<%T>(%L)·{·reader·->", rawType.asTypeName().copy(nullable = false), field)
          .indent()
          .add(rawType.readListItemCode())
          .unindent()
          .add("\n}%L", notNullOperator)
          .applyIf(!rawType.nullable) {
            if (nullable) {
              add("?.map·{ it!! }")
            } else {
              add(".map·{ it!! }")
            }
          }
          .build()
    }
  }
}

private fun CodeGenerationAst.FieldType.readListItemCode(): CodeBlock {
  return when (this) {
    is CodeGenerationAst.FieldType.Scalar -> when (this) {
      is CodeGenerationAst.FieldType.Scalar.ID -> CodeBlock.of(
          "reader.readCustomType<%T>(%T)", ClassName.bestGuess(type), customEnumType.asTypeName()
      )
      is CodeGenerationAst.FieldType.Scalar.String -> CodeBlock.of("reader.readString()")
      is CodeGenerationAst.FieldType.Scalar.Int -> CodeBlock.of("reader.readInt()")
      is CodeGenerationAst.FieldType.Scalar.Boolean -> CodeBlock.of("reader.readBoolean()")
      is CodeGenerationAst.FieldType.Scalar.Float -> CodeBlock.of("reader.readDouble()")
      is CodeGenerationAst.FieldType.Scalar.Enum -> CodeBlock.of(
          "%T.safeValueOf(reader.readString())", typeRef.asTypeName().copy(nullable = false)
      )
      is CodeGenerationAst.FieldType.Scalar.Custom -> CodeBlock.of(
          "reader.readCustomType<%T>(%T)", ClassName.bestGuess(type), customEnumType.asTypeName()
      )
    }
    is CodeGenerationAst.FieldType.Object -> {
      CodeBlock.builder()
          .addStatement("reader.readObject<%T>·{·reader·->", typeRef.asTypeName())
          .indent()
          .addStatement("%T(reader)", typeRef.asTypeName())
          .unindent()
          .add("}")
          .build()
    }
    is CodeGenerationAst.FieldType.Array -> {
      CodeBlock.builder()
          .addStatement("reader.readList<%T>·{·reader·->", rawType.asTypeName().copy(nullable = false))
          .indent()
          .add(rawType.readListItemCode())
          .unindent()
          .add("\n}")
          .applyIf(!rawType.nullable) { add(".map·{ it!! }") }
          .build()
    }
  }
}

internal fun List<CodeGenerationAst.Field>.marshallerFunSpec(override: Boolean = false, thisRef: String): FunSpec {
  val writeFieldsCode = mapIndexed { index, field ->
    field.writeCode(field = "RESPONSE_FIELDS[$index]", thisRef = thisRef)
  }.joinToCode(separator = "")
  return FunSpec.builder("marshaller")
      .applyIf(override) { addModifiers(KModifier.OVERRIDE) }
      .returns(ResponseFieldMarshaller::class)
      .beginControlFlow("return·%T.invoke·{·writer·->", ResponseFieldMarshaller::class)
      .addCode(writeFieldsCode)
      .endControlFlow()
      .build()
}

private fun CodeGenerationAst.Field.writeCode(field: String, thisRef: String): CodeBlock {
  return when (type) {
    is CodeGenerationAst.FieldType.Scalar -> when (type) {
      is CodeGenerationAst.FieldType.Scalar.ID -> CodeBlock.of("writer.writeCustom(%L as %T, this@%L.%L)\n", field,
          ResponseField.CustomTypeField::class, thisRef, name)
      is CodeGenerationAst.FieldType.Scalar.String -> CodeBlock.of("writer.writeString(%L, this@%L.%L)\n", field, thisRef, name)
      is CodeGenerationAst.FieldType.Scalar.Int -> CodeBlock.of("writer.writeInt(%L, this@%L.%L)\n", field, thisRef, name)
      is CodeGenerationAst.FieldType.Scalar.Boolean -> CodeBlock.of("writer.writeBoolean(%L, this@%L.%L)\n", field, thisRef, name)
      is CodeGenerationAst.FieldType.Scalar.Float -> CodeBlock.of("writer.writeDouble(%L, this@%L.%L)\n", field, thisRef, name)
      is CodeGenerationAst.FieldType.Scalar.Enum -> {
        if (type.nullable) {
          CodeBlock.of("writer.writeString(%L, this@%L.%L?.rawValue)\n", field, thisRef, name)
        } else {
          CodeBlock.of("writer.writeString(%L, this@%L.%L.rawValue)\n", field, thisRef, name)
        }
      }
      is CodeGenerationAst.FieldType.Scalar.Custom -> CodeBlock.of("writer.writeCustom(%L as %T, this@%L.%L)\n", field,
          ResponseField.CustomTypeField::class, thisRef, name)
    }
    is CodeGenerationAst.FieldType.Object -> {
      if (type.nullable) {
        CodeBlock.of("writer.writeObject(%L, this@%L.%L?.marshaller())\n", field, thisRef, name)
      } else {
        CodeBlock.of("writer.writeObject(%L, this@%L.%L.marshaller())\n", field, thisRef, name)
      }
    }
    is CodeGenerationAst.FieldType.Array -> {
      CodeBlock.builder()
          .beginControlFlow("writer.writeList(%L, this@%L.%L)·{·value,·listItemWriter·->", field, thisRef, name)
          .beginControlFlow("value?.forEach·{·value·->")
          .add(type.writeListItemCode)
          .endControlFlow()
          .endControlFlow()
          .build()
    }
  }
}

private val CodeGenerationAst.FieldType.Array.writeListItemCode: CodeBlock
  get() {
    val safeValue = if (rawType.nullable) "value?" else "value"
    return when (rawType) {
      is CodeGenerationAst.FieldType.Scalar -> when (rawType) {
        is CodeGenerationAst.FieldType.Scalar.ID -> CodeBlock.of(
            "listItemWriter.writeCustom(%T, value)", rawType.customEnumType.asTypeName()
        )
        is CodeGenerationAst.FieldType.Scalar.String -> CodeBlock.of("listItemWriter.writeString(value)")
        is CodeGenerationAst.FieldType.Scalar.Int -> CodeBlock.of("listItemWriter.writeInt(value)")
        is CodeGenerationAst.FieldType.Scalar.Boolean -> CodeBlock.of("listItemWriter.writeBoolean(value)")
        is CodeGenerationAst.FieldType.Scalar.Float -> CodeBlock.of("listItemWriter.writeDouble(value)")
        is CodeGenerationAst.FieldType.Scalar.Enum -> CodeBlock.of("listItemWriter.writeString($safeValue.rawValue)")
        is CodeGenerationAst.FieldType.Scalar.Custom -> CodeBlock.of(
            "listItemWriter.writeCustom(%T, value)", rawType.customEnumType.asTypeName()
        )
      }
      is CodeGenerationAst.FieldType.Object -> CodeBlock.of("listItemWriter.writeObject($safeValue.marshaller())")
      is CodeGenerationAst.FieldType.Array -> {
        CodeBlock.builder()
            .beginControlFlow("listItemWriter.writeList(value)·{·value,·listItemWriter·->")
            .beginControlFlow("value?.forEach·{·value·->") // value always nullable in ListItemWriter
            .add(rawType.writeListItemCode)
            .endControlFlow()
            .endControlFlow()
            .build()
      }
    }
  }

private fun Number.castTo(type: TypeName): Number {
  return when (type) {
    INT -> toInt()
    LONG -> toLong()
    FLOAT, DOUBLE -> toDouble()
    else -> this
  }
}

internal fun TypeName.createMapperFun(): FunSpec {
  return FunSpec.builder("Mapper")
      .addAnnotation(
          AnnotationSpec.builder(Suppress::class)
              .addMember("%S", "FunctionName")
              .build()
      )
      .returns(ResponseFieldMapper::class.asClassName().parameterizedBy(this))
      .addStatement("return %T·{ invoke(it) }", ResponseFieldMapper::class)
      .build()
}

internal fun PropertySpec.asParameter(): ParameterSpec {
  return ParameterSpec.builder(name, type).build()
}

internal fun Collection<CodeGenerationAst.TypeRef>.accessorProperties(): List<PropertySpec> {
  return map { type ->
    PropertySpec.builder("as${type.name}", type.asTypeName().copy(nullable = true))
        .getter(FunSpec.getterBuilder().addStatement("return this as? %T", type.asTypeName()).build())
        .build()
  }
}

internal fun String.normalizeGraphQLType(): String {
  val normalizedType = removeSuffix("!").removeSurrounding(prefix = "[", suffix = "]").removeSuffix("!")
  return if (normalizedType != this) {
    normalizedType.normalizeGraphQLType()
  } else {
    normalizedType
  }
}

internal val suppressWarningsAnnotation = AnnotationSpec
    .builder(Suppress::class)
    .addMember("%S, %S, %S, %S, %S, %S, %S", "NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
        "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName", "RemoveRedundantQualifierName")
    .build()

internal fun TypeSpec.patchKotlinNativeOptionalArrayProperties(): TypeSpec {
  if (kind != TypeSpec.Kind.CLASS) {
    return this
  }

  val patchedNestedTypes = typeSpecs.map { type ->
    if (type.kind == TypeSpec.Kind.CLASS) {
      type.patchKotlinNativeOptionalArrayProperties()
    } else {
      type
    }
  }

  val nonOptionalListPropertyAccessors = propertySpecs
      .filter { propertySpec ->
        val propertyType = propertySpec.type
        propertyType is ParameterizedTypeName &&
            propertyType.rawType == List::class.asClassName() &&
            propertyType.typeArguments.single().isNullable
      }
      .map { propertySpec ->
        val listItemType = (propertySpec.type as ParameterizedTypeName).typeArguments.single().copy(nullable = false)
        val nonOptionalListType = List::class.asClassName().parameterizedBy(listItemType).copy(nullable = propertySpec.type.isNullable)
        FunSpec
            .builder("${propertySpec.name}FilterNotNull")
            .returns(nonOptionalListType)
            .addStatement("return %L%L.filterNotNull()", propertySpec.name, if (propertySpec.type.isNullable) "?" else "")
            .build()
      }
  return toBuilder()
      .addFunctions(nonOptionalListPropertyAccessors)
      .apply { typeSpecs.clear() }
      .addTypes(patchedNestedTypes)
      .build()
}

private val MULTIPLATFORM_THROWS = ClassName("com.apollographql.apollo.api.internal", "Throws")
private val MULTIPLATFORM_IO_EXCEPTION = ClassName("okio", "IOException")

internal fun FunSpec.Builder.throwsMultiplatformIOException() = throws(MULTIPLATFORM_IO_EXCEPTION)

/**
 * User instead of Kotlin poet throws since we have our own Throws class for Kotlin Multiplatform
 */
internal fun FunSpec.Builder.throws(vararg exceptionClasses: ClassName) = addAnnotation(
    AnnotationSpec.builder(MULTIPLATFORM_THROWS)
        .apply { exceptionClasses.forEach { addMember("%T::class", it) } }
        .build())
