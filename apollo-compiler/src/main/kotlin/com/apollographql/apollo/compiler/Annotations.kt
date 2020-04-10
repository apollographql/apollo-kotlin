package com.apollographql.apollo.compiler

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.CodeBlock
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

object Annotations {
  val NULLABLE: AnnotationSpec = AnnotationSpec.builder(Nullable::class.java).build()
  val NONNULL: AnnotationSpec = AnnotationSpec.builder(NotNull::class.java).build()
  val OVERRIDE: AnnotationSpec = AnnotationSpec.builder(Override::class.java).build()
  val SUPPRESS_RAW_VALUE_WARNING: AnnotationSpec = AnnotationSpec.builder(SuppressWarnings::class.java)
      .addMember( "value", CodeBlock.of( "\$S", "rawtypes")).build()
  val SUPPRESS_UNCHECKED_WARNING: AnnotationSpec = AnnotationSpec.builder(SuppressWarnings::class.java)
      .addMember( "value", CodeBlock.of( "\$S", "unchecked")).build()
  val SUPPRESS_RAW_VALUE_AND_UNCHECKED_WARNING: AnnotationSpec = AnnotationSpec.builder(SuppressWarnings::class.java)
      .addMember("value", CodeBlock.of("\$L", "{\"rawtypes\", \"unchecked\"}")).build()
  val DEPRECATED: AnnotationSpec = AnnotationSpec.builder(java.lang.Deprecated::class.java).build()
}