package com.apollostack.compiler

import com.squareup.javapoet.AnnotationSpec
import javax.annotation.Nonnull
import javax.annotation.Nullable

object Annotations {
  val NULLABLE: AnnotationSpec = AnnotationSpec.builder(Nullable::class.java).build()
  val NONNULL: AnnotationSpec = AnnotationSpec.builder(Nonnull::class.java).build()
  val OVERRIDE: AnnotationSpec = AnnotationSpec.builder(Override::class.java).build()
}