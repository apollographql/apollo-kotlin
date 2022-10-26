package com.apollographql.apollo3.compiler.hooks.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec

@ApolloInternal
class ApolloCompilerKotlinHooksChain(
    private val hooks: List<ApolloCompilerKotlinHooks>,
) : ApolloCompilerKotlinHooks {
  override val version: String = hooks.joinToString("/") { it.version }

  override fun postProcessFileSpec(fileSpec: FileSpec): FileSpec {
    return hooks.fold(fileSpec) { acc, hook ->
      hook.postProcessFileSpec(acc)
    }
  }

  override fun overrideResolvedType(key: ResolverKey, resolved: ClassName?): ClassName? {
    return hooks.fold(resolved) { acc, hook ->
      hook.overrideResolvedType(key, acc)
    }
  }
}
