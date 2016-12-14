package com.apollostack.android.gradle

import com.google.common.collect.Sets
import com.moowork.gradle.node.task.NodeTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.util.PatternSet

public class ApolloIRgenTask extends NodeTask {
  private static final String APOLLO_CODEGEN = "node_modules/apollo-codegen/lib/cli.js"
  static final String NAME = "generate%sApolloIR"
  List<ApolloExtension> config
  String variant

  @InputFiles
  Collection<File> getInputFiles() {
    Set<File> inputFiles = Sets.newHashSet()
    config.each { ext ->
      ext.getFiles().asMap().entrySet().each { entry ->
        entry.value.each { file ->
          inputFiles.add(new File(entry.key, file))
        }
      }
    }
    return inputFiles
  }

  @Override
  public void exec() {
    File apolloScript = project.file(APOLLO_CODEGEN)
    if (!apolloScript.isFile()) {
      throw new GradleException("Apollo-codegen was not found in node_modules. Please run 'gradle ${ApolloCodegenInstallTask.NAME}")
    }
    setScript(apolloScript)
    List<String> apolloArgs = ["generate"]
    apolloArgs.addAll(getInputFiles()*.absolutePath)
    apolloArgs.addAll(["--schema", getSchemaPath(), "--output", "${variant.capitalize()}API.json", "--target json"]);

    setArgs(apolloArgs)
    super.exec()
  }

  private String getSchemaPath() {
    String schemaPath = ""
    PatternSet patternSet = new PatternSet().include("**/schema.json")
    config.graphqlPath.any { path ->
      project.files(path).getAsFileTree().matching(patternSet).visit { element ->
        if (!element.directory) {
          schemaPath = project.files("${path}/${element.path}").asPath
          return true
        }
      }
    }
    if (schemaPath.isEmpty()) {
      throw new GradleException("Couldn't find a schema file. Please ensure a valid schema.json files exists in the " +
          "sourceSet directory")
    }
    return schemaPath
  }
}
