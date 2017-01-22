package com.apollostack.android.gradle

import com.apollostack.compiler.GraphQLCompiler
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.gradle.api.Project
import org.gradle.api.tasks.util.PatternSet

class ApolloExtension {
  static final String NAME = "apollo"

  boolean generateClasses = false
}
