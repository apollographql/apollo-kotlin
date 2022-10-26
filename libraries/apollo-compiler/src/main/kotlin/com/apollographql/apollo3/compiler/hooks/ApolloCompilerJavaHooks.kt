package com.apollographql.apollo3.compiler.hooks

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile

interface ApolloCompilerJavaHooks {
  /**
   * The version of this [ApolloCompilerJavaHooks].
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
   * To keep a file as-is, return [javaFile].
   *
   * @param javaFile the KotlinPoet representation of the file
   */
  fun postProcessJavaFile(javaFile: JavaFile): JavaFile

  /**
   * The default implementation of [ApolloCompilerJavaHooks] that overrides nothing.
   */
  @ApolloInternal
  object Identity : DefaultApolloCompilerJavaHooks() {
    override val version: String = "ApolloCompilerJavaHooks.Identity.0"
  }
}

abstract class DefaultApolloCompilerJavaHooks : ApolloCompilerJavaHooks {
  override fun postProcessJavaFile(javaFile: JavaFile) = javaFile
  override fun overrideResolvedType(key: ResolverKey, resolved: ClassName?) = resolved
}

