package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.FieldType
import com.apollographql.apollo.compiler.ast.InputType
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.ast.TypeRef
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal object KotlinCodeGen {

  val suppressWarningsAnnotation = AnnotationSpec
      .builder(Suppress::class)
      .addMember("%S, %S, %S, %S, %S", "NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
          "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter")
      .build()

  fun deprecatedAnnotation(message: String) = AnnotationSpec
      .builder(Deprecated::class)
      .apply {
        if (message.isNotBlank()) {
          addMember("message = %S", message)
        }
      }
      .build()

  fun FieldType.asTypeName(optional: Boolean = false): TypeName = when (this) {
    is FieldType.Scalar -> when (this) {
      FieldType.Scalar.String -> String::class.asClassName()
      FieldType.Scalar.Int -> INT
      FieldType.Scalar.Boolean -> BOOLEAN
      FieldType.Scalar.Float -> DOUBLE
      is FieldType.Scalar.Enum -> ClassName(
          packageName = typeRef.packageName,
          simpleName = typeRef.name
      )
      is FieldType.Scalar.Custom -> ClassName.bestGuess(mappedType)
    }
    is FieldType.Fragments -> ClassName.bestGuess(name)
    is FieldType.Object -> ClassName(
        packageName = typeRef.packageName,
        simpleName = typeRef.name
    )
    is FieldType.Fragment -> ClassName(
        packageName = typeRef.packageName,
        simpleName = typeRef.name
    )
    is FieldType.Array -> List::class.asClassName().parameterizedBy(rawType.asTypeName(optional = isOptional))
  }.let {
    if (optional) it.copy(nullable = true) else it.copy(nullable = false)
  }

  fun ObjectType.Field.asPropertySpec(initializer: CodeBlock) =
      PropertySpec
          .builder(
              name = name,
              type = if (isOptional) type.asTypeName().copy(nullable = true) else type.asTypeName()
          )
          .applyIf(isDeprecated) { addAnnotation(deprecatedAnnotation(deprecationReason)) }
          .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
          .initializer(initializer)
          .build()

  fun responseFieldsPropertySpec(fields: List<ObjectType.Field>): PropertySpec {
    val initializer = fields
        .map { field -> field.responseFieldInitializerCode }
        .joinToCode(prefix = "arrayOf(\n", separator = ",\n", suffix = "\n)")
    return PropertySpec
        .builder(
            name = "RESPONSE_FIELDS",
            type = Array<ResponseField>::class.asClassName().parameterizedBy(ResponseField::class.asClassName()),
            modifiers = *arrayOf(KModifier.PRIVATE)
        )
        .initializer(initializer)
        .build()
  }

  private val ObjectType.Field.responseFieldInitializerCode: CodeBlock
    get() {
      val factoryMethod = when (type) {
        is FieldType.Scalar -> when (type) {
          is FieldType.Scalar.String -> "forString"
          is FieldType.Scalar.Int -> "forInt"
          is FieldType.Scalar.Boolean -> "forBoolean"
          is FieldType.Scalar.Float -> "forDouble"
          is FieldType.Scalar.Enum -> "forEnum"
          is FieldType.Scalar.Custom -> "forCustomType"
        }
        is FieldType.Object -> "forObject"
        is FieldType.Fragment -> "forFragment"
        is FieldType.Fragments -> "forString"
        is FieldType.Array -> "forList"
      }

      val builder = CodeBlock.builder().add("%T.%L", ResponseField::class, factoryMethod)
      when {
        type is FieldType.Scalar && type is FieldType.Scalar.Custom -> {
          builder.add("(%S, %S, %L, %L, %T.%L, %L)", responseName, schemaName, arguments.takeIf { it.isNotEmpty() }.toCode(), isOptional,
              type.customEnumType.asTypeName(), type.customEnumConst, conditionsListCode(conditions))
        }
        type is FieldType.Fragment -> {
          builder.add("(%S, %S, %L)", responseName, schemaName, conditionsListCode(conditions))
        }
        else -> {
          builder.add("(%S, %S, %L, %L, %L)", responseName, schemaName, arguments.takeIf { it.isNotEmpty() }.toCode(), isOptional,
              conditionsListCode(conditions))
        }
      }
      return builder.build()
    }

  private fun conditionsListCode(conditions: List<ObjectType.Field.Condition>): CodeBlock {
    return conditions
        .map { condition ->
          when (condition) {
            is ObjectType.Field.Condition.Type -> {
              val possibleTypes = condition.types.map { CodeBlock.of("%S", it) }.joinToCode(separator = ", ")
              CodeBlock.of("%T.typeCondition(arrayOf(%L))", ResponseField.Condition::class, possibleTypes)
            }
            is ObjectType.Field.Condition.Directive -> CodeBlock.of("%T.booleanCondition(%S, %L)",
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

  fun List<ObjectType.Field>.toMapperFun(responseTypeName: TypeName): FunSpec {
    val readFieldsCode = mapIndexed { index, field ->
      CodeBlock.of("val %L = %L", field.name, field.type.readCode(
          field = "RESPONSE_FIELDS[$index]",
          optional = field.isOptional
      ))
    }.joinToCode(separator = "")
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

  fun TypeName.createMapperFun(): FunSpec {
    return FunSpec.builder("Mapper")
        .addAnnotation(
            AnnotationSpec.builder(Suppress::class)
                .addMember("%S", "FunctionName")
                .build()
        )
        .returns(ResponseFieldMapper::class.asClassName().parameterizedBy(this))
        .addStatement("return %T { invoke(it) }", ResponseFieldMapper::class)
        .build()
  }

  private fun FieldType.readCode(field: String, optional: Boolean): CodeBlock {
    return when (this) {
      is FieldType.Scalar -> when (this) {
        is FieldType.Scalar.String -> CodeBlock.of("readString(%L)\n", field)
        is FieldType.Scalar.Int -> CodeBlock.of("readInt(%L)\n", field)
        is FieldType.Scalar.Boolean -> CodeBlock.of("readBoolean(%L)\n", field)
        is FieldType.Scalar.Float -> CodeBlock.of("readDouble(%L)\n", field)
        is FieldType.Scalar.Enum -> if (optional) {
          CodeBlock.of("readString(%L)?.let{ %T.safeValueOf(it) }\n", field, typeRef.asTypeName())
        } else {
          CodeBlock.of("%T.safeValueOf(readString(%L))\n", typeRef.asTypeName(), field)
        }
        is FieldType.Scalar.Custom -> if (field.isNotEmpty()) {
          CodeBlock.of("readCustomType<%T>(%L as %T)\n", ClassName.bestGuess(mappedType), field, ResponseField.CustomTypeField::class)
        } else {
          CodeBlock.of("readCustomType<%T>(%T.%L)\n", ClassName.bestGuess(mappedType), customEnumType.asTypeName(), customEnumConst)
        }
      }
      is FieldType.Object -> {
        val fieldCode = field.takeIf { it.isNotEmpty() }?.let { CodeBlock.of("(%L)", it) } ?: CodeBlock.of("")
        CodeBlock.builder()
            .beginControlFlow("readObject<%T>%L { reader ->", typeRef.asTypeName(), fieldCode)
            .addStatement("%T(reader)", typeRef.asTypeName())
            .endControlFlow()
            .build()
      }
      is FieldType.Array -> {
        CodeBlock.builder()
            .beginControlFlow("readList<%T>(%L) { reader ->", rawType.asTypeName(), field)
            .add(rawType.readListItemCode(optional = isOptional))
            .endControlFlow()
            .build()
      }
      is FieldType.Fragments -> {
        CodeBlock.of("%L(reader)\n", name)
      }
      is FieldType.Fragment -> {
        CodeBlock.builder()
            .beginControlFlow("readFragment<%T>(%L) { reader ->", typeRef.asTypeName(), field)
            .addStatement("%T(reader)", typeRef.asTypeName())
            .endControlFlow()
            .build()
      }
    }
  }

  private fun FieldType.readListItemCode(optional: Boolean): CodeBlock {
    return when (this) {
      is FieldType.Scalar -> when (this) {
        is FieldType.Scalar.String -> CodeBlock.of("reader.readString()")
        is FieldType.Scalar.Int -> CodeBlock.of("reader.readInt()")
        is FieldType.Scalar.Boolean -> CodeBlock.of("reader.readBoolean()")
        is FieldType.Scalar.Float -> CodeBlock.of("reader.readDouble()")
        is FieldType.Scalar.Enum -> if (optional) {
          CodeBlock.of("reader.readString()?.let{ %T.safeValueOf(it) }", typeRef.asTypeName())
        } else {
          CodeBlock.of("%T.safeValueOf(reader.readString())", typeRef.asTypeName())
        }
        is FieldType.Scalar.Custom -> CodeBlock.of("reader.readCustomType<%T>(%T.%L)", ClassName.bestGuess(mappedType),
            customEnumType.asTypeName(), customEnumConst)
      }
      is FieldType.Object -> {
        CodeBlock.builder()
            .beginControlFlow("reader.readObject<%T> { reader ->", typeRef.asTypeName())
            .addStatement("%T(reader)", typeRef.asTypeName())
            .endControlFlow()
            .build()
      }
      is FieldType.Array -> {
        CodeBlock.builder()
            .beginControlFlow("reader.readList<%T> { reader ->", rawType.asTypeName())
            .add(rawType.readListItemCode(optional = isOptional))
            .endControlFlow()
            .build()
      }
      is FieldType.Fragments -> CodeBlock.of("%L(reader)", name)
      else -> throw IllegalArgumentException("Unsupported list item type $this")
    }
  }

  fun List<ObjectType.Field>.marshallerFunSpec(override: Boolean = false, thisRef: String): FunSpec {
    val writeFieldsCode = mapIndexed { index, field ->
      field.writeCode(field = "RESPONSE_FIELDS[$index]", thisRef = thisRef)
    }.joinToCode(separator = "")
    return FunSpec.builder("marshaller")
        .applyIf(override) { addModifiers(KModifier.OVERRIDE) }
        .returns(ResponseFieldMarshaller::class)
        .beginControlFlow("return %T { writer ->", ResponseFieldMarshaller::class)
        .addCode(writeFieldsCode)
        .endControlFlow()
        .build()
  }

  private fun ObjectType.Field.writeCode(field: String, thisRef: String): CodeBlock {
    return when (type) {
      is FieldType.Scalar -> when (type) {
        is FieldType.Scalar.String -> CodeBlock.of("writer.writeString(%L, this@%L.%L)\n", field, thisRef, name)
        is FieldType.Scalar.Int -> CodeBlock.of("writer.writeInt(%L, this@%L.%L)\n", field, thisRef, name)
        is FieldType.Scalar.Boolean -> CodeBlock.of("writer.writeBoolean(%L, this@%L.%L)\n", field, thisRef, name)
        is FieldType.Scalar.Float -> CodeBlock.of("writer.writeDouble(%L, this@%L.%L)\n", field, thisRef, name)
        is FieldType.Scalar.Enum -> {
          if (isOptional) {
            CodeBlock.of("writer.writeString(%L, this@%L.%L?.rawValue)\n", field, thisRef, name)
          } else {
            CodeBlock.of("writer.writeString(%L, this@%L.%L.rawValue)\n", field, thisRef, name)
          }
        }
        is FieldType.Scalar.Custom -> CodeBlock.of("writer.writeCustom(%L as %T, this@%L.%L)\n", field,
            ResponseField.CustomTypeField::class, thisRef, name)
      }
      is FieldType.Object -> {
        if (isOptional) {
          CodeBlock.of("writer.writeObject(%L, this@%L.%L?.marshaller())\n", field, thisRef, name)
        } else {
          CodeBlock.of("writer.writeObject(%L, this@%L.%L.marshaller())\n", field, thisRef, name)
        }
      }
      is FieldType.Fragment -> {
        if (isOptional) {
          CodeBlock.of("writer.writeFragment(this@%L.%L?.marshaller())\n", thisRef, name)
        } else {
          CodeBlock.of("writer.writeFragment(this@%L.%L.marshaller())\n", thisRef, name)
        }
      }
      is FieldType.Array -> {
        CodeBlock.builder()
            .beginControlFlow("writer.writeList(%L, this@%L.%L) { value, listItemWriter ->", field, thisRef, name)
            .beginControlFlow("value?.forEach { value ->")
            .add(type.rawType.writeListItemCode)
            .endControlFlow()
            .endControlFlow()
            .build()
      }
      is FieldType.Fragments -> CodeBlock.of("this@%L.%L.marshaller().marshal(writer)\n", thisRef, name)
    }
  }

  private val FieldType.writeListItemCode: CodeBlock
    get() {
      return when (this) {
        is FieldType.Scalar -> when (this) {
          is FieldType.Scalar.String -> CodeBlock.of("listItemWriter.writeString(value)")
          is FieldType.Scalar.Int -> CodeBlock.of("listItemWriter.writeInt(value)")
          is FieldType.Scalar.Boolean -> CodeBlock.of("listItemWriter.writeBoolean(value)")
          is FieldType.Scalar.Float -> CodeBlock.of("listItemWriter.writeDouble(value)")
          is FieldType.Scalar.Enum -> CodeBlock.of("listItemWriter.writeString(value?.rawValue)")
          is FieldType.Scalar.Custom -> CodeBlock.of("listItemWriter.writeCustom(%T.%L, value)", customEnumType.asTypeName(),
              customEnumConst)
        }
        is FieldType.Object -> CodeBlock.of("listItemWriter.writeObject(value?.marshaller())", asTypeName())
        is FieldType.Array -> {
          CodeBlock.builder()
              .beginControlFlow(
                  "listItemWriter.writeList(value) { value, listItemWriter ->",
                  List::class.asClassName().parameterizedBy(rawType.asTypeName()))
              .beginControlFlow("value?.forEach { value ->", List::class.asClassName().parameterizedBy(rawType.asTypeName()))
              .add(rawType.writeListItemCode)
              .endControlFlow()
              .endControlFlow()
              .build()
        }
        else -> throw IllegalArgumentException("Unsupported field type: $this")
      }
    }

  fun InputType.Field.asPropertySpec(initializer: CodeBlock) =
      PropertySpec
          .builder(
              name = name,
              type = if (isOptional) Input::class.asClassName().parameterizedBy(
                  type.asTypeName()) else type.asTypeName()
          )
          .apply { if (description.isNotBlank()) addKdoc("%L\n", description) }
          .apply { initializer(initializer) }
          .build()

  fun String.normalizeGraphQLType(): String {
    val normalizedType = removeSuffix("!").removeSurrounding(prefix = "[", suffix = "]").removeSuffix("!")
    return if (normalizedType != this) {
      normalizedType.normalizeGraphQLType()
    } else {
      normalizedType
    }
  }

  fun Any.toDefaultValueCodeBlock(typeName: TypeName, fieldType: FieldType): CodeBlock = when {
    this is Number -> CodeBlock.of("%L%L", castTo(typeName), if (typeName == LONG) "L" else "")
    fieldType is FieldType.Scalar.Enum -> CodeBlock.of("%T.safeValueOf(%S)", typeName, this)
    fieldType is FieldType.Array -> {
      @Suppress("UNCHECKED_CAST")
      (this as List<Any>).toDefaultValueCodeBlock(typeName, fieldType)
    }
    this !is String -> CodeBlock.of("%L", this)
    else -> CodeBlock.of("%S", this)
  }

  private fun List<Any>.toDefaultValueCodeBlock(typeName: TypeName, fieldType: FieldType.Array): CodeBlock {
    return if (isEmpty()) {
      CodeBlock.of("emptyList()")
    } else {
      filterNotNull()
          .map { value ->
            val rawTypeName = (typeName as ParameterizedTypeName).typeArguments.first().copy(nullable = false)
            value.toDefaultValueCodeBlock(rawTypeName, fieldType.rawType)
          }
          .joinToCode(prefix = "listOf(", separator = ", ", suffix = ")")
    }
  }

  private fun Number.castTo(type: TypeName): Number = when (type) {
    INT -> toInt()
    LONG -> toLong()
    FLOAT, DOUBLE -> toDouble()
    else -> this
  }

  fun TypeRef.asTypeName() = ClassName(packageName = packageName, simpleName = name.capitalize())

  private fun Map<String, Any?>?.toCode(): CodeBlock? {
    return when {
      this == null -> null
      this.isEmpty() -> CodeBlock.of("emptyMap<%T, Any>()", String::class.asTypeName())
      else -> CodeBlock.builder()
          .add("mapOf<%T, Any>(\n", String::class.asTypeName())
          .indent()
          .add(map { it.toCode() }.joinToCode(separator = ",\n"))
          .unindent()
          .add(")")
          .build()
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun Map.Entry<String, Any?>.toCode() = when (value) {
    is Map<*, *> -> CodeBlock.of("%S to %L", key, (value as Map<String, Any>).toCode())
    null -> CodeBlock.of("%S to null", key)
    else -> CodeBlock.of("%S to %S", key, value)
  }
}
