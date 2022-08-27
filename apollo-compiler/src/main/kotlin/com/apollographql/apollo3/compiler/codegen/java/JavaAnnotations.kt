package com.apollographql.apollo3.compiler.codegen.java

import com.squareup.javapoet.AnnotationSpec
import org.jetbrains.annotations.NotNull as JetBrainsNonNull
import org.jetbrains.annotations.Nullable as JetBrainsNullable

object JavaAnnotations {
  val Nullable: AnnotationSpec = AnnotationSpec.builder(JetBrainsNullable::class.java).build()
  val NonNull: AnnotationSpec = AnnotationSpec.builder(JetBrainsNonNull::class.java).build()
}
