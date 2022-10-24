package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec

interface ApolloCompilerKotlinHooks {
  /**
   * The version of this [ApolloCompilerKotlinHooks].
   *
   * Change the version every time the implementation changes to let gradle and build tools know that they have to re-generate the
   * resulting files.
   */
  val version: String

  /**
   * Allows overriding the names of classes that are referenced in the generated code.
   *
   * This is called every time a class needs to be resolved during code generation.
   * To keep a name as-is, return [resolved].
   *
   * @param key the key of the class to resolve
   * @param resolved the name of the class as it was resolved by the compiler
   */
  fun overrideResolvedType(key: ResolverKey, resolved: ClassName?): ClassName?

  /**
   * Allows processing the generated files right before they are written to the disk.
   *
   * This will be called once per file.
   * To keep a file as-is, return [fileSpec].
   *
   * @param fileSpec the KotlinPoet representation of the file
   */
  fun postProcessFileSpec(fileSpec: FileSpec): FileSpec

  /**
   * The default implementation of [ApolloCompilerKotlinHooks] that overrides nothing.
   */
  object Identity : DefaultApolloCompilerKotlinHooks() {
    override val version: String = "ApolloCompilerKotlinHooks.Identity.0"
  }
}

abstract class DefaultApolloCompilerKotlinHooks : ApolloCompilerKotlinHooks {
  override fun postProcessFileSpec(fileSpec: FileSpec) = fileSpec
  override fun overrideResolvedType(key: ResolverKey, resolved: ClassName?) = resolved
}

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
