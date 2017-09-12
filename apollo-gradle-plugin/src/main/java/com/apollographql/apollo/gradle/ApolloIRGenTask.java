package com.apollographql.apollo.gradle;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.apollographql.apollo.compiler.GraphQLCompiler;
import com.moowork.gradle.node.task.NodeTask;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import javax.annotation.Nullable;

public class ApolloIRGenTask extends NodeTask {
  static final String APOLLO_CODEGEN_EXEC_FILE = "lib/cli.js";
  private static final String APOLLO_CODEGEN = "apollo-codegen/node_modules/apollo-codegen/" + APOLLO_CODEGEN_EXEC_FILE;
  static final String NAME = "generate%sApolloIR";

  @Internal private String variant;
  @Internal private ImmutableList<String> sourceSets;
  @Internal private ApolloExtension extension;

  @OutputDirectory private File outputFolder;

  public void init(String variant, ImmutableList<String> sourceSets, ApolloExtension extension) {
    this.variant = variant;
    this.sourceSets = sourceSets;
    this.extension = extension;
    outputFolder = new File(getProject().getBuildDir() + File.separator +
        Joiner.on(File.separator).join(GraphQLCompiler.Companion.getOUTPUT_DIRECTORY()) + "/generatedIR/" + variant);
  }

  @Override
  public void exec() {
    File schemaFile = null;
    if (extension.getSchemaFilePath() != null) {
      schemaFile = Paths.get(extension.getSchemaFilePath()).toFile();
      if (!schemaFile.exists()) {
        schemaFile = Paths.get(getProject().getRootDir().getAbsolutePath(), extension.getSchemaFilePath()).toFile();
      }

      if (!schemaFile.exists()) {
        throw new GradleException("Provided schema file path doesn't exists: " + extension.getSchemaFilePath() +
            ". Please ensure a valid schema file exists");
      }
    }

    File targetPackageFolder = null;
    if (schemaFile != null) {
      if (extension.getOutputPackageName() == null || extension.getOutputPackageName().trim().isEmpty()) {
        throw new GradleException("Missing explicit targetPackageName option. Please ensure a valid package name is provided");
      } else {
        targetPackageFolder = new File(outputFolder.getAbsolutePath()
            + File.separator + "src"
            + File.separator + "main"
            + File.separator + "graphql"
            + File.separator + extension.getOutputPackageName().replace(".", File.separator));
      }
    }

    File apolloScript = new File(getProject().getBuildDir(), APOLLO_CODEGEN);
    if (!apolloScript.isFile()) {
      throw new GradleException("Apollo-codegen was not found in node_modules. Please run the installApolloCodegen task.");
    }
    setScript(apolloScript);

    final List<ApolloCodegenArgs> codegenArgs;
    if (schemaFile == null) {
      codegenArgs = codeGenArgs(getInputs().getSourceFiles().getFiles());
    } else {
      Set<String> queryFilePaths = new HashSet<>();
      for (File queryFile : queryFilesFrom(getInputs().getSourceFiles().getFiles())) {
        queryFilePaths.add(queryFile.getAbsolutePath());
      }
      codegenArgs = Collections.singletonList(new ApolloCodegenArgs(schemaFile, queryFilePaths, targetPackageFolder));
    }

    for (ApolloCodegenArgs codegenArg : codegenArgs) {
      codegenArg.irOutputFolder.mkdirs();

      List<String> apolloArgs = new ArrayList<>();
      apolloArgs.add("generate");
      apolloArgs.addAll(codegenArg.queryFilePaths);
      apolloArgs.addAll(Arrays.asList(
          "--add-typename",
          "--schema", codegenArg.schemaFile.getAbsolutePath(),
          "--output", codegenArg.irOutputFolder.getAbsolutePath() + File.separator + Utils.capitalize(variant) + "API.json",
          "--operation-ids-path", codegenArg.irOutputFolder.getAbsolutePath() + File.separator + Utils.capitalize(variant) + "OperationIdMap.json",
          "--merge-in-fields-from-fragment-spreads", "false",
          "--target", "json"
      ));
      setArgs(apolloArgs);
      super.exec();
    }
  }

  /**
   * Extracts schema files from the task inputs and sorts them in a way similar to the Gradle lookup priority. That is,
   * build variant source set, build type source set, product flavor source set and finally main source set.
   *
   * The schema file under the source set with the highest priority is used and all the graphql query files under the
   * schema file's subdirectories from all source sets are used to generate the IR.
   *
   * If any of the schema file's ancestor directories contain a schema file, a GradleException is thrown. This is
   * considered to be an ambiguous case.
   *
   * @param files - task input files which consist of .graphql query files and schema.json files
   * @return - a map with schema files as a key and associated query files as a value
   */
  private List<ApolloCodegenArgs> codeGenArgs(Set<File> files) {
    final List<File> schemaFiles = getSchemaFilesFrom(files);

    if (schemaFiles.isEmpty()) {
      throw new GradleException("Couldn't find schema files for the variant " + Utils.capitalize(variant) + ". Please" +
          " ensure a valid schema.json exists under the varian't source sets");
    }

    if (illegalSchemasFound(schemaFiles)) {
      throw new GradleException("Found an ancestor directory to a schema file that contains another schema file." +
          " Please ensure no schema files exist on the path to another one");
    }

    ImmutableMap.Builder<String, ApolloCodegenArgs> schemaQueryMap = ImmutableMap.builder();
    for (final File f : schemaFiles) {
      final String normalizedSchemaFileName = getPathRelativeToSourceSet(f);
      // ensures that only the highest priority schema file is used
      if (schemaQueryMap.build().containsKey(normalizedSchemaFileName)) {
        continue;
      }
      schemaQueryMap.put(normalizedSchemaFileName, new ApolloCodegenArgs(f, FluentIterable.from(files).filter(new Predicate<File>() {
        @Override public boolean apply(@Nullable File file) {
          return file != null && !schemaFiles.contains(file) && file.getParent().contains(getPathRelativeToSourceSet(f.getParentFile()));
        }
      }).transform(new Function<File, String>() {
        @Nullable @Override public String apply(@Nullable File file) {
          return file.getAbsolutePath();
        }
      }).toSet(), new File(outputFolder.getAbsolutePath() + File.separator + getProject().relativePath(f.getParent()))));
    }
    return schemaQueryMap.build().values().asList();
  }

  /**
   * Returns "schema.json" files and sorts them based on their source set priorities.
   *
   * @return - schema files sorted by priority based on source set priority
   */
  private List<File> getSchemaFilesFrom(Set<File> files) {
    return FluentIterable.from(files).filter(new Predicate<File>() {
      @Override public boolean apply(@Nullable File file) {
        return file != null && file.getName().equals(GraphQLSourceDirectorySet.SCHEMA_FILE_NAME);
      }
    }).toSortedList(new Comparator<File>() {
      @Override public int compare(File o1, File o2) {
        String sourceSet1 = getSourceSetNameFromFile(o1);
        String sourceSet2 = getSourceSetNameFromFile(o2);
        // negative because the sourceSets list is in reverse order
        return -(sourceSets.indexOf(sourceSet1) - sourceSets.indexOf(sourceSet2));
      }
    });
  }

  private List<File> queryFilesFrom(Set<File> files) {
    return FluentIterable.from(files).filter(new Predicate<File>() {
      @Override public boolean apply(@Nullable File file) {
        return file != null && !file.getName().equals(GraphQLSourceDirectorySet.SCHEMA_FILE_NAME);
      }
    }).toList();
  }

  /**
   * Checks whether a schema file share an ancestor directory that also contains a schema file
   *
   * @param schemaFiles - task's input that have been identified as schema file
   * @return - whether illegal schema files were found
   */
  private boolean illegalSchemasFound(Collection<File> schemaFiles) {
    for (final File f : schemaFiles) {
      final Path parent = Paths.get(f.getParent()).toAbsolutePath();
      List<File> matches = FluentIterable.from(schemaFiles).filter(new Predicate<File>() {
        @Override public boolean apply(@Nullable File file) {
          return file != null && file != f && Paths.get(file.getParent()).startsWith(parent);
        }
      }).toList();

      if (!matches.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the source set folder name given a file path. Assumes the source set name follows the "src" folder based on
   * the inputs received from GraphQLSourceDirectorySet.
   *
   * @return - sourceSet name
   */
  private String getSourceSetNameFromFile(File file) {
    Path absolutePath = Paths.get(file.getAbsolutePath());
    Path basePath = Paths.get(getProject().file("src").getAbsolutePath());

    return basePath.relativize(absolutePath).toString().split(Matcher.quoteReplacement(File.separator))[0];
  }

  /**
   * Returns the file path relative to the sourceSet directory
   *
   * @return path relative to sourceSet directory
   */
  private String getPathRelativeToSourceSet(File file) {
    Path absolutePath = Paths.get(file.getAbsolutePath());
    Path basePath = Paths.get(getProject().file("src").getAbsolutePath() + File.separator + getSourceSetNameFromFile(file));

    return basePath.relativize(absolutePath).toString();
  }

  public File getOutputFolder() {
    return outputFolder;
  }
}
