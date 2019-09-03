package com.apollographql.apollo.gradle

import com.apollographql.apollo.gradle.android.AndroidTaskConfiguratorFactory
import com.apollographql.apollo.gradle.jvm.JvmTaskConfiguratorFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.FileResolver

import javax.inject.Inject

class ApolloPlugin implements Plugin<Project> {
  public static final String TASK_GROUP = "apollo"

  private Project project
  private final FileResolver fileResolver

  @Inject
  ApolloPlugin(FileResolver fileResolver) {
    this.fileResolver = fileResolver
  }

  @Override
  void apply(Project project) {
    this.project = project
    project.plugins.withId("java-base") {
      applyApolloPlugin()
    }
    project.gradle.getTaskGraph().whenReady {
      if (!project.plugins.hasPlugin("java-base")) {
        throw new IllegalArgumentException(
            "Apollo plugin couldn't be applied without Android or Java or Kotlin plugin.")
      }
    }
  }

  private void applyApolloPlugin() {
    project.extensions.create(ApolloExtension.NAME, ApolloExtension, project)
    project.apollo.extensions.create(ApolloSourceSetExtension.NAME, ApolloSourceSetExtension, project)
    createSourceSetExtensions()
    addApolloTasks()
  }

  private void addApolloTasks() {
    Task apolloClassGenTask = project.task("generateApolloClasses")
    apolloClassGenTask.group = TASK_GROUP

    if (isAndroidProject()) {
      AndroidTaskConfiguratorFactory.create(project).configureTasks(apolloClassGenTask)
    } else {
      JvmTaskConfiguratorFactory.create(project).configureTasks(apolloClassGenTask)
    }
  }

  private void createSourceSetExtensions() {
    getSourceSets().all { sourceSet ->
      sourceSet.extensions.add(
          SourceDirectorySet.class,
          GraphQLSourceDirectorySet.NAME,
          GraphQLSourceDirectorySet.create(sourceSet.name, project.objects)
      )
    }
  }

  private boolean isAndroidProject() {
    return project.hasProperty('android') && project.android.sourceSets
  }

  private Object getSourceSets() {
    return (isAndroidProject() ? project.android.sourceSets : project.sourceSets)
  }
}
