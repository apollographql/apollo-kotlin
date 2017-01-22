package com.apollostack.android.gradle

import com.apollostack.compiler.GraphQLCompiler
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.gradle.api.Project
import org.gradle.api.tasks.util.PatternSet

class GraphQLExtension {
  private static final String GRAPHQL_QUERY_PATTERN = "**/*.${GraphQLCompiler.FILE_EXTENSION}"
  static final String NAME = "graphql"

  private final Project project
  private final String sourceSet
  // Maps GraphQL query files' relative paths to a sourceSet directory
  private Multimap<String, String> cachedFiles

  // Configurable extension params
  String graphQLPath
  String schemaFile

  GraphQLExtension(Project project, String sourceSet) {
    this.project = project
    this.sourceSet = sourceSet
  }

  Multimap<String, String> getFiles(String path) {
    if (cachedFiles != null) {
      return cachedFiles
    }
    PatternSet patternSet = new PatternSet().include(GRAPHQL_QUERY_PATTERN)
    ArrayListMultimap<String, String> files = ArrayListMultimap.create()
    project.files(path).getAsFileTree().matching(patternSet).visit {
      if (!it.directory) {
        files.put(path, it.relativePath.pathString)
      }
    }
    cachedFiles = files
    return cachedFiles
  }

  protected String getSourceSet() {
    return sourceSet
  }
}
