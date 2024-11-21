/*
 *  Copyright (c) 2016-2024 Apollo Graph, Inc. (Formerly Meteor Development Group, Inc.)
 *  Copyright (c) 2015-present, Jim Kynde Meyer
 *  All rights reserved.
 *
 *  This source code is licensed under the MIT license found in the
 *  LICENSE file in the root directory of this source tree.
 */
package com.apollographql.ijplugin.psi;

import java.util.Stack;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.apollographql.ijplugin.psi.ApolloGraphQLElementTypes.*;

%%

%{

    private static final class State {
        final int lBraceCount;
        final int state;

        public State(int state, int lBraceCount) {
            this.state = state;
            this.lBraceCount = lBraceCount;
        }

        @Override
        public String toString() {
            return "yystate = " + state + (lBraceCount == 0 ? "" : "lBraceCount = " + lBraceCount);
        }
    }

    protected final Stack<State> myStateStack = new Stack<State>();
    protected int myLeftBraceCount;

    private void pushState(int state) {
        myStateStack.push(new State(yystate(), myLeftBraceCount));
        myLeftBraceCount = 0;
        yybegin(state);
    }

    private void popState() {
        State state = myStateStack.pop();
        myLeftBraceCount = state.lBraceCount;
        yybegin(state.state);
    }

  public ApolloGraphQLLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class ApolloGraphQLLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

UNICODE_BOM = \uFEFF
WHITESPACE_CHAR = [ \t]
LINE_TERMINATOR = \n | (\r\n?)
WHITESPACE = ({WHITESPACE_CHAR} | {LINE_TERMINATOR})+
EOL_COMMENT = "#" .*
NAME = [_A-Za-z][_0-9A-Za-z]*
VARIABLE = \${NAME}

QUOTED_STRING_ESCAPE= \\[^\r\n]
QUOTED_STRING_BODY = ([^\\\"\r\n] | {QUOTED_STRING_ESCAPE})+

THREE_QUO = (\"\"\")
ONE_TWO_QUO = (\"\"?)
BLOCK_STRING_ESCAPE = (\\({THREE_QUO} | [^\\\"\r\n\t ]))
BLOCK_STRING_CHAR = [^\\\"\r\n\t ]
BLOCK_STRING_BODY = {BLOCK_STRING_CHAR}+

DIGIT = [0-9]
NON_ZERO_DIGIT = [1-9]
INTEGER_PART = -? (0 | {NON_ZERO_DIGIT} {DIGIT}*)
FRACTIONAL_PART = "." {DIGIT}+
EXPONENT_PART = [eE] [+-]? {DIGIT}+

NUMBER = {INTEGER_PART}
FLOAT = {INTEGER_PART} {FRACTIONAL_PART} | {INTEGER_PART} {EXPONENT_PART} | {INTEGER_PART} {FRACTIONAL_PART} {EXPONENT_PART}

%eof{
  myLeftBraceCount = 0;
  myStateStack.clear();
%eof}

%state QUOTED_STRING BLOCK_STRING VARIABLE_OR_TEMPLATE TEMPLATE

%%

<YYINITIAL> {
  // Ignored tokens
  {UNICODE_BOM}      |
  {WHITESPACE}       { return WHITE_SPACE; }
  {EOL_COMMENT}      { return EOL_COMMENT; }
  ","                { return WHITE_SPACE; }

  // Punctuators
  "!"                { return BANG; }
  "$"                { pushState(VARIABLE_OR_TEMPLATE); return DOLLAR; }
  "("                { return PAREN_L; }
  ")"                { return PAREN_R; }
  "..."              { return SPREAD; }
  ":"                { return COLON; }
  "="                { return EQUALS; }
  "@"                { return AT; }
  "["                { return BRACKET_L; }
  "]"                { return BRACKET_R; }
  "{"                { return BRACE_L; }
  "|"                { return PIPE; }
  "}"                { return BRACE_R; }
  "&"                { return AMP; }

  // keywords
  "query"            { return QUERY_KEYWORD; }
  "mutation"         { return MUTATION_KEYWORD; }
  "subscription"     { return SUBSCRIPTION_KEYWORD; }
  "fragment"         { return FRAGMENT_KEYWORD; }
  "on"               { return ON_KEYWORD; }
  "schema"           { return SCHEMA_KEYWORD; }
  "type"             { return TYPE_KEYWORD; }
  "scalar"           { return SCALAR_KEYWORD; }
  "interface"        { return INTERFACE_KEYWORD; }
  "implements"       { return IMPLEMENTS_KEYWORD; }
  "enum"             { return ENUM_KEYWORD; }
  "union"            { return UNION_KEYWORD; }
  "extend"           { return EXTEND_KEYWORD; }
  "input"            { return INPUT_KEYWORD; }
  "directive"        { return DIRECTIVE_KEYWORD; }
  "repeatable"       { return REPEATABLE_KEYWORD; }

  // string and number literals
  \"                 { pushState(QUOTED_STRING); return OPEN_QUOTE;    }
  {THREE_QUO}        { pushState(BLOCK_STRING);  return OPEN_TRIPLE_QUOTE;    }
  {NUMBER}           { return NUMBER; }
  {FLOAT}            { return FLOAT; }

  // identifiers
  {NAME}             { return NAME; }
  {VARIABLE}         { return VARIABLE_NAME; }

  [^]                { return BAD_CHARACTER; }
}

<VARIABLE_OR_TEMPLATE> {
  "{"                { pushState(TEMPLATE); return BRACE_L; }
  {NAME}             { popState(); return NAME; }
  [^]                { popState(); return BAD_CHARACTER; }
}

<QUOTED_STRING> {
    {QUOTED_STRING_BODY}    { return REGULAR_STRING_PART; }
    \"                      { popState(); return CLOSING_QUOTE; }
    [^]                     { popState(); return BAD_CHARACTER; }
}

<BLOCK_STRING> {
    {WHITESPACE}            { return WHITE_SPACE; }
    {BLOCK_STRING_ESCAPE}   { return REGULAR_STRING_PART; }
    {ONE_TWO_QUO} / [^\"]   { return REGULAR_STRING_PART; }
    {BLOCK_STRING_BODY}     { return REGULAR_STRING_PART; }
    {THREE_QUO}             { popState(); return CLOSING_TRIPLE_QUOTE; }
    [^]                     { return REGULAR_STRING_PART; }
}

<TEMPLATE> {
    "{"              { myLeftBraceCount++; return TEMPLATE_CHAR; }
    "}"              { if (myLeftBraceCount == 0) { popState(); popState(); return BRACE_R; } myLeftBraceCount--; return TEMPLATE_CHAR; }
   [^\{\}]+          { return TEMPLATE_CHAR; }
}
