package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ast.AST
import com.apollographql.apollo.compiler.ir.ScalarType
import com.squareup.kotlinpoet.*
import javax.annotation.Generated

internal fun Any.toDefaultValueCodeBlock(typeName: TypeName, fieldType: AST.FieldType): CodeBlock = when {
  this is Number -> CodeBlock.of("%L%L", castTo(typeName), if (typeName == LONG) "L" else "")
  fieldType is AST.FieldType.Scalar.Enum -> CodeBlock.of("%T.safeValueOf(%S)", typeName, this)
  fieldType is AST.FieldType.Array -> (this as List<Any>).toDefaultValueCodeBlock(typeName, fieldType)
  this !is String -> CodeBlock.of("%L", this)
  else -> CodeBlock.of("%S", this)
}

internal fun List<Any>.toDefaultValueCodeBlock(typeName: TypeName, fieldType: AST.FieldType.Array): CodeBlock {
  val codeBuilder = CodeBlock.builder().add("listOf(")
  return filterNotNull()
      .map { it.toDefaultValueCodeBlock((typeName as ParameterizedTypeName).typeArguments.first(), fieldType.rawType) }
      .foldIndexed(codeBuilder) { index, builder, code ->
        builder.add(if (index > 0) ", " else "").add(code)
      }
      .add(")")
      .build()
}

internal fun Number.castTo(type: TypeName): Number = when (type) {
  INT -> toInt()
  LONG -> toLong()
  FLOAT, DOUBLE -> toDouble()
  else -> this
}

internal fun AST.TypeRef.asTypeName() = ClassName.bestGuess(
    if (packageName.isNotBlank()) "$packageName.${name.capitalize()}" else name.capitalize())

internal fun Map<String, Any>.toCode(): CodeBlock? {
  return takeIf { it.isNotEmpty() }?.let {
    it.map { it.toCode() }
        .foldIndexed(CodeBlock.builder().add("mapOf<String, Any>(\n").indent()) { index, builder, code ->
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

private fun Map.Entry<String, Any>.toCode(): CodeBlock {
  return when (value) {
    is Map<*, *> -> CodeBlock.of("%S to %L", key, (value as Map<String, Any>).toCode())
    else -> CodeBlock.of("%S to %S", key, value)
  }
}

internal fun Any.normalizeJsonValue(graphQLType: String): Any {
  return when (this) {
    is Number -> {
      val scalarType = ScalarType.forName(graphQLType.removeSuffix("!"))
      when (scalarType) {
        is ScalarType.INT -> toInt()
        is ScalarType.FLOAT -> toDouble()
        else -> this
      }
    }
    is Boolean, is Map<*, *> -> this
    is List<*> -> this.map {
      it!!.normalizeJsonValue(graphQLType.removeSuffix("!").removePrefix("[").removeSuffix("]"))
    }
    else -> toString()
  }
}