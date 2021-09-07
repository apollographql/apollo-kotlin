package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.codegen.CodegenLayout

class JavaContext(
    val layout : CodegenLayout,
    val resolver: JavaResolver
)