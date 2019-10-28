package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.*
import com.apollographql.apollo.gradle.api.CompilationUnit
import com.apollographql.apollo.gradle.api.CompilerParams
import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File

class DefaultCompilationUnit(
    override val serviceName: String,
    override val variantName: String,
    override val androidVariant: Any?,

    override var compilerParams: CompilerParams,

    private var sourcesLocator: SourcesLocator,
    private val sourceSetNames: List<String>,
    private val project: Project
) : CompilationUnit {
  sealed class SourcesLocator {
    class FromService(
        val schemaPath: Property<String>,
        val sourceFolder: Property<String>,
        val rootPackageName: Provider<String>,
        val exclude: ListProperty<String>
    ) : SourcesLocator()

    class FromFiles(
        val schema: File,
        val sourceFolder: String
    ) : SourcesLocator()
  }

  class Sources(
      val schemaFile: Provider<RegularFile>,
      val graphqlFiles: FileCollection,
      val rootFolders: FileCollection,
      val rootPackageName: Provider<String>
  )

  override val name = "${variantName}${serviceName.capitalize()}"

  private var sources: Sources? = null

  override val outputDir = project.objects.directoryProperty()
  override val transformedQueriesDir = project.objects.directoryProperty()

  init {
    if (!compilerParams.generateKotlinModels.isPresent) {
      compilerParams.generateKotlinModels.set(false)
    }
  }

  fun sources(): Sources {
    if (sources != null) {
      return sources!!
    }

    when (val locator = sourcesLocator) {
      is SourcesLocator.FromFiles -> {
        sourcesFromFiles(locator)
      }
      is SourcesLocator.FromService -> {
        sourcesFromService(locator)
      }
    }
    return sources!!
  }

  override fun setSources(rootFolder: Provider<Directory>) {
    val schemaFile = project.objects.fileProperty().value {
      val root = rootFolder.get().asFile
      val walk = root.walkTopDown()
      val file = walk.find {
        it.name.endsWith(".json")
      }
      if (file == null) {
        throw IllegalArgumentException("cannot find a schema in ${root.absolutePath}")
      }
      file
    }

    val graphqlFiles = project.objects.fileCollection()
    graphqlFiles.setFrom(rootFolder.map {
      it.asFileTree.matching {
        it.include("**.graphql", "**.gql")
      }
    })

    val rootFolders = project.objects.fileCollection()
    rootFolders.setFrom(rootFolder)

    val rootPackageName = project.provider { "" }

    sources = Sources(
        schemaFile = schemaFile,
        graphqlFiles = graphqlFiles,
        rootFolders = rootFolders,
        rootPackageName = rootPackageName
    )
  }

  override fun setSources(rootFolders: FileCollection, graphqlFiles: FileCollection, schemaFile: Provider<RegularFile>, rootPackageName: Provider<String>) {
    sources = Sources(
        schemaFile = schemaFile,
        graphqlFiles = graphqlFiles,
        rootFolders = rootFolders,
        rootPackageName = rootPackageName
    )
  }

  fun sourcesFromService(fromService: SourcesLocator.FromService) {
    val sourceFolder = fromService.sourceFolder.orElse(".")
    val rootFolders = project.objects.fileCollection().apply {
      setFrom({
        sourceSetNames.map {
          project.projectDir.child("src", it, "graphql", sourceFolder.get())
        }
      })
    }

    val schemaFile = project.objects.fileProperty().value {
      if (fromService.schemaPath.isPresent) {
        val map = findFilesInSourceSets(project, sourceSetNames, fromService.schemaPath.get(), { true })
        if (map.isEmpty()) {
          val tried = sourceSetNames.map { project.projectDir.child("src", it, "graphql", fromService.schemaPath.get()).absolutePath }
          throw IllegalArgumentException("cannot find a schema. Tried:\n${tried.joinToString("\n")}")
        }
        map.values.first()
      } else {
        val map = findFilesInSourceSets(project, sourceSetNames, sourceFolder.get(), { it.name.endsWith(".json") })
        if (map.isEmpty()) {
          val tried = sourceSetNames.map { project.projectDir.child("src", it, "graphql", sourceFolder.get()).absolutePath }
          throw IllegalArgumentException("cannot find a schema. Please specify service.schemaPath. Tried:\n${tried.joinToString("\n")}")
        }
        map.values.first()
      }
    }

    val graphqlFiles = project.objects.fileCollection().apply {
      setFrom({
        val candidates = findFilesInSourceSets(project, sourceSetNames, sourceFolder.get(), ::isGraphQL).values

        project.files(candidates).asFileTree.matching {
          it.exclude(fromService.exclude.get())
        }.files
      })
    }

    sources = Sources(
        schemaFile = schemaFile,
        graphqlFiles = graphqlFiles,
        rootFolders = rootFolders,
        rootPackageName = fromService.rootPackageName
    )
  }

  fun sourcesFromFiles(fromFiles: SourcesLocator.FromFiles) {
    val rootFolders = project.objects.fileCollection().apply {
      setFrom({
        sourceSetNames.map {
          project.projectDir.child("src", it, "graphql", fromFiles.sourceFolder)
        }
      })
    }
    val rootPackageName = project.provider { fromFiles.sourceFolder.toPackageName() }
    val schemaFile = project.objects.fileProperty().value { fromFiles.schema }
    val graphqlFiles = project.objects.fileCollection().apply {
      setFrom({
        findFilesInSourceSets(project, sourceSetNames, fromFiles.sourceFolder, ::isGraphQL).values
      })
    }

    sources = Sources(
        schemaFile = schemaFile,
        graphqlFiles = graphqlFiles,
        rootFolders = rootFolders,
        rootPackageName = rootPackageName
    )
  }

  companion object {
    fun fromService(project: Project, apolloExtension: DefaultApolloExtension, apolloVariant: ApolloVariant, service: DefaultService): DefaultCompilationUnit {
      val compilerParams = service.withFallback(apolloExtension, project.objects)

      val sourcesLocator = SourcesLocator.FromService(
          rootPackageName = service.rootPackageName,
          schemaPath = service.schemaPath,
          sourceFolder = service.sourceFolder,
          exclude = service.exclude
      )
      return DefaultCompilationUnit(
          project = project,
          variantName = apolloVariant.name,
          sourceSetNames = apolloVariant.sourceSetNames,
          androidVariant = apolloVariant.androidVariant,
          serviceName = service.name,
          sourcesLocator = sourcesLocator,
          compilerParams = compilerParams
      )
    }

    fun fromFiles(project: Project, apolloExtension: DefaultApolloExtension, apolloVariant: ApolloVariant): List<DefaultCompilationUnit> {
      val sourceSetNames = apolloVariant.sourceSetNames
      val schemaFiles = findFilesInSourceSets(project, sourceSetNames, ".") {
        it.name == "schema.json"
      }

      return schemaFiles.entries
          .sortedBy { it.value.canonicalPath } // make sure the order is predictable for tests and in general
          .mapIndexed { i, entry ->
            val name = "service$i"
            val sourceFolder = entry.key.substringBeforeLast("/")

            DefaultCompilationUnit(
                project = project,
                variantName = apolloVariant.name,
                sourceSetNames = apolloVariant.sourceSetNames,
                androidVariant = apolloVariant.androidVariant,
                serviceName = name,
                compilerParams = apolloExtension,
                sourcesLocator = SourcesLocator.FromFiles(entry.value, sourceFolder)
            )
          }
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

