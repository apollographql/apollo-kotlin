package com.apollographql.apollo.compiler.parser.graphql.ast

import com.apollographql.apollo.compiler.parser.antlr.GraphQLLexer
import com.apollographql.apollo.compiler.parser.antlr.GraphQLParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File
import java.io.InputStream

fun GQLDocument.withBuiltinTypes(): GQLDocument {
  val buildInsInputStream = javaClass.getResourceAsStream("/builtins.sdl")
  return copy(
      definitions = definitions + GQLDocument.fromInputStream(buildInsInputStream, false).definitions
  )
}

fun GQLDocument.Companion.fromString(document: String) = GQLDocument.fromInputStream(document.byteInputStream())

fun GQLDocument.Companion.fromFile(file: File) = file.inputStream().use {
  GQLDocument.fromInputStream(it)
}

fun GQLDocument.Companion.fromInputStream(inputStream: InputStream, addBuiltinTypes: Boolean = true): GQLDocument {
  return GraphQLParser(
      CommonTokenStream(
          GraphQLLexer(
              CharStreams.fromStream(inputStream)
          )
      )
  ).document()
      .parse()
      .let {
        if (addBuiltinTypes) {
          it.withBuiltinTypes()
        } else {
          it
        }
      }

}