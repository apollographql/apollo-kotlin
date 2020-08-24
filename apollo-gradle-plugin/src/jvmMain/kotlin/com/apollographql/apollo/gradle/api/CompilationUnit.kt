package com.apollographql.apollo.gradle.api

import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

/**
 * A [CompilationUnit] is a single invocation of the compiler. It is used by
 * [com.apollographql.apollo.gradle.internal.ApolloGenerateSourcesTask] to generate models.
 *
 * It inherits [CompilerParams] so individual parameters can be directly set on the [CompilationUnit]
 */
interface CompilationUnit: CompilerParams {
  /**
   * The name of the [CompilationUnit]
   */
  val name: String
  /**
   * The name of the [Service] used by this [CompilationUnit]
   */
  val serviceName: String
  /**
   * The name of the variant used by this [CompilationUnit]
   */
  val variantName: String
  /**
   * If on Android, this will contain the Android Variant. It is safe to cast it to [com.android.build.gradle.api.BaseVariant]
   */
  val androidVariant: Any?

  /**
   * A json file containing a [Map]<[String], [com.apollographql.apollo.compiler.operationoutput.OperationDescriptor]>
   */
  val operationOutputFile: Provider<RegularFile>

  /**
   * The directory where the generated models will be written
   */
  val outputDir: Provider<Directory>
}
