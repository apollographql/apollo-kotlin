package com.apollographql.apollo.gradle

import com.apollographql.apollo.compiler.GraphQLCompiler
import com.google.common.base.Joiner
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.AbstractTask

abstract class TaskConfigurator {
    public static final String APOLLO_CODEGEN_GENERATE_TASK_NAME = "generate%sApolloIR"

    final boolean useGlobalApolloCodegen = System.properties['apollographql.useGlobalApolloCodegen']?.toBoolean()
    final boolean useExperimentalCodegen = (System.properties['apollographql.useExperimentalCodegen'] ?: "true").toBoolean()
    protected final Project project

    TaskConfigurator(Project project) {
        this.project = project
    }

    abstract void configureTasks(Task apolloIRGenTask, Task apolloClassGenTask)

    protected final ApolloExperimentalCodegenTask createExperimentalCodegenTask(String sourceSetOrVariantName, Collection sourceSets) {
        File outputFolder = new File(project.buildDir, Joiner.on(File.separator).join(GraphQLCompiler.OUTPUT_DIRECTORY + sourceSetOrVariantName))
        String taskName = String.format(ApolloExperimentalCodegenTask.NAME, sourceSetOrVariantName.capitalize())
        return project.tasks.create(taskName, ApolloExperimentalCodegenTask) {
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

    protected final AbstractTask createApolloIRGenTask(String sourceSetOrVariantName, Collection sourceSets) {
        def sourceSetNamesList = sourceSets.collect { it.name }
        File outputFolder = new File(project.buildDir, Joiner.on(File.separator)
                .join(GraphQLCompiler.IR_OUTPUT_DIRECTORY + sourceSetOrVariantName))

        String taskName = String.format(APOLLO_CODEGEN_GENERATE_TASK_NAME, sourceSetOrVariantName.capitalize())
        if (useGlobalApolloCodegen) {
            return project.tasks.create(taskName, ApolloSystemCodegenGenerationTask) {
                sourceSets.each { sourceSet ->
                    sourceSet.graphql.exclude(project.apollo.sourceSet.exclude.get())
                    inputs.files(sourceSet.graphql).skipWhenEmpty()
                }
                group = ApolloPlugin.TASK_GROUP
                description = "Generate an IR file using apollo-codegen for ${sourceSetOrVariantName.capitalize()} GraphQL queries"
                schemaFilePath = (project.apollo.sourceSet.schemaFile.get().length() == 0) ? project.apollo.schemaFilePath :
                        project.apollo.sourceSet.schemaFile
                outputPackageName = project.apollo.outputPackageName
                variant = sourceSetOrVariantName
                sourceSetNames = sourceSetNamesList
                outputDir.set(outputFolder)
            }
        } else {
            return project.tasks.create(taskName, ApolloLocalCodegenGenerationTask) {
                sourceSets.each { sourceSet ->
                    sourceSet.graphql.exclude(project.apollo.sourceSet.exclude.get())
                    inputs.files(sourceSet.graphql).skipWhenEmpty()
                }
                group = ApolloPlugin.TASK_GROUP
                description = "Generate an IR file using apollo-codegen for ${sourceSetOrVariantName.capitalize()} GraphQL queries"
                dependsOn(ApolloCodegenInstallTask.NAME)
                schemaFilePath = (project.apollo.sourceSet.schemaFile.get().length() == 0) ? project.apollo.schemaFilePath :
                        project.apollo.sourceSet.schemaFile
                outputPackageName = project.apollo.outputPackageName
                variant = sourceSetOrVariantName
                sourceSetNames = sourceSetNamesList
                outputDir.set(outputFolder)
            }
        }
    }

    protected final ApolloClassGenerationTask createApolloClassGenTask(String name) {
        String taskName = String.format(ApolloClassGenerationTask.NAME, name.capitalize())
        return project.tasks.create(taskName, ApolloClassGenerationTask) {
            group = ApolloPlugin.TASK_GROUP
            description = "Generate Android classes for ${name.capitalize()} GraphQL queries"
            dependsOn(getProject().getTasks().findByName(String.format(APOLLO_CODEGEN_GENERATE_TASK_NAME, name.capitalize())))
            source = project.tasks.findByName(String.format(APOLLO_CODEGEN_GENERATE_TASK_NAME, name.capitalize())).outputDir
            include "**${File.separatorChar}*API.json"
            customTypeMapping = project.apollo.customTypeMapping
            nullableValueType = project.apollo.nullableValueType
            useSemanticNaming = project.apollo.useSemanticNaming
            generateModelBuilder = project.apollo.generateModelBuilder
            useJavaBeansSemanticNaming = project.apollo.useJavaBeansSemanticNaming
            outputPackageName = project.apollo.outputPackageName
            suppressRawTypesWarning = project.apollo.suppressRawTypesWarning
            generateKotlinModels = project.apollo.generateKotlinModels
            generateVisitorForPolymorphicDatatypes = project.apollo.generateVisitorForPolymorphicDatatypes
        }
    }

}
