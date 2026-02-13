package benchmark

import com.apollographql.apollo.ast.ParserOptions
import com.apollographql.apollo.ast.parseAsGQLDocument
import okio.buffer
import okio.source
import java.io.File
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

