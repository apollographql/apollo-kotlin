package com.apollographql.apollo.compiler

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.CodeBlock
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import javax.annotation.Generated

object Annotations {
  val NULLABLE: AnnotationSpec = AnnotationSpec.builder(Nullable::class.java).build()
  val NONNULL: AnnotationSpec = AnnotationSpec.builder(NotNull::class.java).build()
  val OVERRIDE: AnnotationSpec = AnnotationSpec.builder(Override::class.java).build()
  val GENERATED_BY_APOLLO: AnnotationSpec = AnnotationSpec.builder(Generated::class.java)
      .addMember("value", CodeBlock.of("\$S", "Apollo GraphQL")).build()
  val SUPPRESS_WARNINGS: AnnotationSpec = AnnotationSpec.builder(SuppressWarnings::class.java)
          .addMember( "value", CodeBlock.of( "\$S", "rawtypes")).build()
  val DEPRECATED: AnnotationSpec = AnnotationSpec.builder(java.lang.Deprecated::class.java).build()
}