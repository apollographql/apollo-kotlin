package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.InputFile
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import java.lang.reflect.InvocationTargetException


internal fun FileCollection.toInputFiles(): List<InputFile> {
  val inputFiles = mutableListOf<InputFile>()

  asFileTree.visit {
    if (it.file.isFile) {
      inputFiles.add(InputFile(it.file, it.path))
    }
  }

  return inputFiles
}

/**
 * Isolates inputs so we can use them from a separate classloader.
 *
 * See also https://github.com/gradle/gradle/issues/28147 for why this is needed.
 */
internal fun FileCollection.isolate(): List<Any> = toInputFiles().isolate()
internal fun List<InputFile>.isolate(): List<Any> = flatMap { listOf(it.normalizedPath, it.file) }


internal fun ProjectDependency.getPathCompat(): String {
  val method = this::class.java.methods.firstOrNull {
    it.name == "getPath" && it.parameters.isEmpty()
  }
  return if (method != null) {
    // Gradle 8.11+ path
    // See https://docs.gradle.org/8.11/userguide/upgrading_version_8.html#deprecate_get_dependency_project
    method.invoke(this) as String
  } else {
    dependencyProject.path
  }
}

internal fun Any.reflectiveCall(methodName: String, vararg args: Any?) {
  try {
    javaClass.declaredMethods.single { it.name == methodName }.invoke(this, *args)
  } catch (e: InvocationTargetException) {
    /**
     * Unwrap the exception so it's displayed in Gradle error messages:
     *
     * Execution failed for task ':generateServiceApolloSources'.
     * > A failure occurred while executing com.apollographql.apollo.gradle.internal.GenerateSources
     *    > e: operation.graphql: (2, 5): Can't query `foo2` on type `Query`
     *      ----------------------------------------------------
     *      [1]:query GetFoo {
     *      [2]:    foo2
     *      [3]:}
     *      ----------------------------------------------------
     */
    throw e.cause!!
  }
}
