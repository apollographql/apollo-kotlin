package com.apollographql.apollo.gradle.internal

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object AndroidTaskConfigurator {
  private fun apolloVariant(baseVariant: BaseVariant, isTest: Boolean): ApolloVariant {
    return ApolloVariant(
        name = baseVariant.name,
        sourceSetNames = baseVariant.sourceSets.map { it.name }.distinct(),
        androidVariant = baseVariant,
        isTest = isTest
    )
  }

  fun getVariants(project: Project, androidExtension: Any): NamedDomainObjectContainer<ApolloVariant> {
    val container = project.container(ApolloVariant::class.java)

    when (androidExtension) {
      is LibraryExtension -> {
        androidExtension.libraryVariants.all { variant ->
          container.add(apolloVariant(variant, false))
        }
        androidExtension.testVariants.all { variant ->
          container.add(apolloVariant(variant, true))
        }
        androidExtension.unitTestVariants.all { variant ->
          container.add(apolloVariant(variant, true))
        }
      }
      is AppExtension -> {
        androidExtension.applicationVariants.all { variant ->
          container.add(apolloVariant(variant, false))
        }
        androidExtension.testVariants.all { variant ->
          container.add(apolloVariant(variant, true))
        }
        androidExtension.unitTestVariants.all { variant ->
          container.add(apolloVariant(variant, true))
        }
      }
      else -> {
        // InstantAppExtension or something else we don't support yet
        throw IllegalArgumentException("${androidExtension.javaClass.name} is not supported at the moment")
      }
    }
    return container
  }

  // TODO: make this lazy (https://github.com/apollographql/apollo-android/issues/1454)
  fun registerGeneratedDirectory(
      project: Project,
      compilationUnit: DefaultCompilationUnit,
      codegenProvider: TaskProvider<ApolloGenerateSourcesTask>
  ) {
    val variant = compilationUnit.androidVariant as BaseVariant
    if (compilationUnit.generateKotlinModels()) {
      // This is apparently needed for intelliJ to find the generated files
      variant.addJavaSourceFoldersToModel(codegenProvider.get().outputDir.get().asFile)
      // Tell the kotlin compiler to compile our files
      project.tasks.named("compile${variant.name.capitalize()}Kotlin").configure {
        it.dependsOn(codegenProvider)
        (it as KotlinCompile).source(codegenProvider.get().outputDir.asFile.get())
      }
    } else {
      variant.registerJavaGeneratingTask(codegenProvider.get(), codegenProvider.get().outputDir.get().asFile)

      /**
       * By the time we come here, the KotlinCompile task has been configured by the kotlin plugin already.
       *
       * Right now this is done in [org.jetbrains.kotlin.gradle.plugin.AbstractAndroidProjectHandler.configureSources].
       *
       * To workaround this, we're adding the java generated models folder here
       */
      project.tasks.matching {
        it.name == "compile${variant.name.capitalize()}Kotlin"
      }.configureEach{
        it.dependsOn(codegenProvider)
        (it as KotlinCompile).source(codegenProvider.get().outputDir.get().asFile)
      }
    }
  }
}
