package com.apollographql.apollo.gradle.android

import com.android.build.gradle.api.BaseVariant
import com.apollographql.apollo.gradle.ApolloCodegenTask
import com.apollographql.apollo.gradle.TaskConfigurator
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.Task

class AndroidTaskConfigurator extends TaskConfigurator {

  AndroidTaskConfigurator(Project project) {
    super(project)
  }

  @Override
  void configureTasks(Task apolloClassGenTask) {
    getVariants().all { BaseVariant variant ->
      addVariantTasks(variant, apolloClassGenTask)
    }
    project.android.testVariants.each { BaseVariant tv ->
      addVariantTasks(tv, apolloClassGenTask)
    }
  }

  private void addVariantTasks(BaseVariant variant, Task apolloClassGenTask) {
    ApolloCodegenTask codegenTask = createCodegenTask(variant.name, variant.sourceSets)
    variant.registerJavaGeneratingTask(codegenTask, codegenTask.outputDir.asFile.get())
    apolloClassGenTask.dependsOn(codegenTask)
  }

  private DomainObjectCollection<BaseVariant> getVariants() {
    return project.android.hasProperty('libraryVariants') ? project.android.libraryVariants : project.android.applicationVariants
  }
}
