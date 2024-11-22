/*
 * Copyright (c) 2016-2024 Apollo Graph, Inc. (Formerly Meteor Development Group, Inc.)
 * Copyright (c) 2018-present, Jim Kynde Meyer
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.apollographql.ijplugin.highlighting

import com.apollographql.ijplugin.psi.ApolloGraphQLElementTypes
import com.apollographql.ijplugin.psi.ApolloGraphQLExtendedElementTypes
import com.apollographql.ijplugin.psi.ApolloGraphQLLexer
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class ApolloGraphQLSyntaxHighlighter : SyntaxHighlighterBase() {
  override fun getHighlightingLexer(): Lexer {
    return ApolloGraphQLLexerAdapter()
  }

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
    return when {
      tokenType == ApolloGraphQLElementTypes.NAME -> {
        IDENTIFIER_KEYS
      }

      ApolloGraphQLExtendedElementTypes.KEYWORDS.contains(tokenType) -> {
        KEYWORD_KEYS
      }

      ApolloGraphQLExtendedElementTypes.NUMBER_LITERALS.contains(tokenType) -> {
        NUMBER_KEYS
      }

      ApolloGraphQLExtendedElementTypes.STRING_TOKENS.contains(tokenType) -> {
        STRING_KEYS
      }

      tokenType == ApolloGraphQLElementTypes.EOL_COMMENT -> {
        COMMENT_KEYS
      }

      tokenType == ApolloGraphQLElementTypes.BRACE_L || tokenType == ApolloGraphQLElementTypes.BRACE_R -> {
        BRACES_KEYS
      }

      tokenType == ApolloGraphQLElementTypes.PAREN_L || tokenType == ApolloGraphQLElementTypes.PAREN_R -> {
        PARENTHESES_KEYS
      }

      tokenType == ApolloGraphQLElementTypes.BRACKET_L || tokenType == ApolloGraphQLElementTypes.BRACKET_R -> {
        BRACKETS_KEYS
      }

      tokenType == ApolloGraphQLElementTypes.SPREAD -> {
        SPREAD_KEYS
      }

      tokenType == ApolloGraphQLElementTypes.COLON -> {
        COLON_KEYS
      }

      tokenType == ApolloGraphQLElementTypes.BANG -> {
        BANG_KEYS
      }

      tokenType == ApolloGraphQLElementTypes.PIPE -> {
        PIPE_KEYS
      }

      tokenType == ApolloGraphQLElementTypes.AMP -> {
        AMP_KEYS
      }

      tokenType == TokenType.BAD_CHARACTER -> {
        BAD_CHARACTER_KEYS
      }

      else -> {
        EMPTY_KEYS
      }
    }
  }

  companion object {
    val IDENTIFIER: TextAttributesKey =
      TextAttributesKey.createTextAttributesKey("GRAPHQL_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
    val KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("GRAPHQL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
    val NUMBER: TextAttributesKey = TextAttributesKey.createTextAttributesKey("GRAPHQL_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    val STRING: TextAttributesKey = TextAttributesKey.createTextAttributesKey("GRAPHQL_STRING", DefaultLanguageHighlighterColors.STRING)
    val COMMENT: TextAttributesKey =
      TextAttributesKey.createTextAttributesKey("GRAPHQL_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    val BRACES: TextAttributesKey = TextAttributesKey.createTextAttributesKey("GRAPHQL_BRACES", DefaultLanguageHighlighterColors.BRACES)
    val PARENTHESES: TextAttributesKey =
      TextAttributesKey.createTextAttributesKey("GRAPHQL_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
    val BRACKETS: TextAttributesKey =
      TextAttributesKey.createTextAttributesKey("GRAPHQL_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
    val SPREAD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("GRAPHQL_SPREAD", DefaultLanguageHighlighterColors.SEMICOLON)
    val COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey("GRAPHQL_COLON", DefaultLanguageHighlighterColors.SEMICOLON)
    val BANG: TextAttributesKey = TextAttributesKey.createTextAttributesKey("GRAPHQL_BANG", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    val PIPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("GRAPHQL_PIPE", DefaultLanguageHighlighterColors.SEMICOLON)
    val AMP: TextAttributesKey = TextAttributesKey.createTextAttributesKey("GRAPHQL_AMP", DefaultLanguageHighlighterColors.SEMICOLON)
    val BAD_CHARACTER: TextAttributesKey =
      TextAttributesKey.createTextAttributesKey("GRAPHQL_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

    private val IDENTIFIER_KEYS = arrayOf(IDENTIFIER)
    private val KEYWORD_KEYS = arrayOf(KEYWORD)
    private val NUMBER_KEYS = arrayOf(NUMBER)
    private val STRING_KEYS = arrayOf(STRING)
    private val COMMENT_KEYS = arrayOf(COMMENT)
    private val BRACES_KEYS = arrayOf(BRACES)
    private val PARENTHESES_KEYS = arrayOf(PARENTHESES)
    private val BRACKETS_KEYS = arrayOf(BRACKETS)
    private val SPREAD_KEYS = arrayOf(SPREAD)
    private val COLON_KEYS = arrayOf(COLON)
    private val BANG_KEYS = arrayOf(BANG)
    private val PIPE_KEYS = arrayOf(PIPE)
    private val AMP_KEYS = arrayOf(AMP)
    private val BAD_CHARACTER_KEYS = arrayOf(BAD_CHARACTER)
    private val EMPTY_KEYS: Array<TextAttributesKey> = TextAttributesKey.EMPTY_ARRAY
  }
}

private class ApolloGraphQLLexerAdapter : FlexAdapter(ApolloGraphQLLexer(null))
