package com.apollographql.apollo3.gradle.internal

import com.android.build.gradle.api.BaseVariant
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.gradle.api.Service
import com.apollographql.apollo3.gradle.api.applicationVariants
import com.apollographql.apollo3.gradle.api.kotlinProjectExtensionOrThrow
import com.apollographql.apollo3.gradle.api.libraryVariants
import com.apollographql.apollo3.gradle.api.testVariants
import com.apollographql.apollo3.gradle.api.unitTestVariants
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal class DefaultOutputDirConnection(
    private val project: Project,
    override val task: TaskProvider<out Task>,
    override val outputDir: Provider<Directory>
): Service.OutputDirConnection {
  override fun connectToKotlinSourceSet(name: String) {
    project.kotlinProjectExtensionOrThrow.sourceSets.getByName(name).kotlin.srcDir(outputDir)
  }

  override fun connectToJavaSourceSet(name: String) {
    project.convention.getByType(JavaPluginConvention::class.java)
        .sourceSets
        .getByName(name)
        .allJava
        .srcDir(outputDir)
  }

  override fun connectToAndroidVariant(variant: BaseVariant) {
    val tasks = project.tasks

    // This doesn't seem to do much besides addJavaSourceFoldersToModel
    // variant.registerJavaGeneratingTask(codegenProvider.get(), codegenProvider.get().outputDir.get().asFile)

    // This is apparently needed for intelliJ to find the generated files
    // TODO: make this lazy (https://github.com/apollographql/apollo-android/issues/1454)
    variant.addJavaSourceFoldersToModel(outputDir.get().asFile)
    // Tell the kotlin compiler to compile our files
    tasks.named("compile${variant.name.capitalizeFirstLetter()}Kotlin").configure {
      it.dependsOn(task)
      (it as KotlinCompile).source(outputDir.get())
    }
  }

  override fun connectToAllAndroidApplicationVariants() {
    project.applicationVariants?.all { variant ->
      connectToAndroidVariant(variant)
    }
  }

  override fun connectToAllAndroidLibraryVariants() {
    project.libraryVariants?.all { variant ->
      connectToAndroidVariant(variant)
    }
  }

  override fun connectToAllAndroidTestVariants() {
    project.testVariants?.all { variant ->
      connectToAndroidVariant(variant)
    }
  }

  override fun connectToAllAndroidUnitTestVariants() {
    project.unitTestVariants?.all { variant ->
      connectToAndroidVariant(variant)
    }
  }
}