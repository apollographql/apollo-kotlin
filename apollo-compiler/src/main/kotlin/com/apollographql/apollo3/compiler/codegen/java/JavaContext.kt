package com.apollographql.apollo3.compiler.codegen.java

internal class JavaContext(
    val layout: JavaCodegenLayout,
    val resolver: JavaResolver,
    val generateModelBuilder: Boolean,
)
