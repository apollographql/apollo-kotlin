package com.apollographql.apollo.gradle;

import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;

public final class GraphQLSourceDirectorySet {
  public static final String NAME = "graphql";
  static final String SCHEMA_FILE_NAME = "schema.json";

  private static final String GRAPHQL_QUERY_PATTERN = "**/*.graphql";
  private static final String GQL_QUERY_PATTERN = "**/*.gql";
  private static final String SCHEMA_FILE_PATTERN = "**/" + SCHEMA_FILE_NAME;

  public static SourceDirectorySet create(String name, ObjectFactory objectFactory) {
    SourceDirectorySet sourceSet = objectFactory.sourceDirectorySet(NAME, String.format("%s GraphQL source", NAME));
    sourceSet.srcDir("src/" + name + "/graphql");
    sourceSet.include(GRAPHQL_QUERY_PATTERN, GQL_QUERY_PATTERN, SCHEMA_FILE_PATTERN);
    return sourceSet;
  }
}
