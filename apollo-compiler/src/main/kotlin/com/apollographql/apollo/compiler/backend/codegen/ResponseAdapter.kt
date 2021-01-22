package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
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
  return TypeSpec.objectBuilder(this.name)
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(this.typeRef.asTypeName()))
      .applyIf(
          this.fields.isNotEmpty()
              && this.kind is CodeGenerationAst.ObjectType.Kind.Object) {
        addProperty(responseFieldsPropertySpec(this@responseAdapterTypeSpec))
      }
      .addFunction(readFromResponseFunSpec())
      .addFunction(writeToResponseFunSpec())
      .addTypes(
          this.nestedObjects
              .filter {
                it.kind is CodeGenerationAst.ObjectType.Kind.Object
                    || (it.kind is CodeGenerationAst.ObjectType.Kind.Fragment && it.kind.possibleImplementations.isNotEmpty())
              }
              .map { nestedObject ->
                nestedObject.responseAdapterTypeSpec()
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


private fun responseFieldsPropertySpec(objectType: CodeGenerationAst.ObjectType): PropertySpec {
  val fields = objectType.fields

  val initializer = CodeBlock.builder()
      .addStatement("arrayOf(")
      .indent()
      .also { builder->
        builder.add(fields.map { field -> field.responseFieldInitializerCode(objectType) }.joinToCode(separator = ",\n"))
      }
      .unindent()
      .addStatement("")
      .add(")")
      .build()
  return PropertySpec
      .builder(
          name = "RESPONSE_FIELDS",
          type = Array<ResponseField>::class.asClassName().parameterizedBy(ResponseField::class.asClassName()),
      )
      .initializer(initializer)
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
  val builder =  CodeBlock.builder()
  builder.add("%T(", ResponseField.Type.NotNull::class.java)
  builder.add(this)
  builder.add(")")
  return builder.build()
}

private fun CodeGenerationAst.Field.responseFieldInitializerCode(objectType: CodeGenerationAst.ObjectType): CodeBlock {
    val builder = CodeBlock.builder().add("%T(\n", ResponseField::class)
    builder.indent()
    builder.add("type = %L,\n", type.toTypeCode())
    builder.add("responseName = %S,\n", responseName)
    builder.add("fieldName = %S,\n", schemaName)
    builder.add("arguments = %L,\n", arguments.takeIf { it.isNotEmpty() }?.let { anyToCode(it) } ?: "emptyMap()")
    builder.add("conditions = %L,\n", conditionsListCode(conditions))
    builder.add("possibleFieldSets = %L,\n", fieldsCode(this.type, objectType))
    builder.unindent()
    builder.add(")")

    return builder.build()
  }

private fun fieldsCode(type: CodeGenerationAst.FieldType, objectType: CodeGenerationAst.ObjectType): CodeBlock {
  return when (val leafType = type.leafType()) {
    is CodeGenerationAst.FieldType.Scalar -> CodeBlock.of("emptyMap()")
    is CodeGenerationAst.FieldType.Object -> {
      val nestedObjectType = objectType.nestedObjects.first { it.typeRef == leafType.typeRef }
      val builder = CodeBlock.Builder()
      builder.add("mapOf(\n")
      builder.indent()
      when (val kind = nestedObjectType.kind) {
        is CodeGenerationAst.ObjectType.Kind.Object -> {
          builder.add("%S to %T.RESPONSE_FIELDS\n", "", leafType.typeRef.asAdapterTypeName())
        }
        is CodeGenerationAst.ObjectType.Kind.Fragment -> {
          kind.possibleImplementations.forEach {
            builder.add("%S to %T.RESPONSE_FIELDS,\n", it.key, it.value.asAdapterTypeName())
          }
          builder.add("%S to %T.RESPONSE_FIELDS,\n", "", kind.defaultImplementation.asAdapterTypeName())
        }
      }
      builder.unindent()
      builder.add(")")
      builder.build()
    }
    else -> error("")
  }
}

private fun CodeGenerationAst.FieldType.leafType(): CodeGenerationAst.FieldType = when(this) {
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