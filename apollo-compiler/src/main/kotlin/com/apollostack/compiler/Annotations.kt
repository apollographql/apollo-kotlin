package com.apollostack.compiler

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.CodeBlock
import javax.annotation.Generated
import javax.annotation.Nonnull
import javax.annotation.Nullable

object Annotations {
  val NULLABLE: AnnotationSpec = AnnotationSpec.builder(Nullable::class.java).build()
  val NONNULL: AnnotationSpec = AnnotationSpec.builder(Nonnull::class.java).build()
  val OVERRIDE: AnnotationSpec = AnnotationSpec.builder(Override::class.java).build()
  val GENERATED_BY_APOLLO: AnnotationSpec = AnnotationSpec.builder(Generated::class.java)
      .addMember("value", CodeBlock.of("\$S", "Apollo GraphQL")).build()
}