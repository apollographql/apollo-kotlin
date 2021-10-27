package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.VersionNumber
import com.apollographql.apollo3.compiler.codegen.CodegenLayout

class KotlinContext(
    val layout : CodegenLayout,
    val resolver: KotlinResolver,
    val targetLanguageVersion: VersionNumber,
)
