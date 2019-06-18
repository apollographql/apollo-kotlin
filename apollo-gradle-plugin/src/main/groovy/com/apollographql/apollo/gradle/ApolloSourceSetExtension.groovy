package com.apollographql.apollo.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Property

class ApolloSourceSetExtension {
  static final String NAME = "sourceSet"

  final Property<String> schemaFile
  final Property<List> exclude

  ApolloSourceSetExtension(Project project) {
    schemaFile = project.objects.property(String.class)
    schemaFile.set("")

    exclude = project.objects.property(List.class)
    exclude.set(new ArrayList<String>())
  }

  void setSchemaFile(String schemaFile) {
    this.schemaFile.set(schemaFile)
  }

  void setExclude(List<String> exclude) {
    this.exclude.set(exclude)
  }

  void setExclude(String exclude) {
    this.exclude.set(Collections.singletonList(exclude))
  }
}
