package com.apollographql.android.gradle;

import java.io.File;
import java.util.Set;


public class ApolloCodegenArgs {
  private File schemaFile;
  private Set<String> queryFiles;

  public ApolloCodegenArgs(File schema, Set<String> queries) {
      schemaFile = schema;
      queryFiles = queries;
    }

  public File getSchemaFile() {
    return schemaFile;
  }

  public void setSchemaFile(File schemaFile) {
    this.schemaFile = schemaFile;
  }

  public Set<String> getQueryFiles() {
    return queryFiles;
  }

  public void setQueryFiles(Set<String> queryFiles) {
    this.queryFiles = queryFiles;
  }
}
