// Generated from com/apollographql/apollo/compiler/parser/antlr/GraphQL.g4 by ANTLR 4.7.2

package com.apollographql.apollo.compiler.parser.antlr;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link GraphQLParser}.
 */
public interface GraphQLListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#document}.
	 * @param ctx the parse tree
	 */
	void enterDocument(GraphQLParser.DocumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#document}.
	 * @param ctx the parse tree
	 */
	void exitDocument(GraphQLParser.DocumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#definition}.
	 * @param ctx the parse tree
	 */
	void enterDefinition(GraphQLParser.DefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#definition}.
	 * @param ctx the parse tree
	 */
	void exitDefinition(GraphQLParser.DefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#operationDefinition}.
	 * @param ctx the parse tree
	 */
	void enterOperationDefinition(GraphQLParser.OperationDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#operationDefinition}.
	 * @param ctx the parse tree
	 */
	void exitOperationDefinition(GraphQLParser.OperationDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#selectionSet}.
	 * @param ctx the parse tree
	 */
	void enterSelectionSet(GraphQLParser.SelectionSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#selectionSet}.
	 * @param ctx the parse tree
	 */
	void exitSelectionSet(GraphQLParser.SelectionSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#operationType}.
	 * @param ctx the parse tree
	 */
	void enterOperationType(GraphQLParser.OperationTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#operationType}.
	 * @param ctx the parse tree
	 */
	void exitOperationType(GraphQLParser.OperationTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#selection}.
	 * @param ctx the parse tree
	 */
	void enterSelection(GraphQLParser.SelectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#selection}.
	 * @param ctx the parse tree
	 */
	void exitSelection(GraphQLParser.SelectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#field}.
	 * @param ctx the parse tree
	 */
	void enterField(GraphQLParser.FieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#field}.
	 * @param ctx the parse tree
	 */
	void exitField(GraphQLParser.FieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#fieldName}.
	 * @param ctx the parse tree
	 */
	void enterFieldName(GraphQLParser.FieldNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#fieldName}.
	 * @param ctx the parse tree
	 */
	void exitFieldName(GraphQLParser.FieldNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#alias}.
	 * @param ctx the parse tree
	 */
	void enterAlias(GraphQLParser.AliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#alias}.
	 * @param ctx the parse tree
	 */
	void exitAlias(GraphQLParser.AliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(GraphQLParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(GraphQLParser.ArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(GraphQLParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(GraphQLParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#fragmentSpread}.
	 * @param ctx the parse tree
	 */
	void enterFragmentSpread(GraphQLParser.FragmentSpreadContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#fragmentSpread}.
	 * @param ctx the parse tree
	 */
	void exitFragmentSpread(GraphQLParser.FragmentSpreadContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#inlineFragment}.
	 * @param ctx the parse tree
	 */
	void enterInlineFragment(GraphQLParser.InlineFragmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#inlineFragment}.
	 * @param ctx the parse tree
	 */
	void exitInlineFragment(GraphQLParser.InlineFragmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#fragmentDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFragmentDefinition(GraphQLParser.FragmentDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#fragmentDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFragmentDefinition(GraphQLParser.FragmentDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#fragmentName}.
	 * @param ctx the parse tree
	 */
	void enterFragmentName(GraphQLParser.FragmentNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#fragmentName}.
	 * @param ctx the parse tree
	 */
	void exitFragmentName(GraphQLParser.FragmentNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#directives}.
	 * @param ctx the parse tree
	 */
	void enterDirectives(GraphQLParser.DirectivesContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#directives}.
	 * @param ctx the parse tree
	 */
	void exitDirectives(GraphQLParser.DirectivesContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterDirective(GraphQLParser.DirectiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitDirective(GraphQLParser.DirectiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#typeCondition}.
	 * @param ctx the parse tree
	 */
	void enterTypeCondition(GraphQLParser.TypeConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#typeCondition}.
	 * @param ctx the parse tree
	 */
	void exitTypeCondition(GraphQLParser.TypeConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#variableDefinitions}.
	 * @param ctx the parse tree
	 */
	void enterVariableDefinitions(GraphQLParser.VariableDefinitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#variableDefinitions}.
	 * @param ctx the parse tree
	 */
	void exitVariableDefinitions(GraphQLParser.VariableDefinitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#variableDefinition}.
	 * @param ctx the parse tree
	 */
	void enterVariableDefinition(GraphQLParser.VariableDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#variableDefinition}.
	 * @param ctx the parse tree
	 */
	void exitVariableDefinition(GraphQLParser.VariableDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#variable}.
	 * @param ctx the parse tree
	 */
	void enterVariable(GraphQLParser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#variable}.
	 * @param ctx the parse tree
	 */
	void exitVariable(GraphQLParser.VariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(GraphQLParser.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(GraphQLParser.DefaultValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#valueOrVariable}.
	 * @param ctx the parse tree
	 */
	void enterValueOrVariable(GraphQLParser.ValueOrVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#valueOrVariable}.
	 * @param ctx the parse tree
	 */
	void exitValueOrVariable(GraphQLParser.ValueOrVariableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringValue}
	 * labeled alternative in {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 */
	void enterStringValue(GraphQLParser.StringValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringValue}
	 * labeled alternative in {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 */
	void exitStringValue(GraphQLParser.StringValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code numberValue}
	 * labeled alternative in {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 */
	void enterNumberValue(GraphQLParser.NumberValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code numberValue}
	 * labeled alternative in {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 */
	void exitNumberValue(GraphQLParser.NumberValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanValue}
	 * labeled alternative in {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 */
	void enterBooleanValue(GraphQLParser.BooleanValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanValue}
	 * labeled alternative in {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 */
	void exitBooleanValue(GraphQLParser.BooleanValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arrayValue}
	 * labeled alternative in {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 */
	void enterArrayValue(GraphQLParser.ArrayValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arrayValue}
	 * labeled alternative in {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 */
	void exitArrayValue(GraphQLParser.ArrayValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code literalValue}
	 * labeled alternative in {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 */
	void enterLiteralValue(GraphQLParser.LiteralValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code literalValue}
	 * labeled alternative in {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 */
	void exitLiteralValue(GraphQLParser.LiteralValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inlineInputTypeValue}
	 * labeled alternative in {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 */
	void enterInlineInputTypeValue(GraphQLParser.InlineInputTypeValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inlineInputTypeValue}
	 * labeled alternative in {@link GraphQLParser#value}.
	 * @param ctx the parse tree
	 */
	void exitInlineInputTypeValue(GraphQLParser.InlineInputTypeValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#inlineInputType}.
	 * @param ctx the parse tree
	 */
	void enterInlineInputType(GraphQLParser.InlineInputTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#inlineInputType}.
	 * @param ctx the parse tree
	 */
	void exitInlineInputType(GraphQLParser.InlineInputTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#inlineInputTypeField}.
	 * @param ctx the parse tree
	 */
	void enterInlineInputTypeField(GraphQLParser.InlineInputTypeFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#inlineInputTypeField}.
	 * @param ctx the parse tree
	 */
	void exitInlineInputTypeField(GraphQLParser.InlineInputTypeFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(GraphQLParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(GraphQLParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#typeName}.
	 * @param ctx the parse tree
	 */
	void enterTypeName(GraphQLParser.TypeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#typeName}.
	 * @param ctx the parse tree
	 */
	void exitTypeName(GraphQLParser.TypeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#listType}.
	 * @param ctx the parse tree
	 */
	void enterListType(GraphQLParser.ListTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#listType}.
	 * @param ctx the parse tree
	 */
	void exitListType(GraphQLParser.ListTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#nonNullType}.
	 * @param ctx the parse tree
	 */
	void enterNonNullType(GraphQLParser.NonNullTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#nonNullType}.
	 * @param ctx the parse tree
	 */
	void exitNonNullType(GraphQLParser.NonNullTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link GraphQLParser#array}.
	 * @param ctx the parse tree
	 */
	void enterArray(GraphQLParser.ArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link GraphQLParser#array}.
	 * @param ctx the parse tree
	 */
	void exitArray(GraphQLParser.ArrayContext ctx);
}