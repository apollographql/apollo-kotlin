package com.apollographql.apollo.gradle

import com.apollographql.apollo.compiler.*
import org.gradle.api.Project
import org.gradle.api.tasks.util.PatternSet
import java.io.File

class ServiceVariant(
    val name: String,
    val files: List<File>,
    val schemaFile: File?,
    val schemaPackageName: String,
    val rootPackageName: String
) {

  companion object {
    fun from(project: Project, sourceSetNames: List<String>, service: Service): ServiceVariant {
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

      return ServiceVariant(
          name = service.name,
          files = files,
          schemaFile = schemaFile,
          schemaPackageName = schemaPackageName,
          rootPackageName = service.rootPackageName ?: ""
      )
    }

    fun default(project: Project, sourceSetNames: List<String>): List<ServiceVariant> {
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

            val name = (i++).toString()//entry.key.split(File.separator).map { it.capitalize() }.joinToString("")

            ServiceVariant(
                name = name,
                files = files,
                schemaFile = entry.value,
                schemaPackageName = entry.value.canonicalPath.formatPackageName(dropLast = 1)!!,
                rootPackageName = ""
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