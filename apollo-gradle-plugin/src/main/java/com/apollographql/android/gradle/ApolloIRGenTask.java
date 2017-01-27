package com.apollographql.android.gradle;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;

import com.apollographql.android.compiler.GraphQLCompiler;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.moowork.gradle.node.task.NodeTask;

public class ApolloIRGenTask extends NodeTask {
  private static final String APOLLO_CODEGEN = "node_modules/apollo-codegen/lib/cli.js";
  private static final String DEFAULT_OUTPUT_DIR = "src/main/graphql";
  private static final String DEFAULT_SCHEMA_FILE_PATTERN = "**/schema.json";
  static final String NAME = "generate%sApolloIR";

  @Internal private String variant;
  @Internal private List<GraphQLExtension> config;
  private List<String> possibleGraphQLPaths;

  /** Output directory for the generated IR, defaults to src/main/graphql **/
  @OutputDirectory private File outputDir;

  @InputFiles
  private Set<File> getInputFiles() {
    Set<File> inputFiles = Sets.newHashSet();
    for (GraphQLExtension ext : config) {
      Set<Map.Entry<String, Collection<String>>> entrySet =
          ext.getFiles(StringUtils.isEmpty(ext.graphQLPath) ? ext.graphQLPath : "src/" + ext.getSourceSet() + "/graphql")
              .asMap()
              .entrySet();
      for (Map.Entry<String, Collection<String>> entry : entrySet) {
        for (String file : entry.getValue()) {
          inputFiles.add(new File(entry.getKey(), file));
        }
      }
    }
    return inputFiles;
  }

  public void init(String variantName, List<GraphQLExtension> extensionsConfig) {
    variant = variantName;
    config = extensionsConfig;
    // TODO: change to constant once ApolloPlugin is in java
    setGroup("apollo");
    setDescription("Generate an IR file using apollo-codegen for " + StringUtils.capitalize(variant) + " GraphQL " +
        "queries");
    // TODO: change to constant once ApolloCodeGenInstallTask is in Java
    dependsOn("installApolloCodegen");

    possibleGraphQLPaths = buildPossibleGraphQLPaths();
    File schemaFile = userProvidedSchemaFile() != null ? userProvidedSchemaFile() : searchForSchemaFile();
    outputDir = new File(getProject().getBuildDir() + "/" +
        Joiner.on(File.separator).join(GraphQLCompiler.Companion.getOUTPUT_DIRECTORY()) +
        "/generatedIR" + getProject().relativePath(schemaFile.getParent()));
  }

  @Override
  public void exec() {
    File apolloScript = getProject().file(APOLLO_CODEGEN);
    File schemaFile = userProvidedSchemaFile() != null ? userProvidedSchemaFile() : searchForSchemaFile();

    if (!apolloScript.isFile()) {
      throw new GradleException("Apollo-codegen was not found in node_modules. Please run 'gradle " +
          "installApolloCodegen");
    }

    if (!schemaFile.isFile()) {
      throw new GradleException("Couldn't find a schema file. Please ensure a valid schema.json files exists in the " +
          "sourceSet directory");
    }

    setScript(apolloScript);
    List<String> apolloArgs = Lists.newArrayList("generate");
    Set<String> inputPathSet = Sets.newHashSet(Iterables.transform(getInputFiles(), new Function<File, String>() {
      @Nullable
      @Override
      public String apply(@Nullable File file)
      {
        return getProject().file(file).getAbsolutePath();
      }
    }));
    apolloArgs.addAll(inputPathSet);

    apolloArgs.addAll(Lists.newArrayList("--schema", schemaFile.getAbsolutePath(),
        "--output", outputDir.getAbsolutePath() + "/" + StringUtils.capitalize(variant) + "API.json",
        "--target", "json"));
    setArgs(apolloArgs);
    super.exec();
  }

  private File userProvidedSchemaFile() {
    File schemaFile = null;
    ImmutableList<String> schemaFiles = FluentIterable.from(config)
        .transform(new Function<GraphQLExtension, String>() {
          @Nullable
          @Override
          public String apply(@Nullable GraphQLExtension graphQLExtension) {
            return (graphQLExtension != null ? graphQLExtension.getSchemaFile() : null);
          }
        })
        .filter(Predicates.notNull())
        .toList();

    if (!schemaFiles.isEmpty()) {
      if (schemaFiles.size() > 1) {
        throw new IllegalArgumentException("More than two schema files were specified for the build variant "
            + variant + ". Please ensure that only one schema field is specified for $variant's source sets");
      }
      schemaFile = getProject().file(schemaFiles.get(0));
    }
    return schemaFile;
  }

  private File searchForSchemaFile() {
    final File[] schemaFile = {null};

    PatternSet patternSet = new PatternSet().include(DEFAULT_SCHEMA_FILE_PATTERN);

    for (String path : possibleGraphQLPaths) {
      getProject().files(path).getAsFileTree().matching(patternSet).visit(new Action<FileVisitDetails>() {
        @Override
        public void execute(FileVisitDetails fileVisitDetails) {
          if (!fileVisitDetails.isDirectory()) {
            schemaFile[0] = fileVisitDetails.getFile();
          }
        }
      });
      if (schemaFile[0] != null && schemaFile[0].isFile()) {
        return schemaFile[0];
      }
    }
    return getProject().file(DEFAULT_OUTPUT_DIR + "/schema.json");
  }

  private List<String> buildPossibleGraphQLPaths() {
    List<String> graphQLPaths = new ArrayList<>();
    for (GraphQLExtension ext : config) {
      graphQLPaths.add("src/" + ext.getSourceSet() + "graphql");
    }
    for (GraphQLExtension ext : config) {
      if (!Strings.isNullOrEmpty(ext.graphQLPath)) {
        graphQLPaths.add(ext.graphQLPath);
      }
    }
    return Lists.reverse(graphQLPaths);
  }

  public File getOutputDir() {
    return outputDir;
  }
}
