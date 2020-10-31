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
      definitions = definitions + GQLDocument(buildInsInputStream).definitions
  )
}

operator fun GQLDocument.Companion.invoke(document: String) = GQLDocument(document.byteInputStream())

operator fun GQLDocument.Companion.invoke(file: File) = file.inputStream().use {
  GQLDocument(it)
}

operator fun GQLDocument.Companion.invoke(inputStream: InputStream): GQLDocument {
  return GraphQLParser(
      CommonTokenStream(
          GraphQLLexer(
              CharStreams.fromStream(inputStream)
          )
      )
  ).document().parse()
}