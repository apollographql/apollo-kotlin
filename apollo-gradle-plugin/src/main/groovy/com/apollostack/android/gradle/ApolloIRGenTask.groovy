package com.apollostack.android.gradle

import com.google.common.collect.Sets
import com.moowork.gradle.node.task.NodeTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.util.PatternSet

public class ApolloIRGenTask extends NodeTask {
  private static final String APOLLO_CODEGEN = "node_modules/apollo-codegen/lib/cli.js"
  private static final String DEFAULT_OUTPUT_DIR = "src/main/graphql"
  protected static final String DEFAULT_SCHEMA_FILE_PATTERN = "**/schema.json"
  static final String NAME = "generate%sApolloIR"

  @Internal String variant
  @Internal List<ApolloExtension> config
  private List<String> possibleGraphQLPaths
  private File schemaFile
  /** Output directory for the generated IR, defaults to src/main/graphql **/
  @OutputDirectory File outputDir

  @InputFiles Collection<File> getInputFiles() {
    Set<File> inputFiles = Sets.newHashSet()
    config.each { ext ->
      ext.getFiles(ext.graphQLPath ?: "src/${ext.getSourceSet()}/graphql").asMap().entrySet().each { entry ->
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
    description = "Generate an IR file using apollo-codgen for ${variant.capitalize()} GraphQL queries"
    dependsOn(ApolloCodeGenInstallTask.NAME)

    possibleGraphQLPaths = buildPossibleGraphQLPaths()
    schemaFile = userProvidedSchemaFile() ?: searchForSchemaFile()
    outputDir = schemaFile.getParentFile()
  }

  @Override public void exec() {
    File apolloScript = project.file(APOLLO_CODEGEN)
    File schemaFile = userProvidedSchemaFile() ?: searchForSchemaFile()

    if (!apolloScript.isFile()) {
      throw new GradleException("Apollo-codegen was not found in node_modules. Please run 'gradle " +
          "${ApolloCodeGenInstallTask.NAME}")
    }

    if (!schemaFile.isFile()) {
      throw new GradleException("Couldn't find a schema file. Please ensure a valid schema.json files exists in the " +
          "sourceSet directory")
    }

    setScript(apolloScript)
    List<String> apolloArgs = ["generate"]
    apolloArgs.addAll(getInputFiles().collect { project.file(it).absolutePath })
    apolloArgs.addAll(["--schema", "${schemaFile.absolutePath}",
                       "--output", "$outputDir.absolutePath/${variant.capitalize()}API.json",
                       "--target", "json"]);
    setArgs(apolloArgs)
    super.exec()
  }

  private File userProvidedSchemaFile() {
    List<String> schemaFiles = config.schemaFile?.findAll { it != null }
    File schemaFile = null
    if (schemaFiles) {
      if (schemaFiles.size() > 1) {
        throw new IllegalArgumentException("More than two schema files were specified for the build variant $variant." +
            " Please ensure that only one schema field is specified for $variant's source sets")
      }
      schemaFile = project.file(schemaFile.get(0))
    }
    return schemaFile
  }

  private File searchForSchemaFile() {
    File schemaFile = null
    PatternSet patternSet = new PatternSet().include(DEFAULT_SCHEMA_FILE_PATTERN)
    for (String path : possibleGraphQLPaths) {
      project.files(path).getAsFileTree().matching(patternSet).visit {
        if (!it.directory) {
          schemaFile = it.file
          return true
        }
      }
      if (schemaFile?.isFile()) {
        return schemaFile
      }
    }
    return project.file("$DEFAULT_OUTPUT_DIR/schema.json")
  }

  private List<String> buildPossibleGraphQLPaths() {
    List<String> graphQLPaths = new ArrayList<>();
    config.each { ext ->
      graphQLPaths.add("src/${ext.getSourceSet()}/graphql")
    }
    config.each { ext ->
      if (ext.graphQLPath) {
        graphQLPaths.add(ext.graphQLPath)
      }
    }
    return graphQLPaths.reverse()
  }
}
