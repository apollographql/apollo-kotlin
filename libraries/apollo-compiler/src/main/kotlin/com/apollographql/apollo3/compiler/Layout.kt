package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.ResolverClassName

interface Layout {
  fun className(kind: FileKind, name: String, filePath: String?): ResolverClassName
}

sealed interface Element {

}
enum class FileKind {
  Type,
  Query,
  Mutation,
  Subscription,
  Fragment,
  Schema,
  Resolver
}

fun Layout(
    codegenSchema: CodegenSchema, 
    rootPackageName: String, 
    roots: Set<String>?, 
    useSemanticNaming: Boolean
): Layout = LayoutImpl(codegenSchema, rootPackageName, roots, useSemanticNaming)