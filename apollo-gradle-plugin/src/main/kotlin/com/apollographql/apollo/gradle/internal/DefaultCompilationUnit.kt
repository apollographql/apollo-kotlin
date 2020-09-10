package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.CompilationUnit
import com.apollographql.apollo.gradle.api.CompilerParams
import com.apollographql.apollo.gradle.internal.ApolloPlugin.Companion.isKotlinMultiplatform
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Provider
import java.io.File
import javax.inject.Inject

abstract class DefaultCompilationUnit @Inject constructor(
    val project: Project,
    val apolloExtension: DefaultApolloExtension,
    val apolloVariant: ApolloVariant,
    val service: DefaultService
) : CompilationUnit, CompilerParams by project.objects.newInstance(DefaultCompilerParams::class.java) {

  final override val androidVariant = apolloVariant.androidVariant
  final override val variantName = apolloVariant.name
  final override val serviceName = service.name

  override val name = "${variantName}${serviceName.capitalize()}"

  abstract override val outputDir: DirectoryProperty
  abstract override val operationOutputFile: RegularFileProperty

  fun resolveParams(project: Project): Pair<CompilerParams, SourceDirectorySet> {
    val compilerParams = this
        .withFallback(project.objects, service)
        .withFallback(project.objects, apolloExtension)

    val sourceDirectorySet = if (apolloVariant.isTest) {
      // For tests, reusing sourceDirectorySet from the Service or Extension will
      // generate duplicate classes so we just skip them
      graphqlSourceDirectorySet
    } else {
      compilerParams.graphqlSourceDirectorySet
    }

    if (sourceDirectorySet.srcDirs.isEmpty()) {
      sourceDirectorySet.findSources(compilerParams.schemaFile)
    }

    if (!compilerParams.schemaFile.isPresent) {
      // This needs to be lazy for:
      // - test variants that don't have a schema
      // - chained dependencies where the schema might not be written yet when we come here
      val provider = project.layout.file(
          project.provider {
            if (sourceDirectorySet.isEmpty) {
              // No sources at all, do not bother looking for a schema. Happens for test variants
              return@provider null
            }
            resolveSchema(project = project,
                directories = sourceDirectorySet.srcDirs,
                schemaPathProvider = service.schemaPath,
                sourceSetNames = apolloVariant.sourceSetNames
            )?.let {
              project.file(it)
            }
          }
      )
      // Gradle lazy magic above. We need to be able to represent a "not set" schema if none is found
      compilerParams.schemaFile.set(provider)
    }


    return compilerParams to sourceDirectorySet
  }

  private fun SourceDirectorySet.findSources(schemaFile: RegularFileProperty) {
    val directories = when {
      apolloVariant.isTest -> {
        // Tests only search files under its folder else it adds duplicated models
        // Main variant's generated code is already available in test code
        resolveDirectories(project, project.provider { "." }, apolloVariant.sourceSetNames)
      }
      schemaFile.isPresent -> listOf(schemaFile.asFile.get().parent)
      else -> resolveDirectories(project, service.sourceFolder, apolloVariant.sourceSetNames)
    }

    include("**/*.graphql", "**/*.gql")
    exclude(service.exclude.getOrElse(emptyList()))
    directories.forEach {
      srcDir(it)
    }
  }

  fun generateKotlinModels(): Boolean = when {
    project.isKotlinMultiplatform -> true
    else -> generateKotlinModels.orElse(service.generateKotlinModels).orElse(apolloExtension.generateKotlinModels).getOrElse(false)
  }

  companion object {
    fun createDefaultCompilationUnit(
        project: Project,
        apolloExtension: DefaultApolloExtension,
        apolloVariant: ApolloVariant,
        service: DefaultService
    ): DefaultCompilationUnit {
      return project.objects.newInstance(DefaultCompilationUnit::class.java,
          project,
          apolloExtension,
          apolloVariant,
          service
      ).apply {
        graphqlSourceDirectorySet.include("**/*.graphql", "**/*.gql")
      }
    }

    private fun multipleSchemaError(): String {
      val services = """|
          |  service("service1") {
          |    sourceFolder.set("com/service1)"
          |    rootPackageName.set("com.service1)
          |  }
          |  
          |  service("service2") {
          |    sourceFolder.set("com/service2)"
          |    rootPackageName.set("com.service2)
          |  }
        """.trimMargin()
      return "ApolloGraphQL: By default only one schema.[json | sdl] file is supported.\n" +
          "Please use multiple services instead:\napollo {\n$services\n}" +
          "See https://www.apollographql.com/docs/android/essentials/plugin-configuration/#apolloextension-services-and-compilationunit for more details."
    }

    fun resolveDirectories(project: Project, sourceFolderProvider: Provider<String>, sourceSetNames: List<String>): List<String> {
      val sourceFolder = sourceFolderProvider.orElse(".").get()

      return when {
        sourceFolder.startsWith(File.separator) -> listOf(sourceFolder)
        sourceFolder.startsWith("..") -> listOf(project.file("src/main/graphql/$sourceFolder").normalize().path)
        else -> sourceSetNames.map { "src/$it/graphql/$sourceFolder" }
      }
    }

    fun resolveSchema(project: Project, schemaPathProvider: Provider<String>, directories: Set<File>, sourceSetNames: List<String>): String? {
      if (schemaPathProvider.isPresent) {
        val schemaPath = schemaPathProvider.get()
        if (schemaPath.startsWith(File.separator)) {
          return schemaPath
        } else if (schemaPath.startsWith("..")) {
          return project.file("src/main/graphql/$schemaPath").normalize().path
        } else {
          val all = sourceSetNames.map {
            project.file("src/$it/graphql/$schemaPath")
          }

          val candidates = all.filter {
            it.exists()
          }

          require(candidates.size <= 1) {
            "ApolloGraphQL: duplicate(s) schema file(s) found:\n${candidates.map { it.absolutePath }.joinToString("\n")}"
          }
          require(candidates.size == 1) {
            "ApolloGraphQL: cannot find a schema file at $schemaPath. Tried:\n${all.map { it.absolutePath }.joinToString("\n")}"
          }

          return candidates.first().path
        }
      } else {
        val candidates = directories.flatMap { srcDir ->
          srcDir.walkTopDown().filter { it.name == "schema.json" || it.name == "schema.sdl" }.toList()
        }

        require(candidates.size <= 1) {
          multipleSchemaError()
        }

        return candidates.firstOrNull()?.path
      }
    }
  }
}

