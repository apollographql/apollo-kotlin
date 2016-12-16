package com.apollostack.android.gradle

import com.google.common.collect.Sets
import com.moowork.gradle.node.task.NodeTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.util.PatternSet

public class ApolloIRGenTask extends NodeTask {
  private static final String APOLLO_CODEGEN = "node_modules/apollo-codegen/lib/cli.js"
  private static final String DEFAULT_OUTPUT_DIR = "src/main/graphql"

  static final String NAME = "generate%sApolloIR"
  List<ApolloExtension> config
  String variant

  @OutputDirectory
  /** Output directory for the generated IR, default to src/main/graphql **/
  File outputDir

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

  public void init(String variantName, List<ApolloExtension> extensionsConfig) {
    variant = variantName
    config = extensionsConfig
    group = ApolloPlugin.TASK_GROUP
    description = "Generate IR files using apollo-codgen for ${variant.capitalize()} GraphQL queries"
    dependsOn(ApolloCodeGenInstallTask.NAME)
    outputDir = new File("${getSchemaPath()}/generatedIR")
  }

  @Override
  public void exec() {
    File apolloScript = project.file(APOLLO_CODEGEN)

    if (!apolloScript.isFile()) {
      throw new GradleException("Apollo-codegen was not found in node_modules. Please run 'gradle " +
          "${ApolloCodeGenInstallTask.NAME}")
    }
    if (!new File("$outputDir.parent/schema.json").isFile()) {
      throw new GradleException("Couldn't find a schema file. Please ensure a valid schema.json files exists in the " +
          "sourceSet directory")
    }

    setScript(apolloScript)
    List<String> apolloArgs = ["generate"]
    apolloArgs.addAll(getInputFiles().collect { project.file(it).absolutePath })
    apolloArgs.addAll(["--schema", "$outputDir.parent/schema.json",
                       "--output", "$outputDir.absolutePath/${variant.capitalize()}API.json",
                       "--target", "json"]);
    setArgs(apolloArgs)
    super.exec()
  }

  private String getSchemaPath() {
    String schemaPath = ""
    PatternSet patternSet = new PatternSet().include("**/schema.json")
    for (String path : config.reverse().graphqlPath) {
      project.files(path).getAsFileTree().matching(patternSet).visit {
        if (!it.directory) {
          schemaPath = it.file.parent
          return true
        }
      }
      if (!schemaPath.isEmpty()) {
        return schemaPath
      }
    }
    return DEFAULT_OUTPUT_DIR
  }
}
