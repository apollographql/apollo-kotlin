package com.apollographql.apollo3.compiler.hooks.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks.FileInfo
import com.squareup.kotlinpoet.ClassName

@ApolloInternal
class ApolloCompilerKotlinHooksChain(
    private val hooks: List<ApolloCompilerKotlinHooks>,
) : ApolloCompilerKotlinHooks {
  override val version: String = hooks.joinToString("/") { it.version }

  override fun postProcessFiles(files: Collection<FileInfo>): Collection<FileInfo> {
    return hooks.fold(files) { acc, hook ->
      hook.postProcessFiles(acc)
    }
  }

  override fun overrideResolvedType(key: ResolverKey, resolved: ClassName?): ClassName? {
    return hooks.fold(resolved) { acc, hook ->
      hook.overrideResolvedType(key, acc)
    }
  }
}
