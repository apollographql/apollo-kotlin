package com.apollographql.apollo.gradle

import com.apollographql.apollo.compiler.GraphQLCompiler
import com.google.common.base.Joiner
import org.gradle.api.Project
import org.gradle.api.Task

abstract class TaskConfigurator {
  protected final Project project

  TaskConfigurator(Project project) {
    this.project = project
  }

  abstract void configureTasks(Task apolloClassGenTask)

  protected final ApolloCodegenTask createCodegenTask(String sourceSetOrVariantName, Collection sourceSets) {
    File outputFolder = new File(project.buildDir, Joiner.on(File.separator).join(GraphQLCompiler.OUTPUT_DIRECTORY + sourceSetOrVariantName))
    String taskName = String.format(ApolloCodegenTask.NAME, sourceSetOrVariantName.capitalize())
    return project.tasks.create(taskName, ApolloCodegenTask) {
      source(sourceSets.collect { it.graphql })
      excludeFiles = project.apollo.sourceSet.exclude
      group = ApolloPlugin.TASK_GROUP
      description = "Generate Android classes for ${sourceSetOrVariantName.capitalize()} GraphQL queries"
      schemaFilePath = (project.apollo.sourceSet.schemaFile.get().length() == 0) ? project.apollo.schemaFilePath :
          project.apollo.sourceSet.schemaFile
      outputPackageName = project.apollo.outputPackageName
      variant = sourceSetOrVariantName
      sourceSetNames = sourceSets.collect { it.name }
      outputDir.set(outputFolder)
      customTypeMapping = project.apollo.customTypeMapping
      nullableValueType = project.apollo.nullableValueType
      useSemanticNaming = project.apollo.useSemanticNaming
      generateModelBuilder = project.apollo.generateModelBuilder
      useJavaBeansSemanticNaming = project.apollo.useJavaBeansSemanticNaming
      suppressRawTypesWarning = project.apollo.suppressRawTypesWarning
      generateKotlinModels = project.apollo.generateKotlinModels
      generateVisitorForPolymorphicDatatypes = project.apollo.generateVisitorForPolymorphicDatatypes
    }
  }
}
