package com.apollographql.apollo3.compiler.hooks

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec

@ApolloExperimental
@Deprecated("Use Plugin.kotlinOutputTransform", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
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
   * This will be called once after preparing all files.
   * To keep the files as-is, return [files].
   */
  fun postProcessFiles(files: Collection<FileInfo>): Collection<FileInfo>
  
  @ApolloExperimental
  data class FileInfo(
      /**
       * The KotlinPoet representation of the file.
       */
      val fileSpec: FileSpec,
  )
}
