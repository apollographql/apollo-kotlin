package com.apollographql.android.gradle;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.tasks.util.PatternSet;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class GraphQLExtension {
  private static final String GRAPHQL_QUERY_PATTERN = "**/*.${GraphQLCompiler.FILE_EXTENSION}";
  static final String NAME = "graphql";

  private final Project project;
  private final String sourceSet;
  // Maps GraphQL query files' relative paths to a sourceSet directory
  private Multimap<String, String> cachedFiles;

  // Configurable extension params
  String graphQLPath;
  String schemaFile;

  public GraphQLExtension(Project project, String sourceSet) {
    this.project = project;
    this.sourceSet = sourceSet;
  }

  Multimap<String, String> getFiles(final String path) {
    if (cachedFiles != null) {
      return cachedFiles;
    }
    PatternSet patternSet = new PatternSet().include(GRAPHQL_QUERY_PATTERN);
    final ArrayListMultimap<String, String> files = ArrayListMultimap.create();
    project.files(path).getAsFileTree().matching(patternSet).visit(new Action<FileVisitDetails>() {
      @Override
      public void execute(FileVisitDetails fileVisitDetails) {
        if (!fileVisitDetails.isDirectory()) {
          files.put(path, fileVisitDetails.getRelativePath().getPathString());
        }
      }
    });
    cachedFiles = files;
    return cachedFiles;
  }

  protected String getSourceSet() {
    return sourceSet;
  }

  protected String getSchemaFile() {
    return schemaFile;
  }

  protected void setSchemaFile(String schemaFile) {
    this.schemaFile = schemaFile;
  }

  protected String getGraphQLPath() {
    return graphQLPath;
  }

  protected void setGraphQLPath(String graphQLPath) {
    this.graphQLPath = graphQLPath;
  }
}
