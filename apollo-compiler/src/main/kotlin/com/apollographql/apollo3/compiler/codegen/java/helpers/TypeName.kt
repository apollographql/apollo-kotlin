package com.apollographql.apollo3.compiler.codegen.java.helpers

import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

internal fun TypeName.isOptional(expectedOptionalType: ClassName? = null): Boolean {
  val rawType = (this as? ParameterizedTypeName)?.rawType ?: this
  return if (expectedOptionalType == null) {
    rawType == JavaClassNames.Optional
  } else {
    rawType == expectedOptionalType
  }
}

internal fun TypeName.unwrapOptionalType(withoutAnnotations: Boolean = false): TypeName {
  return if (isOptional()) {
    val unwrappedTypeName = (this as ParameterizedTypeName).typeArguments.first()
    if (unwrappedTypeName == JavaClassNames.Object) {
      // Workaround for "annotation @org.jetbrains.annotations.Nullable not applicable in this type context"
      unwrappedTypeName
    } else {
      unwrappedTypeName.annotated(AnnotationSpec.builder(JavaClassNames.JetBrainsNullable).build())
    }
  } else {
    this
  }.let { if (withoutAnnotations) it.withoutAnnotations() else it }
}

internal fun TypeName.wrapOptionalValue(value: CodeBlock): CodeBlock {
  return if (this.isOptional() && this is ParameterizedTypeName) {
    CodeBlock.of("\$T.presentIfNotNull(\$L)", rawType, value)
  } else {
    value
  }
}
