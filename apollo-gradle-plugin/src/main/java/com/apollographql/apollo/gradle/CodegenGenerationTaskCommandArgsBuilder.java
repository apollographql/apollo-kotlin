package com.apollographql.apollo.gradle;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import org.gradle.api.GradleException;
import org.gradle.api.internal.AbstractTask;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;

class CodegenGenerationTaskCommandArgsBuilder {
  private final AbstractTask task;
  private final String schemaFilePath;
  private final String outputPackageName;
  private final File outputFolder;
  private final String variant;
  private final List<String> sourceSets;

  CodegenGenerationTaskCommandArgsBuilder(AbstractTask task, String schemaFilePath, String outputPackageName,
      File outputFolder, String variant, List<String> sourceSets) {
    this.task = task;
    this.schemaFilePath = schemaFilePath;
    this.outputPackageName = outputPackageName;
    this.outputFolder = outputFolder;
    this.variant = variant;
    this.sourceSets = sourceSets;
  }

  public List<ApolloCodegenArgs> buildCodegenArgs() {
    File schemaFile = null;
    if (schemaFilePath != null && !schemaFilePath.trim().isEmpty()) {
      schemaFile = Paths.get(schemaFilePath).toFile();
      if (!schemaFile.exists()) {
        schemaFile = Paths.get(task.getProject().getProjectDir().getAbsolutePath(), schemaFilePath).toFile();
      }

      if (!schemaFile.exists()) {
        throw new GradleException("Provided schema file path doesn't exists: " + schemaFilePath +
            ". Please ensure a valid schema file exists");
      }
    }

    File targetPackageFolder = null;
    if (schemaFile != null) {
      if (outputPackageName == null || outputPackageName.trim().isEmpty()) {
        throw new GradleException("Missing explicit outputPackageName option. Please ensure a valid package name is provided");
      } else {
        targetPackageFolder = new File(outputFolder.getAbsolutePath()
            + File.separator + "src"
            + File.separator + "main"
            + File.separator + "graphql"
            + File.separator + outputPackageName.replace(".", File.separator));
      }
    }

    final List<ApolloCodegenArgs> codegenArgs;
    if (schemaFile == null) {
      codegenArgs = codeGenArgs(task.getInputs().getSourceFiles().getFiles());
    } else {
      Set<String> queryFilePaths = new HashSet<>();
      for (File queryFile : queryFilesFrom(task.getInputs().getSourceFiles().getFiles())) {
        queryFilePaths.add(queryFile.getAbsolutePath());
      }
      codegenArgs = Collections.singletonList(new ApolloCodegenArgs(schemaFile, queryFilePaths, targetPackageFolder));
    }

    return codegenArgs;
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
      }).toSet(), new File(outputFolder.getAbsolutePath() + File.separator + task.getProject().relativePath(f.getParent()
      ))));
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
  private static boolean illegalSchemasFound(Collection<File> schemaFiles) {
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
    Path basePath = Paths.get(task.getProject().file("src").getAbsolutePath());

    return basePath.relativize(absolutePath).toString().split(Matcher.quoteReplacement(File.separator))[0];
  }

  /**
   * Returns the file path relative to the sourceSet directory
   *
   * @return path relative to sourceSet directory
   */
  private String getPathRelativeToSourceSet(File file) {
    Path absolutePath = Paths.get(file.getAbsolutePath());
    Path basePath = Paths.get(task.getProject().file("src").getAbsolutePath() + File.separator + getSourceSetNameFromFile(file));
    return basePath.relativize(absolutePath).toString();
  }

  static final class ApolloCodegenArgs {
    final File schemaFile;
    final Set<String> queryFilePaths;
    final File outputFolder;

    ApolloCodegenArgs(File schemaFile, Set<String> queryFilePaths, File outputFolder) {
      this.schemaFile = schemaFile;
      this.queryFilePaths = queryFilePaths;
      this.outputFolder = outputFolder;
    }
  }
}
