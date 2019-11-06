package com.apollographql.apollo.gradle.api

import org.gradle.api.file.*
import org.gradle.api.provider.Provider

@ApolloGraphqlDslMarker
interface CompilationUnit {
  val name: String
  val serviceName: String
  val variantName: String
  val androidVariant: Any?

  var compilerParams: CompilerParams

  val outputDir: Provider<Directory>
  val transformedQueriesDir: Provider<Directory>

  fun setSources(rootFolder: Provider<Directory>)
  fun setSources(rootFolders: FileCollection, graphqlFiles: FileCollection, schemaFile: Provider<RegularFile>, rootPackageName: Provider<String>)
}
