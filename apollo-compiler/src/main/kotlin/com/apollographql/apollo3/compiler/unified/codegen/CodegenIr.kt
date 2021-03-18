package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.compiler.unified.AstExtFragmentImplementation
import com.apollographql.apollo3.compiler.unified.AstExtFragmentInterfaces
import com.apollographql.apollo3.compiler.unified.AstExtOperation

data class CodegenIr(
    val extOperations: List<AstExtOperation> = emptyList(),
    val extFragmentInterfaces: List<AstExtFragmentInterfaces> = emptyList(),
    val extFragmentImplementations: List<AstExtFragmentImplementation> = emptyList(),

    ) {
}