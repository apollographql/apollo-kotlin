package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.internal.ApolloPluginHelper.Companion.isKotlinMultiplatform
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class ApolloPluginKotlin : Plugin<Project> {
  override fun apply(target: Project) {
    val pluginHelper = ApolloPluginHelper(target) { true }
    pluginHelper.registerTasks(target)

    target.tasks.withType(KotlinCompile::class.java).configureEach {
      it.kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    target.afterEvaluate {
      if (pluginHelper.apolloExtension.addRuntimeDependency.orElse(true).get()) {
        // Add the runtime dependency.
        if (target.isKotlinMultiplatform) {
          val sourceSets =
              target.extensions.getByType(KotlinMultiplatformExtension::class.java).sourceSets
          val sourceSet = (sourceSets.getByName("commonMain") as DefaultKotlinSourceSet)
          target.configurations.named(sourceSet.apiConfigurationName).configure {
            it.dependencies.add(
                target.dependencies.create("com.apollographql.apollo:apollo-runtime-kotlin:${com.apollographql.apollo.compiler.VERSION}")
            )
          }
        } else {
          target.configurations.named("api").configure {
            it.dependencies.add(
                target.dependencies.create("com.apollographql.apollo:apollo-runtime-kotlin:${com.apollographql.apollo.compiler.VERSION}")
            )
          }
        }
      }
    }
  }
}