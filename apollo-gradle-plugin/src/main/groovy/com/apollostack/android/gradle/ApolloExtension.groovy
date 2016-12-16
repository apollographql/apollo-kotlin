package com.apollostack.android.gradle

import com.apollostack.compiler.GraphQLCompiler
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
    PatternSet patternSet = new PatternSet().include("**/*.${GraphQLCompiler.FILE_EXTENSION}")
    ArrayListMultimap<String, String> files = ArrayListMultimap.create()
    project.files(graphqlPath).getAsFileTree().matching(patternSet).visit {
      if (!it.directory) {
        files.put(graphqlPath, it.relativePath.pathString)
      }
    }
    cachedFiles = files
    return cachedFiles
  }
}
