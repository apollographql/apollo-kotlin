package com.apollographql.android.gradle

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.apollographql.android.VersionKt
import com.google.common.collect.Lists
import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.NodePlugin
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile

class ApolloPlugin implements Plugin<Project> {
  private static final String NODE_VERSION = "6.7.0"
  public static final String TASK_GROUP = "apollo"
  private Project project

  @Override void apply(Project project) {
    this.project = project
    if (project.plugins.hasPlugin(AppPlugin) || project.plugins.hasPlugin(LibraryPlugin) || project.plugins.hasPlugin(
        JavaPlugin)) {
      applyApolloPlugin()
    } else {
      throw new IllegalArgumentException(
          "Apollo plugin couldn't be applied. The Android or Java plugin must be configured first")
    }
  }

  private void applyApolloPlugin() {
    setupNode()
    project.extensions.create(ApolloExtension.NAME, ApolloExtension)
    createSourceSetExtensions()

    def compileDepSet = project.configurations.getByName("compile").dependencies
    project.getGradle().addListener(new DependencyResolutionListener() {
      @Override
      void beforeResolve(ResolvableDependencies resolvableDependencies) {
        compileDepSet.add(project.dependencies.create("com.apollographql.android:api:${VersionKt.VERSION}"))
        project.getGradle().removeListener(this)
      }

      @Override
      void afterResolve(ResolvableDependencies resolvableDependencies) {}
    })

    project.afterEvaluate {
      project.tasks.create(ApolloCodeGenInstallTask.NAME, ApolloCodeGenInstallTask.class)
      addApolloTasks()
    }
  }

  private void addApolloTasks() {
    Task apolloIRGenTask = project.task("generateApolloIR")
    apolloIRGenTask.group(TASK_GROUP)
    Task apolloClassGenTask = project.task("generateApolloClasses")
    apolloClassGenTask.group(TASK_GROUP)

    if (isAndroidProject()) {
      getVariants().all { v ->
        addVariantTasks(v, apolloIRGenTask, apolloClassGenTask, v.sourceSets)
      }
    } else {
      getSourceSets().all { sourceSet ->
        addSourceSetTasks(sourceSet, apolloIRGenTask, apolloClassGenTask)
      }
    }
  }

  private void addVariantTasks(Object variant, Task apolloIRGenTask, Task apolloClassGenTask, Collection<?> sourceSets) {
    List<GraphQLExtension> config = Lists.newArrayListWithCapacity(sourceSets.size())
    sourceSets.each { sourceSet ->
      config.add((GraphQLExtension) sourceSet.extensions[GraphQLExtension.NAME])
    }

    ApolloIRGenTask variantIRTask = createApolloIRGenTask(variant.name, config)
    ApolloClassGenTask variantClassTask = createApolloClassGenTask(variant.name, config,
        project.apollo.generateClasses, project.apollo.customTypeMapping)
    variant.registerJavaGeneratingTask(variantClassTask, variantClassTask.outputDir)
    apolloIRGenTask.dependsOn(variantIRTask)
    apolloClassGenTask.dependsOn(variantClassTask)
  }

  private void addSourceSetTasks(SourceSet sourceSet, Task apolloIRGenTask, Task apolloClassGenTask) {
    String taskName = "main".equals(sourceSet.name) ? "" : sourceSet.name
    def config = [(GraphQLExtension) sourceSet.extensions[GraphQLExtension.NAME]]

    ApolloIRGenTask sourceSetIRTask = createApolloIRGenTask(sourceSet.name, config)
    ApolloClassGenTask sourceSetClassTask = createApolloClassGenTask(sourceSet.name, config,
        project.apollo.generateClasses, project.apollo.customTypeMapping)
    apolloIRGenTask.dependsOn(sourceSetIRTask)
    apolloClassGenTask.dependsOn(sourceSetClassTask)

    JavaCompile compileTask = (JavaCompile) project.tasks.getByName("compile${taskName.capitalize()}Java")
    compileTask.source += project.fileTree(sourceSetClassTask.outputDir)
    compileTask.dependsOn(apolloClassGenTask)
  }

  private void setupNode() {
    project.plugins.apply NodePlugin
    NodeExtension nodeConfig = project.extensions.findByName("node") as NodeExtension
    nodeConfig.download = true
    nodeConfig.version = NODE_VERSION
  }

  private ApolloIRGenTask createApolloIRGenTask(String name, List<GraphQLExtension> config) {
    String taskName = String.format(ApolloIRGenTask.NAME, name.capitalize())
    ApolloIRGenTask task = project.tasks.create(taskName, ApolloIRGenTask)
    task.init(name, config)
    return task
  }

  private ApolloClassGenTask createApolloClassGenTask(String name, List<GraphQLExtension> conf, boolean generateClasses,
                                                      Map<String, String> customTypeMapping) {
    String taskName = String.format(ApolloClassGenTask.NAME, name.capitalize())
    ApolloClassGenTask task = project.tasks.create(taskName, ApolloClassGenTask)
    task.source(project.tasks.findByName(String.format(ApolloIRGenTask.NAME, name.capitalize())).outputDir)
    task.include("**${File.separatorChar}*API.json")
    task.init(name, conf, generateClasses, customTypeMapping)
    return task
  }

  private void createSourceSetExtensions() {
    getSourceSets().all { sourceSet ->
      sourceSet.extensions.create(GraphQLExtension.NAME, GraphQLExtension, project, sourceSet.name)
    }
  }

  private boolean isAndroidProject() {
    return project.hasProperty('android') && project.android.sourceSets
  }

  private Object getSourceSets() {
    return (isAndroidProject() ? project.android.sourceSets : project.sourceSets)
  }

  private DomainObjectCollection<BaseVariant> getVariants() {
    return project.android.applicationVariants ?: project.android.libraryVariants
  }

}
