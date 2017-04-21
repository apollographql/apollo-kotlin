package com.apollographql.android.gradle

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.apollographql.android.VersionKt
import com.google.common.collect.ImmutableList
import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.NodePlugin
import org.gradle.api.DomainObjectCollection
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile

import javax.inject.Inject

class ApolloPlugin implements Plugin<Project> {
  private static final String NODE_VERSION = "6.7.0"
  public static final String TASK_GROUP = "apollo"
  private static final String APOLLO_DEP_GROUP = "com.apollographql.apollo"
  private static final String RUNTIME_DEP_NAME = "apollo-runtime"

  private Project project
  private final FileResolver fileResolver

  @Inject
  public ApolloPlugin(FileResolver fileResolver) {
    this.fileResolver = fileResolver
  }

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
        def apolloRuntimeDep = compileDepSet.find { dep ->
          dep.group == APOLLO_DEP_GROUP
          dep.name == RUNTIME_DEP_NAME
        }
        if (apolloRuntimeDep != null && apolloRuntimeDep.version != VersionKt.VERSION && !apolloRuntimeDep instanceof ProjectDependency) {
          throw new GradleException(
              "apollo-runtime version ${apolloRuntimeDep.version} isn't compatible with the apollo-gradle-plugin version ${VersionKt.VERSION}")
        }
        if (System.getProperty("apollographql.skipRuntimeDep") != "true" && apolloRuntimeDep == null) {
          compileDepSet.add(project.dependencies.create("$APOLLO_DEP_GROUP:$RUNTIME_DEP_NAME:$VersionKt.VERSION"))
        }
      }

      @Override
      void afterResolve(ResolvableDependencies resolvableDependencies) {
        project.getGradle().removeListener(this)
      }
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
    ApolloIRGenTask variantIRTask = createApolloIRGenTask(variant.name, sourceSets)
    ApolloClassGenTask variantClassTask = createApolloClassGenTask(variant.name, project.apollo.customTypeMapping,
        project.apollo.nullableValueType, project.apollo.generateAccessors)
    variant.registerJavaGeneratingTask(variantClassTask, variantClassTask.outputDir)
    apolloIRGenTask.dependsOn(variantIRTask)
    apolloClassGenTask.dependsOn(variantClassTask)
  }

  private void addSourceSetTasks(SourceSet sourceSet, Task apolloIRGenTask, Task apolloClassGenTask) {
    String taskName = "main".equals(sourceSet.name) ? "" : sourceSet.name

    ApolloIRGenTask sourceSetIRTask = createApolloIRGenTask(sourceSet.name, [sourceSet])
    ApolloClassGenTask sourceSetClassTask = createApolloClassGenTask(sourceSet.name, project.apollo.customTypeMapping,
        project.apollo.nullableValueType, project.apollo.generateAccessors)
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

  private ApolloIRGenTask createApolloIRGenTask(String sourceSetOrVariantName, Collection<Object> sourceSets) {
    String taskName = String.format(ApolloIRGenTask.NAME, sourceSetOrVariantName.capitalize())
    ApolloIRGenTask task = project.tasks.create(taskName, ApolloIRGenTask) {
      group = TASK_GROUP
      description = "Generate an IR file using apollo-codegen for ${sourceSetOrVariantName.capitalize()} GraphQL queries"
      dependsOn(ApolloCodeGenInstallTask.NAME)
      sourceSets.each { sourceSet ->
        inputs.file(sourceSet.graphql).skipWhenEmpty()
      }
    }

    ImmutableList.Builder<String> sourceSetNamesList = ImmutableList.builder();
    sourceSets.each { sourceSet -> sourceSetNamesList.add(sourceSet.name) }

    task.init(sourceSetOrVariantName, sourceSetNamesList.build())
    return task
  }

  private ApolloClassGenTask createApolloClassGenTask(String name, Map<String, String> customTypeMapping,
                                                      String nullableValueType, boolean generateAccessors) {
    String taskName = String.format(ApolloClassGenTask.NAME, name.capitalize())
    ApolloClassGenTask task = project.tasks.create(taskName, ApolloClassGenTask) {
      group = TASK_GROUP
      description = "Generate Android classes for ${name.capitalize()} GraphQL queries"
      dependsOn(getProject().getTasks().findByName(String.format(ApolloIRGenTask.NAME, name.capitalize())));
      source = project.tasks.findByName(String.format(ApolloIRGenTask.NAME, name.capitalize())).outputDir
      include "**${File.separatorChar}*API.json"
    }
    task.init(name, customTypeMapping, nullableValueType, generateAccessors)
    return task
  }

  private void createSourceSetExtensions() {
    getSourceSets().all { sourceSet ->
      sourceSet.extensions.create(GraphQLSourceDirectorySet.NAME, GraphQLSourceDirectorySet, sourceSet.name,
          fileResolver)
    }
  }

  private boolean isAndroidProject() {
    return project.hasProperty('android') && project.android.sourceSets
  }

  private Object getSourceSets() {
    return (isAndroidProject() ? project.android.sourceSets : project.sourceSets)
  }

  private DomainObjectCollection<BaseVariant> getVariants() {
    return project.android.hasProperty(
        'libraryVariants') ? project.android.libraryVariants : project.android.applicationVariants
  }

}
