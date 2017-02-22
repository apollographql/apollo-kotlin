package com.apollographql.android.gradle;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;

import com.apollographql.android.compiler.GraphQLCompiler;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.moowork.gradle.node.task.NodeTask;

public class ApolloIRGenTask extends NodeTask {
  private static final String APOLLO_CODEGEN = "node_modules/apollo-codegen/lib/cli.js";
  static final String NAME = "generate%sApolloIR";

  @Internal private String variant;
  @Internal private ImmutableList<String> sourceSets;

  @OutputDirectory private File outputDir;

  public void init(String variantName, ImmutableList<String> variantSourceSets) {
    variant = variantName;
    sourceSets = variantSourceSets;
    outputDir = new File(getProject().getBuildDir() + "/" +
        Joiner.on(File.separator).join(GraphQLCompiler.Companion.getOUTPUT_DIRECTORY()) + "/generatedIR/" + variant);
  }

  @Override
  public void exec() {
    File apolloScript = getProject().file(APOLLO_CODEGEN);
    if (!apolloScript.isFile()) {
      throw new GradleException("Apollo-codegen was not found in node_modules. Please run the installApolloCodegen task.");
    }
    setScript(apolloScript);

    Map<String, ApolloCodegenArgs> schemaQueryMap = buildSchemaQueryMap(getInputs().getSourceFiles().getFiles());
    for (Map.Entry<String, ApolloCodegenArgs> entry : schemaQueryMap.entrySet()) {
      String irOutput = outputDir.getAbsolutePath() + "/" + getProject().relativePath(entry.getValue().getSchemaFile().getParent());
      new File(irOutput).mkdirs();

      List<String> apolloArgs = Lists.newArrayList("generate");
      apolloArgs.addAll(entry.getValue().getQueryFiles());
      apolloArgs.addAll(Lists.newArrayList("--schema", entry.getValue().getSchemaFile().getAbsolutePath(),
          "--output", irOutput + "/" + Utils.capitalize(variant) + "API.json", "--target", "json"));
      setArgs(apolloArgs);
      super.exec();
    }
  }

  /**
   * Extracts schema files from the task inputs and sorts them in a way similar to the Gradle lookup priority.
   * That is, build variant source set, build type source set, product flavor source set and finally main
   * source set.
   *
   * The schema file under the source set with the highest priority is used and all the graphql query files under the
   * schema file's subdirectories from all source sets are used to generate the IR.
   *
   * If any of the schema file's ancestor directories contain a schema file, a GradleException is
   * thrown. This is considered to be an ambiguous case.
   *
   * @param files - task input files which consist of .graphql query files and schema.json files
   * @return - a map with schema files as a key and associated query files as a value
   */
  private Map<String, ApolloCodegenArgs> buildSchemaQueryMap(Set<File> files) {
    final List<File> schemaFiles = FluentIterable.from(files).filter(new Predicate<File>() {
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

    if (schemaFiles.isEmpty()) {
      throw new GradleException("Couldn't find schema files for the variant " + Utils.capitalize(variant) + ". Please" +
          " ensure a valid schema.json exists under the varian't source sets");
    }

    if (illegalSchemasFound(schemaFiles)) {
            throw new GradleException("Found an ancestor directory to a schema file that contains another schema file." +
                " Please ensure no schema files exist on the path to another one");
    }

    Map<String, ApolloCodegenArgs> schemaQueryMap = new HashMap<>();
    for (final File f : schemaFiles) {
      final String normalizedSchemaFileName = getPathRelativeToSourceSet(f);
      // ensures that only the highest priority schema file is used
      if (schemaQueryMap.containsKey(normalizedSchemaFileName)) {
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
      }).toSet()));
    }
    return schemaQueryMap;
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
   * Returns the source set folder name given a file path. Assumes the source set name
   * follows the "src" folder based on the inputs received from GraphQLSourceDirectorySet.
   *
   * @return - sourceSet name
   */
  private String getSourceSetNameFromFile(File file) {
    Path absolutePath = Paths.get(file.getAbsolutePath());
    Path basePath = Paths.get(getProject().file("src").getAbsolutePath());

    return basePath.relativize(absolutePath).toString().split("/")[0];
  }

  /**
   * Returns the file path relative to the sourceSet directory
   *
   * @return path relative to sourceSet directory
   */
  private String getPathRelativeToSourceSet(File file) {
    Path absolutePath = Paths.get(file.getAbsolutePath());
    Path basePath = Paths.get(getProject().file("src").getAbsolutePath() + "/" + getSourceSetNameFromFile(file));

    return basePath.relativize(absolutePath).toString();
  }

  public File getOutputDir() {
    return outputDir;
  }
}
