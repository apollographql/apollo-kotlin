package com.apollographql.apollo.gradle

import com.apollographql.apollo.compiler.*
import com.apollographql.apollo.gradle.api.CompilationUnit
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.util.PatternSet
import java.io.File

class DefaultCompilationUnit(
    override val serviceName: String,
    override val variantName: String,
    override val androidVariant: Any?,
    val files: List<File>,
    val schemaFile: File?,
    val schemaPackageName: String,
    val rootPackageName: String,
    project: Project
): CompilationUnit {
  val name = "${variantName}${serviceName.capitalize()}"
  val outputDirectory = project.buildDir.child("generated", "source", "apollo", "classes", variantName, serviceName)
  val transformedQueriesDirectory = project.buildDir.child("generated", "transformedQueries", "apollo", variantName, serviceName)

  override lateinit var outputDir: Provider<Directory>
  override lateinit var transformedQueriesDir: Provider<Directory>

  companion object {
    fun from(project: Project, apolloVariant: ApolloVariant, service: Service): DefaultCompilationUnit {
      val sourceSetNames = apolloVariant.sourceSetNames

      val schemaFilePath = service.schemaFilePath
      if (schemaFilePath == null) {
        throw IllegalArgumentException("Please define schemaFilePath for service '${service.name}'")
      }

      var schemaFile = project.projectDir.child(schemaFilePath)
      if (!schemaFile.isFile) {
        throw IllegalArgumentException("No schema found at:\n${schemaFile.absolutePath}")
      }
      val schemaKey = schemaFile.canonicalPath.relativePathToGraphql()
      if (schemaKey != null) {
        // schema is under src/{foo}/graphql, see if it is overriden by anoter variant
        val schemaCandidates = findFilesInSourceSets(project, sourceSetNames, schemaKey) { true }
        schemaFile = schemaCandidates.values.first()
      }

      val sourceFolderPath = if (service.sourceFolderPath != null) {
        service.sourceFolderPath!!
      } else {
        // if schemaFilePath is outside src/{foo}/graphql, search the whole graphql folder
        schemaFile.canonicalPath.relativePathToGraphql(dropLast = 1) ?: "."
      }

      val candidateFiles = findFilesInSourceSets(project, sourceSetNames, sourceFolderPath, ::isGraphQL).values.toList()

      val files = if (service.exclude != null) {
        val patternSet = PatternSet()
        patternSet.exclude(service.exclude!!)
        project.files(candidateFiles).asFileTree.matching(patternSet).toList()
      } else {
        candidateFiles
      }

      val schemaPackageName = schemaFile.canonicalPath.formatPackageName(dropLast = 1) ?: ""

      return DefaultCompilationUnit(
          serviceName = service.name,
          variantName = apolloVariant.name,
          files = files,
          schemaFile = schemaFile,
          schemaPackageName = schemaPackageName,
          rootPackageName = service.rootPackageName ?: "",
          androidVariant = apolloVariant.androidVariant,
          project = project
      )
    }

    fun default(project: Project, apolloVariant: ApolloVariant): List<DefaultCompilationUnit> {
      val sourceSetNames = apolloVariant.sourceSetNames
      val schemaFiles = findFilesInSourceSets(project, sourceSetNames, ".") {
        it.name == "schema.json"
      }

      if (schemaFiles.isEmpty()) {
        return emptyList()
      }

      var i = 0
      val services = schemaFiles.entries
          .sortedBy { it.value.canonicalPath } // make sure the order is predicable for tests
          .map { entry ->
            val sourceFolderPath = entry.value.canonicalPath.relativePathToGraphql(dropLast = 1)!!
            val files = findFilesInSourceSets(project, sourceSetNames, sourceFolderPath, ::isGraphQL).values.toList()

            val name = "service${i++}"

            DefaultCompilationUnit(
                serviceName = name,
                variantName = apolloVariant.name,
                files = files,
                schemaFile = entry.value,
                schemaPackageName = entry.value.canonicalPath.formatPackageName(dropLast = 1)!!,
                rootPackageName = "",
                androidVariant = apolloVariant.androidVariant,
                project = project
            )
          }

      return services
    }

    fun isGraphQL(file: File): Boolean {
      return file.name.endsWith(".graphql") || file.name.endsWith(".gql")
    }

    /**
     * Finds the files in the given sourceSets taking into account their precedence according to the android plugin order
     *
     * Returns a map with the relative path to the path as key and the file as value
     *
     * Files coming last will have higher priorities that the first ones.
     */
    private fun findFilesInSourceSets(project: Project, sourceSetNames: List<String>, path: String, predicate: (File) -> Boolean): Map<String, File> {
      val candidates = mutableMapOf<String, File>()
      sourceSetNames.forEach { sourceSetName ->
        val root = project.projectDir.child("src", sourceSetName, "graphql", path)
        val files = root.findFiles(predicate)

        files.forEach {
          val key = if (root.isFile) {
            // toRelativeString only works on directories.
            ""
          } else {
            it.toRelativeString(root)
          }

          // overwrite the previous entry if it was there already
          // this is ok as Android orders the sourceSetNames from lower to higher priority
          candidates[key] = it
        }
      }
      return candidates
    }

    private fun File.findFiles(predicate: (File) -> Boolean): List<File> {
      return when {
        isDirectory -> listFiles()?.flatMap { it.findFiles(predicate) } ?: emptyList()
        isFile && predicate(this) -> listOf(this)
        else -> emptyList()
      }
    }
  }
}