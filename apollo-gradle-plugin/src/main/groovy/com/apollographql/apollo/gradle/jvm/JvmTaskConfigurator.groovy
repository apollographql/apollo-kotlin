package com.apollographql.apollo.gradle.jvm

import com.apollographql.apollo.gradle.ApolloClassGenerationTask
import com.apollographql.apollo.gradle.ApolloExperimentalCodegenTask
import com.apollographql.apollo.gradle.TaskConfigurator
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.compile.JavaCompile

class JvmTaskConfigurator  extends TaskConfigurator{
    JvmTaskConfigurator(Project project) {
        super(project)
    }

    @Override
    void configureTasks(Task apolloIRGenTask, Task apolloClassGenTask) {
        project.sourceSets.all { SourceSet sourceSet ->
            addSourceSetTasks(sourceSet, apolloIRGenTask, apolloClassGenTask)
        }
    }

    private void addSourceSetTasks(SourceSet sourceSet, Task apolloIRGenTask, Task apolloClassGenTask) {
        String taskName = "main".equals(sourceSet.name) ? "" : sourceSet.name

        DirectoryProperty outputDir;
        if (useExperimentalCodegen) {
            ApolloExperimentalCodegenTask codegenTask = createExperimentalCodegenTask(sourceSet.name, [sourceSet])
            apolloClassGenTask.dependsOn(codegenTask)
            outputDir = codegenTask.outputDir
        } else {
            AbstractTask sourceSetIRTask = createApolloIRGenTask(sourceSet.name, [sourceSet])
            ApolloClassGenerationTask sourceSetClassTask = createApolloClassGenTask(sourceSet.name)
            apolloIRGenTask.dependsOn(sourceSetIRTask)
            apolloClassGenTask.dependsOn(sourceSetClassTask)
            outputDir = sourceSetClassTask.outputDir
        }

        // we use afterEvaluate here as we need to know the value of generateKotlinModels from addSourceSetTasks
        // TODO we should avoid afterEvaluate usage
        project.afterEvaluate {
            if (project.apollo.generateKotlinModels.get() != true) {
                JavaCompile compileTask = (JavaCompile) project.tasks.findByName("compile${taskName.capitalize()}Java")
                compileTask.source += project.fileTree(outputDir)
                compileTask.dependsOn(apolloClassGenTask)
            }
        }

        sourceSet.java.srcDir(outputDir)

        def compileKotlinTask = project.tasks.findByName("compile${taskName.capitalize()}Kotlin") as SourceTask
        if (compileKotlinTask != null) {
            // kotlinc uses the generated java classes as input too so we need the generated classes
            compileKotlinTask.dependsOn(apolloClassGenTask)
            // this is somewhat redundant with sourceSet.java.srcDir above but I believe by the time we come here the java plugin
            // has been configured already so we need to manually tell kotlinc where to find the generated classes
            compileKotlinTask.source(outputDir)
        }
    }

}
