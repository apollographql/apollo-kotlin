package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.AST
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import javax.annotation.Generated

object KotlinCodeGen {

  val suppressWarningsAnnotation = AnnotationSpec.builder(Suppress::class)
    .addMember("%S, %S", "NAME_SHADOWING", "LocalVariableName")
    .build()

  val generatedByApolloAnnotation = AnnotationSpec.builder(Generated::class)
    .addMember("%S", "Apollo GraphQL")
    .build()

  fun deprecatedAnnotation(message: String) = AnnotationSpec.builder(Deprecated::class)
    .apply {
      if (message.isNotBlank()) {
        addMember("message = %S", message)
      }
    }
    .build()

  fun AST.FieldType.asTypeName(optional: Boolean = false): TypeName = when (this) {
    is AST.FieldType.Scalar -> when (this) {
      AST.FieldType.Scalar.String -> String::class.asClassName()
      AST.FieldType.Scalar.Int -> INT
      AST.FieldType.Scalar.Boolean -> BOOLEAN
      AST.FieldType.Scalar.Float -> DOUBLE
      is AST.FieldType.Scalar.Enum -> ClassName(
        packageName = typeRef.packageName,
        simpleName = typeRef.name
      )
      is AST.FieldType.Scalar.Custom -> ClassName.bestGuess(mappedType)
    }
    is AST.FieldType.Fragments -> ClassName.bestGuess(name)
    is AST.FieldType.Object -> ClassName(
      packageName = typeRef.packageName,
      simpleName = typeRef.name
    )
    is AST.FieldType.InlineFragment -> ClassName(
      packageName = typeRef.packageName,
      simpleName = typeRef.name
    )
    is AST.FieldType.Array -> List::class.asClassName().parameterizedBy(rawType.asTypeName(optional = true))
  }.let {
    if (optional) it.asNullable() else it.asNonNull()
  }

  fun AST.ObjectType.Field.asPropertySpec(initializer: CodeBlock): PropertySpec {
    return PropertySpec.builder(
      name = name,
      type = if (isOptional) type.asTypeName().asNullable() else type.asTypeName()
    )
      .applyIf(isDeprecated) { addAnnotation(deprecatedAnnotation(deprecationReason)) }
      .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
      .initializer(initializer)
      .build()
  }

  fun responseFieldsPropertySpec(fields: List<AST.ObjectType.Field>): PropertySpec {
    val initializer = fields.map { field ->
      field.responseFieldInitializerCode
    }.joinToCode(prefix = "arrayOf(\n", separator = ",\n", suffix = "\n)")
    return PropertySpec.builder(
      name = "RESPONSE_FIELDS",
      type = Array<ResponseField>::class.asClassName().parameterizedBy(ResponseField::class.asClassName()),
      modifiers = *arrayOf(KModifier.PRIVATE)
    ).initializer(initializer).build()
  }

  private val AST.ObjectType.Field.responseFieldInitializerCode: CodeBlock
    get() {
      val builder = CodeBlock.builder().add("%T.%L", ResponseField::class, when (type) {
        is AST.FieldType.Scalar -> when (type) {
          is AST.FieldType.Scalar.String -> "forString"
          is AST.FieldType.Scalar.Int -> "forInt"
          is AST.FieldType.Scalar.Boolean -> "forBoolean"
          is AST.FieldType.Scalar.Float -> "forDouble"
          is AST.FieldType.Scalar.Enum -> "forEnum"
          is AST.FieldType.Scalar.Custom -> "forCustomType"
        }
        is AST.FieldType.Fragments -> "forString"
        is AST.FieldType.Object -> "forObject"
        is AST.FieldType.InlineFragment -> "forInlineFragment"
        is AST.FieldType.Array -> "forList"
      })

      when {
        type is AST.FieldType.Scalar && type is AST.FieldType.Scalar.Custom -> {
          builder.add("(%S, %S, %L, %L, %T.%L, %L)", responseName, schemaName, arguments.toCode(), isOptional,
            type.customEnumType.asTypeName(), type.customEnumConst, conditionsListCode(conditions))
        }
        type is AST.FieldType.InlineFragment -> {
          builder.add("(%S, %S, %L)", responseName, schemaName, conditionsListCode(conditions))
        }
        else -> {
          builder.add("(%S, %S, %L, %L, %L)", responseName, schemaName, arguments.toCode(), isOptional,
            conditionsListCode(conditions))
        }
      }

      return builder.build()
    }

  private fun conditionsListCode(conditions: List<AST.ObjectType.Field.Condition>): CodeBlock? {
    return conditions.takeIf { conditions.isNotEmpty() }
      ?.map { condition ->
        when (condition) {
          is AST.ObjectType.Field.Condition.Type -> CodeBlock.of("%S", condition.type)
          is AST.ObjectType.Field.Condition.Directive -> CodeBlock.of("%T.booleanCondition(%S, %L)",
            ResponseField.Condition::class, condition.variableName, condition.inverted)
        }
      }
      ?.joinToCode(prefix = "listOf(", separator = ", ", suffix = ")")
  }

  fun List<AST.ObjectType.Field>.toMapperFun(responseTypeName: TypeName): FunSpec {
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

  private fun AST.FieldType.readCode(reader: String, field: String): CodeBlock {
    return when (this) {
      is AST.FieldType.Scalar -> when (this) {
        is AST.FieldType.Scalar.String -> CodeBlock.of("%L.readString(%L)", reader, field)
        is AST.FieldType.Scalar.Int -> CodeBlock.of("%L.readInt(%L)", reader, field)
        is AST.FieldType.Scalar.Boolean -> CodeBlock.of("%L.readBoolean(%L)", reader, field)
        is AST.FieldType.Scalar.Float -> CodeBlock.of("%L.readDouble(%L)", reader, field)
        is AST.FieldType.Scalar.Enum -> CodeBlock.of("%T.safeValueOf(%L.readString(%L))", typeRef.asTypeName(), reader,
          field)
        is AST.FieldType.Scalar.Custom -> if (field.isNotEmpty()) {
          CodeBlock.of("%L.readCustomType<%T>(%L as %T)", reader, ClassName.bestGuess(mappedType),
            field, ResponseField.CustomTypeField::class)
        } else {
          CodeBlock.of("%L.readCustomType<%T>(%T.%L)", reader, ClassName.bestGuess(mappedType),
            customEnumType.asTypeName(), customEnumConst)
        }
      }
      is AST.FieldType.Object -> {
        val fieldCode = field.takeIf { it.isNotEmpty() }?.let { CodeBlock.of("(%L)", it) } ?: CodeBlock.of("")
        CodeBlock.builder()
          .add("%L.readObject<%T>%L { reader ->\n", reader, typeRef.asTypeName(), fieldCode)
          .indent()
          .addStatement("%T(reader)", typeRef.asTypeName())
          .unindent()
          .add("}\n")
          .build()
      }
      is AST.FieldType.Array -> {
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
      is AST.FieldType.Fragments -> {
        CodeBlock.builder()
          .beginControlFlow("%L.readConditional(%L) { conditionalType, reader ->", reader, field)
          .add(
            fields.map { field ->
              CodeBlock.of("val %L = if (%T.POSSIBLE_TYPES.contains(conditionalType)) %T(reader) else null",
                field.name, field.type.asTypeName(), field.type.asTypeName())
            }.joinToCode(separator = "\n", suffix = "\n")
          )
          .addStatement("%L(", name)
          .indent()
          .add(
            fields.map { field ->
              if (field.isOptional) {
                CodeBlock.of("%L = %L", field.name, field.name)
              } else {
                CodeBlock.of("%L = %L!!", field.name, field.name)
              }

            }.joinToCode(separator = ",\n", suffix = "\n")
          )
          .unindent()
          .addStatement(")")
          .endControlFlow()
          .build()
      }
      is AST.FieldType.InlineFragment -> {
        CodeBlock.builder()
          .beginControlFlow("%L.readConditional(%L) { conditionalType, reader ->", reader, field)
          .addStatement("%T(reader)", typeRef.asTypeName())
          .endControlFlow()
          .build()
      }
    }
  }

  fun marshallerFunSpec(fields: List<AST.ObjectType.Field>): FunSpec {
    val writeFieldsCode = fields.mapIndexed { index, field ->
      field.writeCode(field = "RESPONSE_FIELDS[$index]")
    }.joinToCode(separator = "\n", suffix = "\n")
    return FunSpec.builder("marshaller")
      .returns(ResponseFieldMarshaller::class)
      .beginControlFlow("return %T", ResponseFieldMarshaller::class)
      .addCode(writeFieldsCode)
      .endControlFlow()
      .build()
  }

  private fun AST.ObjectType.Field.writeCode(field: String): CodeBlock {
    return when (type) {
      is AST.FieldType.Scalar -> when (type) {
        is AST.FieldType.Scalar.String -> CodeBlock.of("it.writeString(%L, %L)", field, name)
        is AST.FieldType.Scalar.Int -> CodeBlock.of("it.writeInt(%L, %L)", field, name)
        is AST.FieldType.Scalar.Boolean -> CodeBlock.of("it.writeBoolean(%L, %L)", field, name)
        is AST.FieldType.Scalar.Float -> CodeBlock.of("it.writeDouble(%L, %L)", field, name)
        is AST.FieldType.Scalar.Enum -> {
          if (isOptional) {
            CodeBlock.of("it.writeString(%L, %L?.rawValue)", field, name)
          } else {
            CodeBlock.of("it.writeString(%L, %L.rawValue)", field, name)
          }
        }
        is AST.FieldType.Scalar.Custom -> CodeBlock.of("it.writeCustom(%L as %T, %L)", field,
          ResponseField.CustomTypeField::class, name)
      }
      is AST.FieldType.Object, is AST.FieldType.InlineFragment -> {
        if (isOptional) {
          CodeBlock.of("it.writeObject(%L, %L?.marshaller())", field, name)
        } else {
          CodeBlock.of("it.writeObject(%L, %L.marshaller())", field, name)
        }
      }
      is AST.FieldType.Array -> {
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
      is AST.FieldType.Fragments -> CodeBlock.of("%L.marshaller().marshal(it)", name)
    }
  }

  private val AST.FieldType.writeListItemCode: CodeBlock
    get() {
      return when (this) {
        is AST.FieldType.Scalar -> when (this) {
          is AST.FieldType.Scalar.String -> CodeBlock.of("listItemWriter.writeString(value)")
          is AST.FieldType.Scalar.Int -> CodeBlock.of("listItemWriter.writeInt(value)")
          is AST.FieldType.Scalar.Boolean -> CodeBlock.of("listItemWriter.writeBoolean(value)")
          is AST.FieldType.Scalar.Float -> CodeBlock.of("listItemWriter.writeDouble(value)")
          is AST.FieldType.Scalar.Enum -> CodeBlock.of("listItemWriter.writeString(value?.rawValue)")
          is AST.FieldType.Scalar.Custom -> CodeBlock.of("listItemWriter.writeCustom(%T.%L, value)",
            customEnumType.asTypeName(), customEnumConst)
        }
        is AST.FieldType.Object -> CodeBlock.of("listItemWriter.writeObject(value?.marshaller())",
          asTypeName())
        is AST.FieldType.Array -> {
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

  fun AST.InputType.Field.asPropertySpec(initializer: CodeBlock): PropertySpec {
    return PropertySpec.builder(
      name = name,
      type = if (isOptional) Input::class.asClassName().parameterizedBy(type.asTypeName()) else type.asTypeName()
    )
      .apply { if (description.isNotBlank()) addKdoc("%L\n", description) }
      .apply { initializer(initializer) }
      .build()
  }

  fun String.normalizeGraphQLType(): String {
    val normalizedType = removeSuffix("!").removeSurrounding(prefix = "[", suffix = "]").removeSuffix("!")
    return if (normalizedType != this) {
      normalizedType.normalizeGraphQLType()
    } else {
      normalizedType
    }
  }
}