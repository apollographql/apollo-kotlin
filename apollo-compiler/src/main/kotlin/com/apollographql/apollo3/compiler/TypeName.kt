package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.java.JavaAnnotations
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName

fun TypeName.isList() =
  (this is ParameterizedTypeName && rawType == JavaClassNames.List)

fun TypeName.listParamType(): TypeName {
  return (this as ParameterizedTypeName)
    .typeArguments
    .first()
    .let { if (it is WildcardTypeName) it.upperBounds.first() else it }
}

fun TypeName.isOptional(expectedOptionalType: ClassName? = null): Boolean {
  val rawType = (this as? ParameterizedTypeName)?.rawType ?: this
  return if (expectedOptionalType == null) {
    rawType == JavaClassNames.Optional || rawType == JavaClassNames.Input
  } else {
    rawType == expectedOptionalType
  }
}

fun TypeName.unwrapOptionalType(withoutAnnotations: Boolean = false): TypeName {
  return if (isOptional()) {
    val unwrappedTypeName = (this as ParameterizedTypeName).typeArguments.first()
    if (unwrappedTypeName == JavaClassNames.Object) {
      // Workaround for "annotation @org.jetbrains.annotations.Nullable not applicable in this type context"
      unwrappedTypeName
    } else {
      unwrappedTypeName.annotated(JavaAnnotations.Nullable)
    }
  } else {
    this
  }.let { if (withoutAnnotations) it.withoutAnnotations() else it }
}

fun TypeName.unwrapOptionalValue(
  varName: String,
  checkIfPresent: Boolean = true,
  transformation: ((CodeBlock) -> CodeBlock)? = null
): CodeBlock {
  return if (isOptional() && this is ParameterizedTypeName) {
    if (rawType == JavaClassNames.Input) {
      val valueCode = CodeBlock.of("\$L.value", varName)
      if (checkIfPresent) {
        CodeBlock.of("\$L != null ? \$L : null", valueCode, transformation?.invoke(valueCode) ?: valueCode)
      } else {
        transformation?.invoke(valueCode) ?: valueCode
      }
    } else {
      val valueCode = CodeBlock.of("\$L.get()", varName)
      if (checkIfPresent) {
        CodeBlock.of("\$L.isPresent() ? \$L : null", varName, transformation?.invoke(valueCode) ?: valueCode)
      } else {
        transformation?.invoke(valueCode) ?: valueCode
      }
    }
  } else {
    val valueCode = CodeBlock.of("\$L", varName)
    if (annotations.contains(JavaAnnotations.Nullable) && checkIfPresent && transformation != null) {
      CodeBlock.of("\$L != null ? \$L : null", varName, transformation.invoke(valueCode))
    } else {
      transformation?.invoke(valueCode) ?: valueCode
    }
  }
}

fun TypeName.wrapOptionalValue(value: CodeBlock): CodeBlock {
  return if (this.isOptional() && this is ParameterizedTypeName) {
    CodeBlock.of("\$T.fromNullable(\$L)", rawType, value)
  } else {
    value
  }
}

fun TypeName.defaultOptionalValue(): CodeBlock {
  return if (this.isOptional() && this is ParameterizedTypeName) {
    CodeBlock.of("\$T.absent()", rawType)
  } else {
    CodeBlock.of("")
  }
}
