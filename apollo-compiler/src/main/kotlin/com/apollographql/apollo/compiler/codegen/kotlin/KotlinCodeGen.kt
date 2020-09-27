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
      is FieldType.Scalar.Enum -> ClassName(typeRef.packageName, typeRef.name)
      is FieldType.Scalar.Custom -> ClassName.bestGuess(mappedType)
    }
    is FieldType.Fragments -> ClassName.bestGuess(name)
    is FieldType.Object -> ClassName(typeRef.packageName, typeRef.name)
    is FieldType.Fragment -> ClassName(typeRef.packageName, typeRef.name)
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

  fun TypeName.createMapperFun(): FunSpec {
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

  private fun FieldType.readCode(field: String, optional: Boolean): CodeBlock {
    val notNullOperator = "!!".takeIf { !optional } ?: ""
    return when (this) {
      is FieldType.Scalar -> when (this) {
        is FieldType.Scalar.String -> CodeBlock.of("readString(%L)%L", field, notNullOperator)
        is FieldType.Scalar.Int -> CodeBlock.of("readInt(%L)%L", field, notNullOperator)
        is FieldType.Scalar.Boolean -> CodeBlock.of("readBoolean(%L)%L", field, notNullOperator)
        is FieldType.Scalar.Float -> CodeBlock.of("readDouble(%L)%L", field, notNullOperator)
        is FieldType.Scalar.Enum -> if (optional) {
          CodeBlock.of("readString(%L)?.let·{ %T.safeValueOf(it) }", field, typeRef.asTypeName())
        } else {
          CodeBlock.of("%T.safeValueOf(readString(%L)!!)", typeRef.asTypeName(), field)
        }
        is FieldType.Scalar.Custom -> if (field.isNotEmpty()) {
          CodeBlock.of("readCustomType<%T>(%L as %T)%L", ClassName.bestGuess(mappedType), field, ResponseField.CustomTypeField::class,
              notNullOperator)
        } else {
          CodeBlock.of("readCustomType<%T>(%T.%L)%L", ClassName.bestGuess(mappedType), customEnumType.asTypeName(), customEnumConst,
              notNullOperator)
        }
      }
      is FieldType.Object -> {
        val fieldCode = field.takeIf { it.isNotEmpty() }?.let { CodeBlock.of("(%L)", it) } ?: CodeBlock.of("")
        CodeBlock.builder()
            .addStatement("readObject<%T>%L·{ reader ->", typeRef.asTypeName(), fieldCode)
            .indent()
            .addStatement("%T(reader)", typeRef.asTypeName())
            .unindent()
            .add("}%L", notNullOperator)
            .build()
      }
      is FieldType.Array -> {
        CodeBlock.builder()
            .addStatement("readList<%T>(%L)·{ reader ->", rawType.asTypeName(), field)
            .indent()
            .add(rawType.readListItemCode())
            .unindent()
            .add("\n}%L", notNullOperator)
            .applyIf(!isOptional) {
              if (optional) {
                add("?.map·{ it!! }")
              } else {
                add(".map·{ it!! }")
              }
            }
            .build()
      }
      is FieldType.Fragments -> {
        CodeBlock.of("%L(reader)", name)
      }
      is FieldType.Fragment -> {
        CodeBlock.builder()
            .addStatement("readFragment<%T>(%L)·{ reader ->", typeRef.asTypeName(), field)
            .indent()
            .addStatement("%T(reader)", typeRef.asTypeName())
            .unindent()
            .add("}%L", notNullOperator)
            .build()
      }
    }
  }

  private fun FieldType.readListItemCode(): CodeBlock {
    return when (this) {
      is FieldType.Scalar -> when (this) {
        is FieldType.Scalar.String -> CodeBlock.of("reader.readString()")
        is FieldType.Scalar.Int -> CodeBlock.of("reader.readInt()")
        is FieldType.Scalar.Boolean -> CodeBlock.of("reader.readBoolean()")
        is FieldType.Scalar.Float -> CodeBlock.of("reader.readDouble()")
        is FieldType.Scalar.Enum -> CodeBlock.of("%T.safeValueOf(reader.readString())", typeRef.asTypeName())
        is FieldType.Scalar.Custom -> {
          CodeBlock.of("reader.readCustomType<%T>(%T.%L)", ClassName.bestGuess(mappedType), customEnumType.asTypeName(), customEnumConst)
        }
      }
      is FieldType.Object -> {
        CodeBlock.builder()
            .addStatement("reader.readObject<%T>·{ reader ->", typeRef.asTypeName())
            .indent()
            .addStatement("%T(reader)", typeRef.asTypeName())
            .unindent()
            .add("}")
            .build()
      }
      is FieldType.Array -> {
        CodeBlock.builder()
            .addStatement("reader.readList<%T>·{ reader ->", rawType.asTypeName())
            .indent()
            .add(rawType.readListItemCode())
            .unindent()
            .add("\n}")
            .applyIf(!isOptional) { add(".map·{ it!! }") }
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
        .beginControlFlow("return %T.invoke·{ writer ->", ResponseFieldMarshaller::class)
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
            .beginControlFlow("writer.writeList(%L, this@%L.%L)·{ value, listItemWriter ->", field, thisRef, name)
            .beginControlFlow("value?.forEach·{ value ->")
            .add(type.writeListItemCode)
            .endControlFlow()
            .endControlFlow()
            .build()
      }
      is FieldType.Fragments -> CodeBlock.of("this@%L.%L.marshaller().marshal(writer)\n", thisRef, name)
    }
  }

  private val FieldType.Array.writeListItemCode: CodeBlock
    get() {
      val safeValue = if (isOptional) "value?" else "value"
      return when (rawType) {
        is FieldType.Scalar -> when (rawType) {
          is FieldType.Scalar.String -> CodeBlock.of("listItemWriter.writeString(value)")
          is FieldType.Scalar.Int -> CodeBlock.of("listItemWriter.writeInt(value)")
          is FieldType.Scalar.Boolean -> CodeBlock.of("listItemWriter.writeBoolean(value)")
          is FieldType.Scalar.Float -> CodeBlock.of("listItemWriter.writeDouble(value)")
          is FieldType.Scalar.Enum -> CodeBlock.of("listItemWriter.writeString($safeValue.rawValue)")
          is FieldType.Scalar.Custom -> {
            CodeBlock.of("listItemWriter.writeCustom(%T.%L, value)", rawType.customEnumType.asTypeName(), rawType.customEnumConst)
          }
        }
        is FieldType.Object -> CodeBlock.of("listItemWriter.writeObject($safeValue.marshaller())", asTypeName())
        is FieldType.Array -> {
          CodeBlock.builder()
              .beginControlFlow(
                  "listItemWriter.writeList(value)·{ value, listItemWriter ->",
                  List::class.asClassName().parameterizedBy(rawType.rawType.asTypeName())
              )
              .beginControlFlow(
                  "value?.forEach·{ value ->", // value always nullable in ListItemWriter
                  List::class.asClassName().parameterizedBy(rawType.rawType.asTypeName())
              )
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

  fun TypeRef.asTypeName() = ClassName(packageName, name.capitalize())

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
      this is Number -> CodeBlock.of("%S", this.toString()) // TODO: replace with actual numbers instead of relying on coercion
      this is Boolean -> CodeBlock.of("%S", this.toString())
      else -> throw IllegalStateException("Cannot generate code for $this")
    }
  }

  fun TypeSpec.patchKotlinNativeOptionalArrayProperties(): TypeSpec {
    val patchedNestedTypes = typeSpecs.map { it.patchKotlinNativeOptionalArrayProperties() }
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
}
