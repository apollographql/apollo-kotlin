package com.apollographql.ijplugin.psi

import com.intellij.psi.tree.IElementType

object ApolloGraphQLElementTypes {
  @JvmField
  val AMP: IElementType = ApolloGraphQLTokenType("&")

  @JvmField
  val AT: IElementType = ApolloGraphQLTokenType("@")

  @JvmField
  val BANG: IElementType = ApolloGraphQLTokenType("!")

  @JvmField
  val BRACE_L: IElementType = ApolloGraphQLTokenType("{")

  @JvmField
  val BRACE_R: IElementType = ApolloGraphQLTokenType("}")

  @JvmField
  val BRACKET_L: IElementType = ApolloGraphQLTokenType("[")

  @JvmField
  val BRACKET_R: IElementType = ApolloGraphQLTokenType("]")

  @JvmField
  val CLOSING_QUOTE: IElementType = ApolloGraphQLTokenType("CLOSING_QUOTE")

  @JvmField
  val CLOSING_TRIPLE_QUOTE: IElementType = ApolloGraphQLTokenType("CLOSING_TRIPLE_QUOTE")

  @JvmField
  val COLON: IElementType = ApolloGraphQLTokenType(":")

  @JvmField
  val DIRECTIVE_KEYWORD: IElementType = ApolloGraphQLTokenType("directive")

  @JvmField
  val DOLLAR: IElementType = ApolloGraphQLTokenType("$")

  @JvmField
  val ENUM_KEYWORD: IElementType = ApolloGraphQLTokenType("enum")

  @JvmField
  val EOL_COMMENT: IElementType = ApolloGraphQLTokenType("EOL_COMMENT")

  @JvmField
  val EQUALS: IElementType = ApolloGraphQLTokenType("=")

  @JvmField
  val EXTEND_KEYWORD: IElementType = ApolloGraphQLTokenType("extend")

  @JvmField
  val FLOAT: IElementType = ApolloGraphQLTokenType("FLOAT")

  @JvmField
  val FRAGMENT_KEYWORD: IElementType = ApolloGraphQLTokenType("fragment")

  @JvmField
  val IMPLEMENTS_KEYWORD: IElementType = ApolloGraphQLTokenType("implements")

  @JvmField
  val INPUT_KEYWORD: IElementType = ApolloGraphQLTokenType("input")

  @JvmField
  val INTERFACE_KEYWORD: IElementType = ApolloGraphQLTokenType("interface")

  @JvmField
  val MUTATION_KEYWORD: IElementType = ApolloGraphQLTokenType("mutation")

  @JvmField
  val NAME: IElementType = ApolloGraphQLTokenType("NAME")

  @JvmField
  val NUMBER: IElementType = ApolloGraphQLTokenType("NUMBER")

  @JvmField
  val ON_KEYWORD: IElementType = ApolloGraphQLTokenType("on")

  @JvmField
  val OPEN_QUOTE: IElementType = ApolloGraphQLTokenType("OPEN_QUOTE")

  @JvmField
  val OPEN_TRIPLE_QUOTE: IElementType = ApolloGraphQLTokenType("OPEN_TRIPLE_QUOTE")

  @JvmField
  val PAREN_L: IElementType = ApolloGraphQLTokenType("(")

  @JvmField
  val PAREN_R: IElementType = ApolloGraphQLTokenType(")")

  @JvmField
  val PIPE: IElementType = ApolloGraphQLTokenType("|")

  @JvmField
  val QUERY_KEYWORD: IElementType = ApolloGraphQLTokenType("query")

  @JvmField
  val REGULAR_STRING_PART: IElementType = ApolloGraphQLTokenType("REGULAR_STRING_PART")

  @JvmField
  val REPEATABLE_KEYWORD: IElementType = ApolloGraphQLTokenType("repeatable")

  @JvmField
  val SCALAR_KEYWORD: IElementType = ApolloGraphQLTokenType("scalar")

  @JvmField
  val SCHEMA_KEYWORD: IElementType = ApolloGraphQLTokenType("schema")

  @JvmField
  val SPREAD: IElementType = ApolloGraphQLTokenType("...")

  @JvmField
  val SUBSCRIPTION_KEYWORD: IElementType = ApolloGraphQLTokenType("subscription")

  @JvmField
  val TEMPLATE_CHAR: IElementType = ApolloGraphQLTokenType("TEMPLATE_CHAR")

  @JvmField
  val TYPE_KEYWORD: IElementType = ApolloGraphQLTokenType("type")

  @JvmField
  val UNION_KEYWORD: IElementType = ApolloGraphQLTokenType("union")

  @JvmField
  val VARIABLE_NAME: IElementType = ApolloGraphQLTokenType("VARIABLE_NAME")
}
