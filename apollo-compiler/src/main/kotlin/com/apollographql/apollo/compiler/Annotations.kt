package com.apollographql.apollo.compiler

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.CodeBlock
import javax.annotation.Generated
import javax.annotation.Nonnull
import javax.annotation.Nullable

object Annotations {
  val NULLABLE: com.squareup.javapoet.AnnotationSpec = com.squareup.javapoet.AnnotationSpec.builder(
      javax.annotation.Nullable::class.java).build()
  val NONNULL: com.squareup.javapoet.AnnotationSpec = com.squareup.javapoet.AnnotationSpec.builder(
      javax.annotation.Nonnull::class.java).build()
  val OVERRIDE: com.squareup.javapoet.AnnotationSpec = com.squareup.javapoet.AnnotationSpec.builder(Override::class.java).build()
  val GENERATED_BY_APOLLO: com.squareup.javapoet.AnnotationSpec = com.squareup.javapoet.AnnotationSpec.builder(
      javax.annotation.Generated::class.java)
      .addMember("value", com.squareup.javapoet.CodeBlock.of("\$S", "Apollo GraphQL")).build()
}