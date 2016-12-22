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

  @Override void apply(Project project) {
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
    //TODO: add dependency on apollo-runtime once we have the jars on nexus

    project.tasks.create(ApolloCodeGenInstallTask.NAME, ApolloCodeGenInstallTask.class)

    Task apolloIRGenTask = project.task("generateApolloIR")
    apolloIRGenTask.group(TASK_GROUP)
    Task apolloClassGenTask = project.task("generateApolloClasses")
    apolloClassGenTask.group(TASK_GROUP)

    variants.all { v ->
      List<ApolloExtension> config = Lists.newArrayListWithCapacity(sourceSets.size())
      sourceSets.each { sourceSet ->
        config.add((ApolloExtension) sourceSet.extensions[ApolloExtension.NAME])
      }

      ApolloIRGenTask variantIRTask = createApolloIRGenTask(project, v.name, config)
      ApolloClassGenTask variantClassTask = createApolloClassGenTask(project, v.name, config)
      v.registerJavaGeneratingTask(variantClassTask, variantClassTask.outputDir)
      apolloIRGenTask.dependsOn(variantIRTask)
      apolloClassGenTask.dependsOn(variantClassTask)
    }
  }

  private static void setupNode(Project project) {
    project.plugins.apply NodePlugin
    NodeExtension nodeConfig = project.extensions.findByName("node") as NodeExtension
    nodeConfig.download = true
    nodeConfig.version = NODE_VERSION
  }

  private static ApolloIRGenTask createApolloIRGenTask(Project project, String name, List<ApolloExtension> config) {
    String taskName = String.format(ApolloIRGenTask.NAME, name.capitalize())
    ApolloIRGenTask task = project.tasks.create(taskName, ApolloIRGenTask)
    task.init(name, config)
    return task
  }

  private static ApolloClassGenTask createApolloClassGenTask(Project project, String name, List<ApolloExtension> conf) {
    String taskName = String.format(ApolloClassGenTask.NAME, name.capitalize())
    ApolloClassGenTask task = project.tasks.create(taskName, ApolloClassGenTask)
    task.source(project.tasks.findByName(String.format(ApolloIRGenTask.NAME, name.capitalize())).outputDir)
    task.include("**${File.separatorChar}*API.json")
    task.init(name, conf)
    return task
  }

  private static void createExtensionForSourceSet(Project project, def sourceSet) {
    sourceSet.extensions.create(ApolloExtension.NAME, ApolloExtension, project, sourceSet.name)
  }
}
