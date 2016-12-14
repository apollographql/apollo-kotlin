package com.apollostack.android.gradle

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.google.common.collect.Lists
import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.NodePlugin
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class ApolloPlugin implements Plugin<Project> {
  private static final String NODE_VERSION = "6.7.0"
  public static final String TASK_GROUP = "apollo"

  @Override
  void apply(Project project) {
    setupNode(project)
    project.plugins.all { p ->
      if (p instanceof AppPlugin) {
        configureAndroid(project,
            (DomainObjectCollection<BaseVariant>) project.android.applicationVariants)
      }
      if (p instanceof LibraryPlugin) {
        configureAndroid(project,
            (DomainObjectCollection<BaseVariant>) project.android.libraryVariants)
      }
    }
  }

  private static void configureAndroid(Project project, DomainObjectCollection<BaseVariant> variants) {
    project.android.sourceSets.all { s ->
      createExtensionForSourceSet(project, s)
    }

    Task apolloIRGenTask = project.task("generateApolloIR")
    apolloIRGenTask.group(TASK_GROUP)

    project.tasks.create(ApolloCodegenInstallTask.NAME, ApolloCodegenInstallTask.class)

    variants.all { v ->
      ApolloIRgenTask variantIRTask = createApolloIRGenTask(project, v.name, v.sourceSets)
      apolloIRGenTask.dependsOn(variantIRTask)
    }
  }

  private static void setupNode(Project project) {
    project.plugins.apply NodePlugin
    NodeExtension nodeConfig = project.extensions.findByName("node") as NodeExtension
    nodeConfig.download = true
    nodeConfig.version = NODE_VERSION
  }

  private static ApolloIRgenTask createApolloIRGenTask(Project project, String name, Collection<?> sourceSets) {
    List<ApolloExtension> config = Lists.newArrayListWithCapacity(sourceSets.size())
    sourceSets.each { sourceSet ->
      config.add((ApolloExtension) sourceSet.extensions[ApolloExtension.NAME])
    }
    String taskName = String.format(ApolloIRgenTask.NAME, name.capitalize())
    ApolloIRgenTask task = project.tasks.create(taskName, ApolloIRgenTask)
    task.variant = name
    task.group = TASK_GROUP
    task.config = config
    task.description = "Generate Android classes for working with ${name.capitalize()} GraphQL queries"
    task.dependsOn(ApolloCodegenInstallTask.NAME)

    return task
  }

  private static void createExtensionForSourceSet(Project project, def sourceSet) {
    sourceSet.extensions.create(ApolloExtension.NAME, ApolloExtension, project, sourceSet.name)
  }
}

