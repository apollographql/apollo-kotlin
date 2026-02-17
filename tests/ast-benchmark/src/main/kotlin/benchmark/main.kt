package benchmark

import com.apollographql.apollo.ast.ParserOptions
import com.apollographql.apollo.ast.parseAsGQLDocument
import okio.buffer
import okio.source
import java.io.File
import java.net.URI
import java.net.URL
import graphql.parser.Parser as GraphQLJavaParser

fun findTestFiles(): List<File>  {
  return File(".").resolve("../../libraries/apollo-compiler/src/test/graphql")
      .walk()
      .filter { it.extension in setOf("graphql", "graphqls") }
      .filter {
        when (it.parentFile.name) {
          "empty", "__schema" -> false // contains empty document which are not spec compliant
          "simple_fragment" -> false // contains operation/fragment descriptions which are not spec compliant
          "antlr_tokens" -> false // contains operation descriptions https://github.com/graphql-java/graphql-java/issues/4077
          else -> true
        }
      }
      .toList()
}

fun parseWithGraphQLJava(testFiles: List<File>): Double {
  return testFiles.sumOf {
    GraphQLJavaParser.parse(it.readText()).definitions.size.toDouble()
  }
}

fun parseWithApollo(testFiles: List<File>): Double {
  return testFiles.sumOf {
    it.source().buffer().parseAsGQLDocument(options = ParserOptions.Builder().build()).getOrThrow().definitions.size.toDouble()
  }
}

/**
 * This is downloaded to avoid storing a 6MB file in the repo.
 * The test file is from graphql-java: https://raw.githubusercontent.com/graphql-java/graphql-java/70acb6cd1da81e7fdead317123c2dd4dd2461407/src/test/resources/large-schema-4.graphqls
 */
val largeSchema = URI("https://storage.googleapis.com/apollo-kotlin-files/large-schema-4.graphqls").toURL()
