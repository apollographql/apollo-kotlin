package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.CompilationUnit
import com.apollographql.apollo.gradle.api.CompilerParams
import com.apollographql.apollo.gradle.internal.ApolloPlugin.Companion.isKotlinMultiplatform
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.SourceDirectorySet
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

  private fun resolveSchema(graphqlSourceDirectorySet: SourceDirectorySet): File {
    if (service.schemaPath.isPresent) {
      val schemaPath = service.schemaPath.get()
      if (schemaPath.startsWith(File.separator)) {
        return project.file(schemaPath)
      } else if (schemaPath.startsWith("..")) {
        return project.file("src/main/graphql/$schemaPath").normalize()
      } else {
        val all = apolloVariant.sourceSetNames.map {
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

        return candidates.first()
      }
    } else {
      val candidates = graphqlSourceDirectorySet.srcDirs.flatMap { srcDir ->
        srcDir.walkTopDown().filter { it.name == "schema.json" }.toList()
      }

      require(candidates.size <= 1) {
        multipleSchemaError(candidates)
      }
      require(candidates.size == 1) {
        "ApolloGraphQL: cannot find schema.json. Please specify it explicitely. Looked under:\n" +
            graphqlSourceDirectorySet.srcDirs.map { it.absolutePath }.joinToString("\n")
      }
      return candidates.first()
    }
  }

  fun setSourcesIfNeeded(sourceDirectorySet: SourceDirectorySet, schemaFile: RegularFileProperty) {
    if (sourceDirectorySet.srcDirs.isEmpty()) {
      sourceDirectorySet.findSources(schemaFile)
    }

    if (!schemaFile.isPresent) {
      schemaFile.set { resolveSchema(sourceDirectorySet) }
    }
  }

  private fun SourceDirectorySet.findSources(schemaFile: RegularFileProperty) {
    when {
      apolloVariant.isTest -> {
        // Tests only search files under its folder else it adds duplicated models
        // Main variant's generated code is already available in test code
        srcDirFromVariant(apolloVariant, ".")
      }
      schemaFile.isPresent -> srcDir(schemaFile.asFile.get().parent)
      else -> {
        val sourceFolder = service.sourceFolder.orElse(".").get()
        when {
          sourceFolder.startsWith(File.separator) -> srcDir(sourceFolder)
          sourceFolder.startsWith("..") -> srcDir(project.file("src/main/graphql/$sourceFolder").normalize())
          else -> srcDirFromVariant(apolloVariant, sourceFolder)
        }
      }
    }

    include("**/*.graphql", "**/*.gql")
    exclude(service.exclude.getOrElse(emptyList()))
  }

  private fun SourceDirectorySet.srcDirFromVariant(apolloVariant: ApolloVariant, sourceFolder: String) {
    apolloVariant.sourceSetNames.forEach {
      srcDir("src/$it/graphql/$sourceFolder")
    }
  }

  fun generateKotlinModels(): Boolean = when {
    project.isKotlinMultiplatform -> true
    else -> generateKotlinModels.orElse(service.generateKotlinModels).orElse(apolloExtension.generateKotlinModels).getOrElse(false)
  }

  companion object {
    private fun createDefaultCompilationUnit(
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

    fun fromService(project: Project, apolloExtension: DefaultApolloExtension, apolloVariant: ApolloVariant, service: DefaultService): DefaultCompilationUnit {
      return createDefaultCompilationUnit(project, apolloExtension, apolloVariant, service)
    }

    fun fromFiles(project: Project, apolloExtension: DefaultApolloExtension, apolloVariant: ApolloVariant): DefaultCompilationUnit {
      val service = project.objects.newInstance(DefaultService::class.java, project.objects, "service")
      return createDefaultCompilationUnit(project, apolloExtension, apolloVariant, service)
    }

    private fun multipleSchemaError(schemaList: List<File>): String {
      val services = schemaList.joinToString("\n") {
        """|
          |  service("${it.parentFile.name}") {
          |    sourceFolder = "${it.parentFile.normalize().absolutePath}"
          |  }
        """.trimMargin()
      }
      return "ApolloGraphQL: By default only one schema.json file is supported.\nPlease use multiple services instead:\napollo {\n$services\n}"
    }
  }
}

