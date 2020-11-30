package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
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
import com.squareup.kotlinpoet.joinToCode

internal fun CodeGenerationAst.FieldType.asTypeName(): TypeName {
  return when (this) {
    is CodeGenerationAst.FieldType.Scalar -> when (this) {
      is CodeGenerationAst.FieldType.Scalar.ID,
      is CodeGenerationAst.FieldType.Scalar.String -> String::class.asClassName()
      is CodeGenerationAst.FieldType.Scalar.Int -> INT
      is CodeGenerationAst.FieldType.Scalar.Boolean -> BOOLEAN
      is CodeGenerationAst.FieldType.Scalar.Float -> DOUBLE
      is CodeGenerationAst.FieldType.Scalar.Enum -> typeRef.asTypeName()
      is CodeGenerationAst.FieldType.Scalar.Custom -> ClassName.bestGuess(type.escapeKotlinReservedWord())
    }
    is CodeGenerationAst.FieldType.Object -> typeRef.asTypeName()
    is CodeGenerationAst.FieldType.Array -> List::class.asClassName().parameterizedBy(rawType.asTypeName())
  }.copy(nullable = nullable)
}

internal fun CodeGenerationAst.TypeRef.asTypeName(): ClassName {
  return if (this.enclosingType == null) {
    ClassName(packageName = this.packageName, this.name.escapeKotlinReservedWord())
  } else {
    val enclosingType = this.enclosingType.asTypeName()
    val packageName = this.packageName.takeIf { it.isNotEmpty() } ?: enclosingType.packageName
    ClassName(packageName = packageName, enclosingType.simpleNames + this.name.escapeKotlinReservedWord())
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
          name = name.escapeKotlinReservedWord(),
          type = type.asTypeName()
      )
      .applyIf(override) { addModifiers(KModifier.OVERRIDE) }
      .applyIf(deprecationReason != null) { addAnnotation(deprecatedAnnotation(deprecationReason!!)) }
      .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
      .applyIf(initializer != null) { initializer(initializer!!) }
      .build()
}

private fun Number.castTo(type: TypeName): Number {
  return when (type) {
    INT -> toInt()
    LONG -> toLong()
    FLOAT, DOUBLE -> toDouble()
    else -> this
  }
}

internal fun Collection<CodeGenerationAst.TypeRef>.accessorProperties(): List<PropertySpec> {
  return map { type ->
    PropertySpec.builder("as${type.name.escapeKotlinReservedWord()}", type.asTypeName().copy(nullable = true))
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

internal fun CodeGenerationAst.Field.asOptionalParameterSpec(withDefaultValue: Boolean = true): ParameterSpec {
  return ParameterSpec
      .builder(this.responseName, this.type.asTypeName().copy(nullable = true))
      .applyIf(withDefaultValue) { defaultValue("null") }
      .build()
}
