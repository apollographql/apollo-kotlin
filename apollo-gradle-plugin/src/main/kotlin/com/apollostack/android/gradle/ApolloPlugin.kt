package com.apollostack.android.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.apollostack.compiler.GraphQLCompiler
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class ApolloPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.plugins.all {
      when (it) {
        is AppPlugin -> configureAndroid(project,
            project.extensions.getByType(AppExtension::class.java).applicationVariants)
        is LibraryPlugin -> configureAndroid(project,
            project.extensions.getByType(LibraryExtension::class.java).libraryVariants)
      }
    }
  }

  private fun <T : BaseVariant> configureAndroid(project: Project, variants: DomainObjectSet<T>) {
    val generateApollo = project.task("generateApolloClasses")
    variants.all {
      val taskName = "generate${it.name.capitalize()}ApolloClasses"
      val task = project.tasks.create(taskName, ApolloTask::class.java)
      task.group = "apollo"
      task.buildDirectory = project.buildDir
      task.description = "Generate Android interfaces for working with ${it.name} GraphQL queries"
      task.source("src")
      task.include("**${File.separatorChar}*.${GraphQLCompiler.FILE_EXTENSION}")

      generateApollo.dependsOn(task)

      it.registerJavaGeneratingTask(task, task.outputDirectory)
    }
  }
}
