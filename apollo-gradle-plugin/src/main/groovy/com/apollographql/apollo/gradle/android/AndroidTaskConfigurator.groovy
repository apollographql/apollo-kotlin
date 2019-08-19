package com.apollographql.apollo.gradle.android

import com.android.build.gradle.api.BaseVariant
import com.apollographql.apollo.gradle.ApolloClassGenerationTask
import com.apollographql.apollo.gradle.ApolloExperimentalCodegenTask
import com.apollographql.apollo.gradle.TaskConfigurator
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.AbstractTask

class AndroidTaskConfigurator extends TaskConfigurator {

    AndroidTaskConfigurator(Project project) {
        super(project)
    }

    @Override
    void configureTasks(Task apolloIRGenTask, Task apolloClassGenTask) {
        getVariants().all { BaseVariant variant ->
            addVariantTasks(variant, apolloIRGenTask, apolloClassGenTask, variant.sourceSets)
        }
        project.android.testVariants.each { BaseVariant tv ->
            addVariantTasks(tv, apolloIRGenTask, apolloClassGenTask, tv.sourceSets)
        }
    }

    private void addVariantTasks(BaseVariant variant, Task apolloIRGenTask, Task apolloClassGenTask, Collection sourceSets) {
        if (useExperimentalCodegen) {
            ApolloExperimentalCodegenTask codegenTask = createExperimentalCodegenTask(variant.name, variant.sourceSets)
            variant.registerJavaGeneratingTask(codegenTask, codegenTask.outputDir.asFile.get())
            apolloClassGenTask.dependsOn(codegenTask)
        } else {
            AbstractTask variantIRTask = createApolloIRGenTask(variant.name, sourceSets)
            ApolloClassGenerationTask variantClassTask = createApolloClassGenTask(variant.name)
            variant.registerJavaGeneratingTask(variantClassTask, variantClassTask.outputDir.asFile.get())
            apolloIRGenTask.dependsOn(variantIRTask)
            apolloClassGenTask.dependsOn(variantClassTask)
        }
    }

    private DomainObjectCollection<BaseVariant> getVariants() {
        return project.android.hasProperty('libraryVariants') ? project.android.libraryVariants : project.android.applicationVariants
    }
}
