package com.apollostack.compiler

import com.squareup.javapoet.AnnotationSpec
import javax.annotation.Nullable

fun String.normalizeTypeName() = removeSuffix("!").removeSurrounding("[", "]").removeSuffix("!")

object JavaPoetUtils {
  val NULLABLE_ANNOTATION = AnnotationSpec.builder(Nullable::class.java).build()
}
