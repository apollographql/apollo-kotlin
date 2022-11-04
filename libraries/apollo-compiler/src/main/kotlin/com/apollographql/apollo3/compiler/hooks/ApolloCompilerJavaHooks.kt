package com.apollographql.apollo3.compiler.hooks

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks.FileInfo
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile

@ApolloExperimental
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
   * This will be called once after preparing all files.
   * To keep the files as-is, return [files].
   */
  fun postProcessFiles(files: Collection<FileInfo>): Collection<FileInfo>

  /**
   * The default implementation of [ApolloCompilerJavaHooks] that overrides nothing.
   */
  @ApolloInternal
  object Identity : DefaultApolloCompilerJavaHooks() {
    override val version: String = "ApolloCompilerJavaHooks.Identity.0"
  }

  @ApolloExperimental
  data class FileInfo(
      /**
       * The JavaPoet representation of the file.
       */
      val javaFile: JavaFile,
  )
}

@ApolloExperimental
abstract class DefaultApolloCompilerJavaHooks : ApolloCompilerJavaHooks {
  override fun postProcessFiles(files: Collection<FileInfo>) = files
  override fun overrideResolvedType(key: ResolverKey, resolved: ClassName?) = resolved
}

