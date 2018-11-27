package com.apollographql.apollo.gradle


import com.android.build.gradle.api.BaseVariant
import com.apollographql.apollo.compiler.GraphQLCompiler
import com.google.common.base.Joiner
import com.google.common.collect.ImmutableList
import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.NodePlugin
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.AbstractTask
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile

import javax.inject.Inject

class ApolloPlugin implements Plugin<Project> {
  private static final String NODE_VERSION = "6.7.0"
  public static final String TASK_GROUP = "apollo"
  public static final String APOLLO_CODEGEN_GENERATE_TASK_NAME = "generate%sApolloIR"

  private Project project
  private final FileResolver fileResolver
  private boolean useGlobalApolloCodegen

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
    if (isUseGlobalApolloCodegenEnabled() && verifySystemApolloCodegenVersion()) {
      useGlobalApolloCodegen = true
    } else {
      setupNode()
    }

    project.extensions.create(ApolloExtension.NAME, ApolloExtension, project)
    createSourceSetExtensions()

    if (!useGlobalApolloCodegen) {
      project.tasks.create(ApolloCodegenInstallTask.NAME, ApolloCodegenInstallTask.class)
    }

    addApolloTasks()
  }

  private void addApolloTasks() {
    Task apolloIRGenTask = project.task("generateApolloIR")
    apolloIRGenTask.group = TASK_GROUP

    Task apolloClassGenTask = project.task("generateApolloClasses")
    apolloClassGenTask.group = TASK_GROUP

    if (isAndroidProject()) {
      getVariants().all { v ->
        addVariantTasks(v, apolloIRGenTask, apolloClassGenTask, v.sourceSets)
      }
      project.android.testVariants.each { tv ->
        addVariantTasks(tv, apolloIRGenTask, apolloClassGenTask, tv.sourceSets)
      }
    } else {
      getSourceSets().all { sourceSet ->
        addSourceSetTasks(sourceSet, apolloIRGenTask, apolloClassGenTask)
      }
    }
  }

  private void addVariantTasks(BaseVariant variant, Task apolloIRGenTask, Task apolloClassGenTask, Collection<?> sourceSets) {
    AbstractTask variantIRTask = createApolloIRGenTask(variant.name, sourceSets)
    ApolloClassGenerationTask variantClassTask = createApolloClassGenTask(variant.name)
    variant.registerJavaGeneratingTask(variantClassTask, variantClassTask.outputDir.asFile.get())
    apolloIRGenTask.dependsOn(variantIRTask)
    apolloClassGenTask.dependsOn(variantClassTask)
  }

  private void addSourceSetTasks(SourceSet sourceSet, Task apolloIRGenTask, Task apolloClassGenTask) {
    String taskName = "main".equals(sourceSet.name) ? "" : sourceSet.name

    AbstractTask sourceSetIRTask = createApolloIRGenTask(sourceSet.name, [sourceSet])
    ApolloClassGenerationTask sourceSetClassTask = createApolloClassGenTask(sourceSet.name)
    apolloIRGenTask.dependsOn(sourceSetIRTask)
    apolloClassGenTask.dependsOn(sourceSetClassTask)

    JavaCompile compileTask = (JavaCompile) project.tasks.findByName("compile${taskName.capitalize()}Java")
    compileTask.source += project.fileTree(sourceSetClassTask.outputDir)
    compileTask.dependsOn(apolloClassGenTask)

    sourceSet.java.srcDir(sourceSetClassTask.outputDir)

    AbstractCompile compileKotlinTask = (AbstractCompile) project.tasks.findByName(
        "compile${taskName.capitalize()}Kotlin")
    if (compileKotlinTask != null) {
      // kotlinc uses the generated java classes as input too so we need the generated classes
      compileKotlinTask.dependsOn(apolloClassGenTask)
      // this is somewhat redundant with sourceSet.java.srcDir above but I believe by the time we come here the java plugin
      // has been configured already so we need to manually tell kotlinc where to find the generated classes
      compileKotlinTask.source(sourceSetClassTask.outputDir)
    }
  }

  private void setupNode() {
    project.plugins.apply NodePlugin
    NodeExtension nodeConfig = project.extensions.findByName("node") as NodeExtension
    nodeConfig.download = true
    nodeConfig.version = NODE_VERSION
  }

  private AbstractTask createApolloIRGenTask(String sourceSetOrVariantName, Collection<Object> sourceSets) {
    ImmutableList.Builder<String> sourceSetNamesList = ImmutableList.builder()
    sourceSets.each { sourceSet ->
      sourceSetNamesList.add(sourceSet.name)
    }

    File outputFolder = new File(project.buildDir, Joiner.on(File.separator)
        .join(GraphQLCompiler.IR_OUTPUT_DIRECTORY + sourceSetOrVariantName))

    String taskName = String.format(APOLLO_CODEGEN_GENERATE_TASK_NAME, sourceSetOrVariantName.capitalize())
    if (useGlobalApolloCodegen) {
      return project.tasks.create(taskName, ApolloSystemCodegenGenerationTask) {
        sourceSets.each { sourceSet ->
          inputs.files(sourceSet.graphql).skipWhenEmpty()
        }
        group = TASK_GROUP
        description = "Generate an IR file using apollo-codegen for ${sourceSetOrVariantName.capitalize()} GraphQL queries"
        schemaFilePath = project.apollo.schemaFilePath
        outputPackageName = project.apollo.outputPackageName
        variant = sourceSetOrVariantName
        sourceSetNames = sourceSetNamesList.build()
        outputDir.set(outputFolder)
      }
    } else {
      return project.tasks.create(taskName, ApolloLocalCodegenGenerationTask) {
        sourceSets.each { sourceSet ->
          inputs.files(sourceSet.graphql).skipWhenEmpty()
        }
        group = TASK_GROUP
        description = "Generate an IR file using apollo-codegen for ${sourceSetOrVariantName.capitalize()} GraphQL queries"
        dependsOn(ApolloCodegenInstallTask.NAME)
        schemaFilePath = project.apollo.schemaFilePath
        outputPackageName = project.apollo.outputPackageName
        variant = sourceSetOrVariantName
        sourceSetNames = sourceSetNamesList.build()
        outputDir.set(outputFolder)
      }
    }
  }

  private ApolloClassGenerationTask createApolloClassGenTask(String name) {
    String taskName = String.format(ApolloClassGenerationTask.NAME, name.capitalize())
    return project.tasks.create(taskName, ApolloClassGenerationTask) {
      group = TASK_GROUP
      description = "Generate Android classes for ${name.capitalize()} GraphQL queries"
      dependsOn(getProject().getTasks().findByName(String.format(APOLLO_CODEGEN_GENERATE_TASK_NAME, name.capitalize())))
      source = project.tasks.findByName(
          String.format(APOLLO_CODEGEN_GENERATE_TASK_NAME, name.capitalize())).outputDir
      include "**${File.separatorChar}*API.json"
      customTypeMapping = project.apollo.customTypeMapping
      nullableValueType = project.apollo.nullableValueType
      useSemanticNaming = project.apollo.useSemanticNaming
      generateModelBuilder = project.apollo.generateModelBuilder
      useJavaBeansSemanticNaming = project.apollo.useJavaBeansSemanticNaming
      outputPackageName = project.apollo.outputPackageName
      suppressRawTypesWarning = project.apollo.suppressRawTypesWarning
    }
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

  private static boolean isUseGlobalApolloCodegenEnabled() {
    return (System.properties['apollographql.useGlobalApolloCodegen'] != null) &&
        System.properties['apollographql.useGlobalApolloCodegen'].toBoolean()
  }

  private static boolean verifySystemApolloCodegenVersion() {
    println("Verifying system 'apollo-codegen' version (executing command 'apollo-codegen --version') ...")
    try {
      StringBuilder output = new StringBuilder()
      Process checkGlobalApolloCodegen = "apollo-codegen --version".execute()
      checkGlobalApolloCodegen.consumeProcessOutput(output, null)
      checkGlobalApolloCodegen.waitForOrKill(5000)

      if (output.toString().trim() == GraphQLCompiler.APOLLOCODEGEN_VERSION) {
        println("Found required 'apollo-codegen@" + GraphQLCompiler.APOLLOCODEGEN_VERSION + "' version.")
        println("Skip apollo-codegen installation.")
        return true
      } else {
        println("Required 'apollo-codegen@" + GraphQLCompiler.APOLLOCODEGEN_VERSION + "' version not found.")
        println("Installing apollo-codegen ... ")
        return false
      }
    } catch (Exception e) {
      println("Failed to verify system 'apollo-codegen' version: " + e)
      println("Installing apollo-codegen ... ")
      return false
    }
  }
}
