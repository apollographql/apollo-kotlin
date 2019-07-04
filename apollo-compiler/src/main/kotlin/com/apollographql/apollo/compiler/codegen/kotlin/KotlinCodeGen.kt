package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.FieldType
import com.apollographql.apollo.compiler.ast.InputType
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.ast.TypeRef
import com.apollographql.apollo.compiler.ir.ScalarType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal object KotlinCodeGen {

  val suppressWarningsAnnotation = AnnotationSpec
      .builder(Suppress::class)
      .addMember("%S, %S, %S, %S", "NAME_SHADOWING", "LocalVariableName", "RemoveExplicitTypeArguments",
          "NestedLambdaShadowedImplicitParameter")
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
    is FieldType.InlineFragment -> ClassName(
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
            type = Array<ResponseField>::
            class.asClassName().parameterizedBy(ResponseField::
            class.asClassName()),
            modifiers = *arrayOf(KModifier.PRIVATE)
        )
        .initializer(initializer)
        .build()
  }

  private val ObjectType.Field.responseFieldInitializerCode: CodeBlock
    get() {
      val builder = CodeBlock.builder().add("%T.%L", ResponseField::class, when (type) {
        is FieldType.Scalar -> when (type) {
          is FieldType.Scalar.String -> "forString"
          is FieldType.Scalar.Int -> "forInt"
          is FieldType.Scalar.Boolean -> "forBoolean"
          is FieldType.Scalar.Float -> "forDouble"
          is FieldType.Scalar.Enum -> "forEnum"
          is FieldType.Scalar.Custom -> "forCustomType"
        }
        is FieldType.Fragments -> "forString"
        is FieldType.Object -> "forObject"
        is FieldType.InlineFragment -> "forInlineFragment"
        is FieldType.Array -> "forList"
      })

      when {
        type is FieldType.Scalar && type is FieldType.Scalar.Custom -> {
          builder.add("(%S, %S, %L, %L, %T.%L, %L)", responseName, schemaName, arguments.toCode(), isOptional,
              type.customEnumType.asTypeName(), type.customEnumConst, conditionsListCode(conditions))
        }
        type is FieldType.InlineFragment -> {
          builder.add("(%S, %S, %L)", responseName, schemaName, conditionsListCode(conditions))
        }
        else -> {
          builder.add("(%S, %S, %L, %L, %L)", responseName, schemaName, arguments.toCode(), isOptional,
              conditionsListCode(conditions))
        }
      }

      return builder.build()
    }

  private fun conditionsListCode(conditions: List<ObjectType.Field.Condition>): CodeBlock? {
    return conditions.takeIf { conditions.isNotEmpty() }
        ?.map { condition ->
          when (condition) {
            is ObjectType.Field.Condition.Type -> CodeBlock.of("%S", condition.type)
            is ObjectType.Field.Condition.Directive -> CodeBlock.of("%T.booleanCondition(%S, %L)",
                ResponseField.Condition::class, condition.variableName, condition.inverted)
          }
        }
        ?.joinToCode(prefix = "listOf(", separator = ", ", suffix = ")")
  }

  fun List<ObjectType.Field>.toMapperFun(responseTypeName: TypeName): FunSpec {
    val readFieldsCode = mapIndexed { index, field ->
      CodeBlock.of("val %L = %L", field.name, field.type.readCode(
          reader = "reader",
          field = "RESPONSE_FIELDS[$index]"
      ))
    }.joinToCode(separator = "\n", suffix = "\n")
    val mapFieldsCode = map { field ->
      CodeBlock.of("%L = %L", field.name, field.name)
    }.joinToCode(separator = ",\n", suffix = "\n")
    return FunSpec.builder("invoke")
        .addModifiers(KModifier.OPERATOR)
        .addParameter(ParameterSpec.builder("reader", ResponseReader::class).build())
        .returns(responseTypeName)
        .addCode(CodeBlock.builder()
            .add(readFieldsCode)
            .addStatement("return %T(", responseTypeName)
            .indent()
            .add(mapFieldsCode)
            .unindent()
            .addStatement(")")
            .build()
        )
        .build()
  }

  private fun FieldType.readCode(reader: String, field: String): CodeBlock {
    return when (this) {
      is FieldType.Scalar -> when (this) {
        is FieldType.Scalar.String -> CodeBlock.of("%L.readString(%L)", reader, field)
        is FieldType.Scalar.Int -> CodeBlock.of("%L.readInt(%L)", reader, field)
        is FieldType.Scalar.Boolean -> CodeBlock.of("%L.readBoolean(%L)", reader, field)
        is FieldType.Scalar.Float -> CodeBlock.of("%L.readDouble(%L)", reader, field)
        is FieldType.Scalar.Enum -> CodeBlock.of("%T.safeValueOf(%L.readString(%L))", typeRef.asTypeName(), reader,
            field)
        is FieldType.Scalar.Custom -> if (field.isNotEmpty()) {
          CodeBlock.of("%L.readCustomType<%T>(%L as %T)", reader, ClassName.bestGuess(mappedType),
              field, ResponseField.CustomTypeField::class)
        } else {
          CodeBlock.of("%L.readCustomType<%T>(%T.%L)", reader, ClassName.bestGuess(mappedType),
              customEnumType.asTypeName(), customEnumConst)
        }
      }
      is FieldType.Object -> {
        val fieldCode = field.takeIf { it.isNotEmpty() }?.let { CodeBlock.of("(%L)", it) } ?: CodeBlock.of("")
        CodeBlock.builder()
            .add("%L.readObject<%T>%L { reader ->\n", reader, typeRef.asTypeName(), fieldCode)
            .indent()
            .addStatement("%T(reader)", typeRef.asTypeName())
            .unindent()
            .add("}\n")
            .build()
      }
      is FieldType.Array -> {
        CodeBlock.builder()
            .apply {
              if (field.isBlank()) {
                add("%L.readList<%T> {\n", reader, rawType.asTypeName())
              } else {
                add("%L.readList<%T>(%L) {\n", reader, rawType.asTypeName(), field)
              }
            }
            .indent()
            .add(rawType.readCode(reader = "it", field = ""))
            .add("\n")
            .unindent()
            .add("}")
            .build()
      }
      is FieldType.Fragments -> {
        CodeBlock.builder()
            .beginControlFlow("%L.readConditional(%L) { conditionalType, reader ->", reader, field)
            .add(
                fields.map { field ->
                  if (field.isOptional) {
                    CodeBlock.of("val %L = if (%T.POSSIBLE_TYPES.contains(conditionalType)) %T(reader) else null",
                        field.name, field.type.asTypeName(), field.type.asTypeName())
                  } else {
                     CodeBlock.of("val %L = %T(reader)", field.name, field.type.asTypeName())
                  }
                }.joinToCode(separator = "\n", suffix = "\n")
            )
            .addStatement("%L(", name)
            .indent()
            .add(
                fields.map { field ->
                  if (field.isOptional) {
                    CodeBlock.of("%L = %L", field.name, field.name)
                  } else {
                    CodeBlock.of("%L = %L", field.name, field.name)
                  }

                }.joinToCode(separator = ",\n", suffix = "\n")
            )
            .unindent()
            .addStatement(")")
            .endControlFlow()
            .build()
      }
      is FieldType.InlineFragment -> {
        val conditionalBranches = fragmentRefs.map { typeRef ->
          CodeBlock.of("in %T.POSSIBLE_TYPES -> %T(reader)", typeRef.asTypeName(), typeRef.asTypeName())
        }
        CodeBlock.builder()
            .beginControlFlow("%L.readConditional(%L) { conditionalType, reader ->", reader, field)
            .beginControlFlow("when(conditionalType)")
            .add(conditionalBranches.joinToCode("\n"))
            .addStatement("")
            .addStatement("else -> null")
            .endControlFlow()
            .endControlFlow()
            .build()
      }
    }
  }

  fun marshallerFunSpec(fields: List<ObjectType.Field>, override: Boolean = false): FunSpec {
    val writeFieldsCode = fields.mapIndexed { index, field ->
      field.writeCode(field = "RESPONSE_FIELDS[$index]")
    }.joinToCode(separator = "\n", suffix = "\n")
    return FunSpec.builder("marshaller")
        .applyIf(override) { addModifiers(KModifier.OVERRIDE) }
        .returns(ResponseFieldMarshaller::class)
        .beginControlFlow("return %T", ResponseFieldMarshaller::class)
        .addCode(writeFieldsCode)
        .endControlFlow()
        .build()
  }

  private fun ObjectType.Field.writeCode(field: String): CodeBlock {
    return when (type) {
      is FieldType.Scalar -> when (type) {
        is FieldType.Scalar.String -> CodeBlock.of("it.writeString(%L, %L)", field, name)
        is FieldType.Scalar.Int -> CodeBlock.of("it.writeInt(%L, %L)", field, name)
        is FieldType.Scalar.Boolean -> CodeBlock.of("it.writeBoolean(%L, %L)", field, name)
        is FieldType.Scalar.Float -> CodeBlock.of("it.writeDouble(%L, %L)", field, name)
        is FieldType.Scalar.Enum -> {
          if (isOptional) {
            CodeBlock.of("it.writeString(%L, %L?.rawValue)", field, name)
          } else {
            CodeBlock.of("it.writeString(%L, %L.rawValue)", field, name)
          }
        }
        is FieldType.Scalar.Custom -> CodeBlock.of("it.writeCustom(%L as %T, %L)", field,
            ResponseField.CustomTypeField::class, name)
      }
      is FieldType.Object, is FieldType.InlineFragment -> {
        if (isOptional) {
          CodeBlock.of("it.writeObject(%L, %L?.marshaller())", field, name)
        } else {
          CodeBlock.of("it.writeObject(%L, %L.marshaller())", field, name)
        }
      }
      is FieldType.Array -> {
        CodeBlock.builder()
            .add("it.writeList(%L, %L) { value, listItemWriter ->\n", field, name)
            .indent()
            .add("value?.forEach { value ->\n")
            .indent()
            .add(type.rawType.writeListItemCode)
            .add("\n")
            .unindent()
            .add("}")
            .add("\n")
            .unindent()
            .add("}")
            .build()
      }
      is FieldType.Fragments -> CodeBlock.of("%L.marshaller().marshal(it)", name)
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
          is FieldType.Scalar.Custom -> CodeBlock.of("listItemWriter.writeCustom(%T.%L, value)",
              customEnumType.asTypeName(), customEnumConst)
        }
        is FieldType.Object -> CodeBlock.of("listItemWriter.writeObject(value?.marshaller())",
            asTypeName())
        is FieldType.Array -> {
          CodeBlock.builder()
              .add("listItemWriter.writeList(value) { value, listItemWriter ->\n",
                  List::class.asClassName().parameterizedBy(rawType.asTypeName()))
              .indent()
              .add("value?.forEach { value ->\n", List::class.asClassName().parameterizedBy(rawType.asTypeName()))
              .indent()
              .add(rawType.writeListItemCode)
              .add("\n")
              .unindent()
              .add("}")
              .add("\n")
              .unindent()
              .add("}")
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
    fieldType is FieldType.Array -> (this as List<Any>).toDefaultValueCodeBlock(typeName, fieldType)
    this !is String -> CodeBlock.of("%L", this)
    else -> CodeBlock.of("%S", this)
  }

  private fun List<Any>.toDefaultValueCodeBlock(typeName: TypeName, fieldType: FieldType.Array): CodeBlock {
    val codeBuilder = CodeBlock.builder().add("listOf(")
    return filterNotNull()
        .map {
          it.toDefaultValueCodeBlock((typeName as ParameterizedTypeName).typeArguments.first(), fieldType.rawType)
        }
        .foldIndexed(codeBuilder) { index, builder, code ->
          builder.add(if (index > 0) ", " else "").add(code)
        }
        .add(")")
        .build()
  }

  private fun Number.castTo(type: TypeName): Number = when (type) {
    INT -> toInt()
    LONG -> toLong()
    FLOAT, DOUBLE -> toDouble()
    else -> this
  }

  fun TypeRef.asTypeName() = ClassName(packageName = packageName, simpleName = name.capitalize())

  private fun Map<String, Any>.toCode(): CodeBlock? {
    return takeIf { it.isNotEmpty() }?.let {
      it.map { it.toCode() }
          .foldIndexed(CodeBlock.builder().add("mapOf<%T, Any>(\n",
              String::class.asTypeName()).indent()) { index, builder, code ->
            if (index > 0) {
              builder.add(",\n")
            }
            builder.add(code)
          }
          .unindent()
          .add(")")
          .build()
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun Map.Entry<String, Any>.toCode() = when (value) {
    is Map<*, *> -> CodeBlock.of("%S to %L", key, (value as Map<String, Any>).toCode())
    else -> CodeBlock.of("%S to %S", key, value)
  }

  fun Any.normalizeJsonValue(graphQLType: String): Any = when (this) {
    is Number -> {
      val scalarType = ScalarType.forName(graphQLType.removeSuffix("!"))
      when (scalarType) {
        is ScalarType.INT -> toInt()
        is ScalarType.FLOAT -> toDouble()
        else -> this
      }
    }
    is Boolean, is Map<*, *> -> this
    is List<*> -> this.mapNotNull {
      it?.normalizeJsonValue(graphQLType.removeSuffix("!").removePrefix("[").removeSuffix("]"))
    }
    else -> toString()
  }
}
