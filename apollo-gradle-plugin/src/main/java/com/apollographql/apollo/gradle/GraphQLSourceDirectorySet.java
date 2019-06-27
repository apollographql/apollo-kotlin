package com.apollographql.apollo.gradle;

import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory;

public class GraphQLSourceDirectorySet extends DefaultSourceDirectorySet {
  static final String SCHEMA_FILE_NAME = "schema.json";
  static final String NAME = "graphql";

  private static final String GRAPHQL_QUERY_PATTERN = "**/*.graphql";
  private static final String GQL_QUERY_PATTERN = "**/*.gql";
  private static final String SCHEMA_FILE_PATTERN = "**/" + SCHEMA_FILE_NAME;

  public GraphQLSourceDirectorySet(String name, FileResolver fileResolver) {
    super(name, String.format("%s GraphQL source", name), fileResolver, new DefaultDirectoryFileTreeFactory());
    srcDir("src/" + name + "/graphql");
    include(GRAPHQL_QUERY_PATTERN, GQL_QUERY_PATTERN, SCHEMA_FILE_PATTERN);
  }
}
