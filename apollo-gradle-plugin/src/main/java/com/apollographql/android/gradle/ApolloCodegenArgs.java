package com.apollographql.android.gradle;

import java.io.File;
import java.util.Set;

final class ApolloCodegenArgs {
  private final File schemaFile;
  private final Set<String> queryFiles;

  ApolloCodegenArgs(File schema, Set<String> queries) {
      schemaFile = schema;
      queryFiles = queries;
    }

  File getSchemaFile() {
    return schemaFile;
  }

  Set<String> getQueryFiles() {
    return queryFiles;
  }

}
