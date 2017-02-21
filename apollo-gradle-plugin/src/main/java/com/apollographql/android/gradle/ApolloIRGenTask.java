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
  @Internal private ImmutableList<String> sourceSetPriority;

  @OutputDirectory private File outputDir;

  public void init(String variantName, ImmutableList<String> variantSourceSets) {
    variant = variantName;
    sourceSetPriority = variantSourceSets.reverse();
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
        return sourceSetPriority.indexOf(getSourceSetNameFromFile(o1)) - sourceSetPriority.indexOf(getSourceSetNameFromFile(o2));
      }
    });

    if (illegalSchemasFound(schemaFiles)) {
            throw new GradleException("Found an ancestor directory to a schema file that contains another schema file." +
                " Please ensure no schema files exist on the path to another one");
    }

    Map<String, ApolloCodegenArgs> schemaQueryMap = new HashMap<>();

    for (final File f : schemaFiles) {
      final String normalizedSchemaFileName = normalizeFileName(f);
      if (schemaQueryMap.containsKey(normalizedSchemaFileName)) {
        continue;
      }
      final Path schemaFileParentPath = Paths.get(f.getParent()).toAbsolutePath();
      schemaQueryMap.put(normalizedSchemaFileName, new ApolloCodegenArgs(f, FluentIterable.from(files).filter(new Predicate<File>() {
        @Override public boolean apply(@Nullable File file) {
          return file != null && !schemaFiles.contains(file) && file.getParent().contains(normalizeFileName(f.getParentFile()));
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

  private String getSourceSetNameFromFile(File file) {
    return file.getAbsolutePath().split("src/")[1].split("/")[0];
  }

  private String normalizeFileName(File file) {
    return file.getAbsolutePath().split("src/")[1].split("/", 2)[1];
  }

  public File getOutputDir() {
    return outputDir;
  }
}
