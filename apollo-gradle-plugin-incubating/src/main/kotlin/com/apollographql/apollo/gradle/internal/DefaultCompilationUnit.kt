package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.child
import com.apollographql.apollo.gradle.api.CompilationUnit
import com.apollographql.apollo.gradle.api.CompilerParams
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File
import javax.inject.Inject

open class DefaultCompilationUnit @Inject constructor(
    override val serviceName: String,
    override val variantName: String,
    override val compilerParams: CompilerParams,
    private val sourcesLocator: SourcesLocator,
    private val sourceSetNames: List<String>,
    private val project: Project
) : CompilationUnit {
  sealed class SourcesLocator {
    class FromService(
        val schemaPath: Property<String>,
        val sourceFolder: Property<String>,
        val exclude: ListProperty<String>
    ) : SourcesLocator()

    class FromFiles(
        val schema: File
    ) : SourcesLocator()
  }

  internal class Sources(
      val schemaFile: Provider<RegularFile>,
      val graphqlFiles: FileCollection,
      val rootFolders: FileCollection
  )

  override var androidVariant: Any? = null
  override val name = "${variantName}${serviceName.capitalize()}"

  private var sources: Sources? = null

  override val outputDir = project.objects.directoryProperty()
  override val transformedQueriesDir = project.objects.directoryProperty()

  init {
    if (!compilerParams.generateKotlinModels.isPresent) {
      compilerParams.generateKotlinModels.set(false)
    }
  }

  internal fun sources(): Sources {
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

  override fun compilerParams(action: Action<CompilerParams>) {
    action.execute(compilerParams)
  }

  override fun sources(action: Action<CompilationUnit.Sources>) {
    val params = CompilationUnit.Sources(
        project.objects.fileProperty(),
        project.objects.directoryProperty()
    )
    action.execute(params)
    customSources(params)
  }

  private fun customSources(params: CompilationUnit.Sources) {
    require(params.graphqlDir.isPresent) { "rootFolder must be specified" }

    if (params.schemaFile.isPresent.not()) {
      params.schemaFile.value {
        val root = params.graphqlDir.get().asFile
        root.walkTopDown().find {
          it.name.endsWith(".json")
        } ?: throw IllegalArgumentException("cannot find a schema in ${root.absolutePath}")
      }
    }

    val graphqlFiles = project.objects.fileCollection()
    graphqlFiles.setFrom(params.graphqlDir.map { dir ->
      dir.asFileTree.filter(::isGraphQL)
    })

    val rootFolders = project.objects.fileCollection()
    rootFolders.setFrom(params.graphqlDir)

    sources = Sources(params.schemaFile, graphqlFiles, rootFolders)
  }

  private fun sourcesFromService(fromService: SourcesLocator.FromService) {
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
        rootFolders = rootFolders
    )
  }

  private fun sourcesFromFiles(fromFiles: SourcesLocator.FromFiles) {
    val rootFolders = project.objects.fileCollection().apply {
      setFrom({
        sourceSetNames.map {
          project.projectDir.child("src", it, "graphql")
        }
      })
    }
    val schemaFile = project.objects.fileProperty().value { fromFiles.schema }
    val graphqlFiles = project.objects.fileCollection().apply {
      setFrom({
        findFilesInSourceSets(project, sourceSetNames, ".", ::isGraphQL).values
      })
    }

    sources = Sources(
        schemaFile = schemaFile,
        graphqlFiles = graphqlFiles,
        rootFolders = rootFolders
    )
  }

  companion object {
    fun fromService(project: Project, apolloExtension: DefaultApolloExtension, apolloVariant: ApolloVariant, service: DefaultService): DefaultCompilationUnit {
      val compilerParams = service.withFallback(apolloExtension, project.objects)

      val sourcesLocator = SourcesLocator.FromService(
          schemaPath = service.schemaPath,
          sourceFolder = service.sourceFolder,
          exclude = service.exclude
      )

      return project.objects.newInstance(DefaultCompilationUnit::class.java,
          service.name,
          apolloVariant.name,
          compilerParams,
          sourcesLocator,
          apolloVariant.sourceSetNames,
          project
      ).apply {
        androidVariant = apolloVariant.androidVariant
      }
    }

    fun fromFiles(project: Project, apolloExtension: DefaultApolloExtension, apolloVariant: ApolloVariant): DefaultCompilationUnit? {
      val sourceSetNames = apolloVariant.sourceSetNames
      val schemaFiles = findFilesInSourceSets(project, sourceSetNames, ".") {
        it.name == "schema.json"
      }
      require(schemaFiles.size <= 1) { multipleSchemaError(schemaFiles.keys) }

      val schema = schemaFiles.values.firstOrNull() ?: return null
      return project.objects.newInstance(DefaultCompilationUnit::class.java,
          "service0",
          apolloVariant.name,
          apolloExtension,
          SourcesLocator.FromFiles(schema),
          apolloVariant.sourceSetNames,
          project
      ).apply {
        androidVariant = apolloVariant.androidVariant
      }
    }

    fun isGraphQL(file: File): Boolean {
      return file.name.endsWith(".graphql") || file.name.endsWith(".gql")
    }

    /**
     * Finds the files in the given sourceSets.
     *
     * Returns a map with the relative path to the path as key and the file as value
     *
     * @throws [kotlin.IllegalArgumentException] if there are multiple files with the same relative path
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

          if (candidates[key] != null) {
            throw IllegalArgumentException("ApolloGraphQL: duplicate file found:\n${it.absolutePath}\n${candidates[key]?.absolutePath}")
          }
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

    private fun multipleSchemaError(schemaPaths: Set<String>): String {
      val service = schemaPaths.joinToString("\n") {
        """|
          |  service("serviceName") {
          |    sourceFolder = "${it.replace("/schema.json", "")}"
          |  }
        """.trimMargin()
      }
      return "By default only one schema.json file is supported.\nPlease use multiple services instead:\napollo {\n$service\n}"
    }
  }
}

