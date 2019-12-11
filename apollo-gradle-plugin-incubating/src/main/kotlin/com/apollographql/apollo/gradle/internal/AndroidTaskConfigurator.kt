package com.apollographql.apollo.gradle.internal

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object AndroidTaskConfigurator {
  private fun apolloVariant(baseVariant: BaseVariant): ApolloVariant {
    return AndroidApolloVariant(
        name = baseVariant.name,
        sourceSetNames = baseVariant.sourceSets.map { it.name }.distinct(),
        androidVariant = baseVariant
    )
  }

  fun getVariants(project: Project, androidExtension: Any): NamedDomainObjectContainer<ApolloVariant> {
    val container = project.container(ApolloVariant::class.java)

    when (androidExtension) {
      is LibraryExtension -> {
        androidExtension.libraryVariants.all { variant ->
          container.add(apolloVariant(variant))
        }
        androidExtension.testVariants.all { variant ->
          container.add(apolloVariant(variant))
        }
        androidExtension.unitTestVariants.all { variant ->
          container.add(apolloVariant(variant))
        }
      }
      is AppExtension -> {
        androidExtension.applicationVariants.all { variant ->
          container.add(apolloVariant(variant))
        }
        androidExtension.testVariants.all { variant ->
          container.add(apolloVariant(variant))
        }
        androidExtension.unitTestVariants.all { variant ->
          container.add(apolloVariant(variant))
        }
      }
      else -> {
        // InstantAppExtension or something else we don't support yet
        throw IllegalArgumentException("${androidExtension.javaClass.name} is not supported at the moment")
      }
    }
    return container
  }
}

class AndroidApolloVariant(name: String, sourceSetNames: List<String>, androidVariant: Any) : ApolloVariant(name, sourceSetNames, androidVariant) {
  // TODO: make this lazy (https://github.com/apollographql/apollo-android/issues/1454)
  override fun registerGeneratedDirectory(
      project: Project,
      forKotlin: Boolean,
      codegenProvider: TaskProvider<ApolloGenerateSourcesTask>
  ) {
    val variant = this.androidVariant as BaseVariant
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
    }.configureEach {
      it.dependsOn(codegenProvider)
      (it as KotlinCompile).source(codegenProvider.get().outputDir.get().asFile)
    }
  }
}
