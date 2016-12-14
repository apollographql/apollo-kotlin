package com.apollostack.android.gradle

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.gradle.api.Project
import org.gradle.api.tasks.util.PatternSet

class ApolloExtension {
  private final Project project
  static String NAME = "apollo"
  String graphqlPath
  // Maps GraphQL query files' relative paths to a sourceSet directory
  private Multimap<String, String> cachedFiles

  ApolloExtension(Project project, String sourceSet) {
    this.project = project
    this.graphqlPath = "src/$sourceSet/graphql"
  }

  Multimap<String, String> getFiles() {
    if (cachedFiles != null) {
      return cachedFiles
    }
    PatternSet patternSet = new PatternSet().include("**/*.graphql")
    ArrayListMultimap<String, String> files = ArrayListMultimap.create()
    project.files(graphqlPath).getAsFileTree().matching(patternSet).visit { el ->
      if (!el.directory) {
        files.put(graphqlPath, element.relativePath.pathString)
      }
    }
    cachedFiles = files
    return cachedFiles
  }
}
