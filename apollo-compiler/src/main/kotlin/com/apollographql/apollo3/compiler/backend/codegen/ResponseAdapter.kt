package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.internal.ListResponseAdapter
import com.apollographql.apollo3.api.internal.NullableResponseAdapter
import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

internal fun CodeGenerationAst.OperationType.responseAdapterTypeSpec(generateAsInternal: Boolean = false): TypeSpec {
  return this.dataType
      .copy(name = "${this.name.escapeKotlinReservedWord()}_ResponseAdapter")
      .rootResponseAdapterTypeSpec(generateAsInternal)
}

internal fun CodeGenerationAst.FragmentType.responseAdapterTypeSpec(generateAsInternal: Boolean = false): TypeSpec {
  val dataType = this.implementationType.nestedObjects.single()
  return dataType
      .copy(name = "${this.implementationType.name.escapeKotlinReservedWord()}_ResponseAdapter")
      .rootResponseAdapterTypeSpec(generateAsInternal)
}

private fun CodeGenerationAst.ObjectType.rootResponseAdapterTypeSpec(generateAsInternal: Boolean = false): TypeSpec {
  return this.responseAdapterTypeSpec()
      .toBuilder()
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addAnnotation(suppressWarningsAnnotation)
      .build()
}

private fun CodeGenerationAst.ObjectType.responseAdapterTypeSpec(): TypeSpec {

  return TypeSpec.classBuilder(this.name)
      .primaryConstructor(
          FunSpec.constructorBuilder()
              .addParameter(ParameterSpec.builder("customScalarAdapters", ResponseAdapterCache::class.asTypeName()).build())
              .build()
      )
      .applyIf(!isTypeCase) { addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(this@responseAdapterTypeSpec.typeRef.asTypeName())) }
      .apply {
        if (fields.isNotEmpty()) {
          if (kind is CodeGenerationAst.ObjectType.Kind.Object) {
            addType(companionObjectTypeSpec(this@responseAdapterTypeSpec))
            addProperties(adapterPropertySpecs(this@responseAdapterTypeSpec))
          } else if (kind is CodeGenerationAst.ObjectType.Kind.Fragment) {
            addProperties(objectAdapterPropertySpecs(kind))
          }
        }
      }
      .addFunction(readFromResponseFunSpec())
      .addFunction(writeToResponseFunSpec())
      .addTypes(
          this.nestedObjects
              .mapNotNull { nestedObject ->
                when {
                  nestedObject.kind is CodeGenerationAst.ObjectType.Kind.Object ||
                  (nestedObject.kind is CodeGenerationAst.ObjectType.Kind.Fragment && nestedObject.kind.possibleImplementations.isNotEmpty()) -> {
                    nestedObject.responseAdapterTypeSpec()
                  }
                  else -> null
                }
              }
      )
      .build()
}

private fun objectAdapterPropertySpecs(kind: CodeGenerationAst.ObjectType.Kind.Fragment): Iterable<PropertySpec> {
  return (kind.possibleImplementations.values.distinct() + kind.defaultImplementation).map { typeRef ->
    PropertySpec.builder(kotlinNameForTypeCaseAdapterField(typeRef), typeRef.asAdapterTypeName())
        .initializer("%L(customScalarAdapters)", typeRef.asAdapterTypeName())
        .build()
  }
}

private fun companionObjectTypeSpec(objectType: CodeGenerationAst.ObjectType): TypeSpec {
  return TypeSpec.companionObjectBuilder().apply {
    addProperty(responseFieldsPropertySpec(objectType))
    addProperty(responseNamesPropertySpec())
  }.build()
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

private fun adapterPropertySpecs(objectType: CodeGenerationAst.ObjectType): List<PropertySpec> {
  return objectType.fields.map { it.type }.toSet().map { it.adapterPropertySpec() }
}

private fun adapterInitializer(type: CodeGenerationAst.FieldType): CodeBlock {
  if (type.nullable) {
    return CodeBlock.of("%T(%L)", NullableResponseAdapter::class.asClassName(), adapterInitializer(type.nonNullable()))
  }
  return when (type) {
    is CodeGenerationAst.FieldType.Array -> CodeBlock.of("%T(%L)", ListResponseAdapter::class.asClassName(), adapterInitializer(type.rawType))
    is CodeGenerationAst.FieldType.Scalar.Boolean -> CodeBlock.of("%M", MemberName("com.apollographql.apollo3.api.internal", "booleanResponseAdapter"))
    is CodeGenerationAst.FieldType.Scalar.ID -> CodeBlock.of("%M", MemberName("com.apollographql.apollo3.api.internal", "stringResponseAdapter"))
    is CodeGenerationAst.FieldType.Scalar.String -> CodeBlock.of("%M", MemberName("com.apollographql.apollo3.api.internal", "stringResponseAdapter"))
    is CodeGenerationAst.FieldType.Scalar.Int -> CodeBlock.of("%M", MemberName("com.apollographql.apollo3.api.internal", "intResponseAdapter"))
    is CodeGenerationAst.FieldType.Scalar.Float -> CodeBlock.of("%M", MemberName("com.apollographql.apollo3.api.internal", "doubleResponseAdapter"))
    is CodeGenerationAst.FieldType.Scalar.Enum -> CodeBlock.of("%T", type.typeRef.asEnumAdapterTypeName().copy(nullable = false))
    is CodeGenerationAst.FieldType.Object -> CodeBlock.of("%T(customScalarAdapters)", type.typeRef.asAdapterTypeName().copy(nullable = false))
    is CodeGenerationAst.FieldType.Scalar.Custom -> CodeBlock.of(
        "customScalarAdapters.responseAdapterFor<%T>(%T)",
        ClassName.bestGuess(type.type),
        type.typeRef.asTypeName()
    )
  }
}

private fun CodeGenerationAst.FieldType.adapterPropertySpec(): PropertySpec {
  return PropertySpec
      .builder(
          name = kotlinNameForAdapterField(this),
          type = ResponseAdapter::class.asClassName().parameterizedBy(asTypeName())
      )
      .initializer(adapterInitializer(this))
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
    builder.add("arguments = %L,\n", arguments.takeIf { it.isNotEmpty() }?.let { anyToCode(it) } ?: "emptyMap()")
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
        is CodeGenerationAst.ObjectType.Kind.Fragment -> {
          kind.possibleImplementations.forEach {
            builder.add("%T(%S, %T.RESPONSE_FIELDS),\n", ResponseField.FieldSet::class, it.key, it.value.asAdapterTypeName())
          }
          builder.add("%T(null, %T.RESPONSE_FIELDS),\n", ResponseField.FieldSet::class, kind.defaultImplementation.asAdapterTypeName())
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

private fun anyToCode(any: Any?): CodeBlock {
  return with(any) {
    when {
      this == null -> CodeBlock.of("null")
      this is Map<*, *> && this.isEmpty() -> CodeBlock.of("emptyMap<%T,·Any?>()", String::class.asTypeName())
      this is Map<*, *> -> CodeBlock.builder()
          .add("mapOf<%T,·Any?>(\n", String::class.asTypeName())
          .indent()
          .add(map { CodeBlock.of("%S to %L", it.key, anyToCode(it.value)) }.joinToCode(separator = ",\n"))
          .unindent()
          .add(")")
          .build()
      this is List<*> && this.isEmpty() -> CodeBlock.of("emptyList<Any?>()")
      this is List<*> -> CodeBlock.builder()
          .add("listOf<Any?>(\n")
          .indent()
          .add(map { anyToCode(it) }.joinToCode(separator = ",\n"))
          .unindent()
          .add(")")
          .build()
      this is String -> CodeBlock.of("%S", this)
      this is Number -> CodeBlock.of("%L", this)
      this is Boolean -> CodeBlock.of("%L", this)
      else -> throw IllegalStateException("Cannot generate code for $this")
    }
  }
}