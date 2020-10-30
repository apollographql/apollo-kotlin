package com.apollographql.apollo.compiler.parser.graphql.ast

import com.apollographql.apollo.compiler.parser.antlr.GraphQLParser

data class GQLDocument(val definitions: List<GQLDefinition>)

interface GQLDefinition
data class GQLSchemaDefinition(val description: String, val directives: List<GQLDirective>, val rootOperationTypeDefinitions: List<GQLOperationTypeDefinition>) : GQLDefinition
data class GQLOperationDefinition(val name: String?, val variableDefinitions: List<GQLVariableDefinition>, val directives: List<GQLDirective>, val selections: List<GQLSelection>): GQLDefinition
data class GQLInterfaceTypeDefinition(val description: String, val name: String, val implementsInterfaces: List<String>, val fields: List<GQLFieldDefinition>) : GQLDefinition
data class GQLObjectTypeDefinition(val description: String, val name: String, val directives: List<GQLDirective>, val fields: List<GQLFieldDefinition>, val implementsInterfaces: List<String>) : GQLDefinition
data class GQLInputObjectTypeDefinition(val description: String, val name: String, val directives: List<GQLDirective>, val inputFields: List<GQLInputValueDefinition>) : GQLDefinition
data class GQLScalarTypeDefinition(val description: String, val name: String, val directives: List<GQLDirective>) : GQLDefinition
data class GQLEnumTypeDefinition(val description: String, val name: String, val directives: List<GQLDirective>, val enumValues: List<GQLEnumValueDefinition>) : GQLDefinition
data class GQLUnionTypeDefinition(val description: String, val name: String, val directives: List<GQLDirective>, val memberTypes: List<String>?) : GQLDefinition
data class GQLDirectiveDefinition(val description: String, val name: String, val arguments: List<GQLInputValueDefinition>) : GQLDefinition
data class GQLSchemaExtension(val directives: List<GQLDirective>, val operationTypesDefinition: List<GQLOperationTypeDefinition>) : GQLDefinition
data class GQLEnumTypeExtension(val name: String, val directives: List<GQLDirective>, val enumValues: List<GQLEnumValueDefinition>) : GQLDefinition
data class GQLObjectTypeExtension(val name: String, val directives: List<GQLDirective>, val fields: List<GQLFieldDefinition>) : GQLDefinition
data class GQLInputObjectTypeExtension(val name: String, val directives: List<GQLDirective>, val inputFields: List<GQLInputValueDefinition>) : GQLDefinition
data class GQLScalarTypeExtension(val name: GraphQLParser.NameContext, val directives: List<GQLDirective>) : GQLDefinition
data class GQLInterfaceTypeExtension(val name: String, val fields: List<GQLFieldDefinition>) : GQLDefinition
data class GQLUnionTypeExtension(val name: String, val directives: List<GQLDirective>, val memberTypes: List<String>?) : GQLDefinition

data class GQLEnumValueDefinition(val description: String, val name: String, val directives: List<GQLDirective>)
data class GQLFieldDefinition(val description: String, val name: String, val arguments: List<GQLInputValueDefinition>, val type: GQLType, val directives: List<GQLDirective>)
data class GQLInputValueDefinition(val description: String, val name: String, val directives: List<GQLDirective>, val type: GQLType, val defaultValue: GQLValue?)
data class GQLVariableDefinition(val name: String, val type: GQLType, val defaultValue: GQLValue)
data class GQLOperationTypeDefinition(val operationType: String, val namedType: String)
data class GQLDirective(val name: String, val arguments: List<GQLArgument>)

interface GQLSelection
data class GQLField(val alias: String?, val name: String, val arguments: List<GQLArgument>, val directives: List<GQLDirective>, val selections: List<GQLSelection>) : GQLSelection
data class GQLInlineFragment(val typeCondition: String?, val directives: List<GQLDirective>, val selectionSet: List<GQLSelection>) : GQLSelection
data class GQLFragmentSpread(val name: String, val directives: List<GQLDirective>) : GQLSelection

interface GQLType
data class GQLNamedType(val name: String): GQLType
data class GQLNonNullType(val type: GQLType): GQLType
data class GQLListType(val type: GQLType): GQLType

interface GQLValue
data class GQLVariableValue(val name: String): GQLValue
data class GQLIntValue(val value: Int): GQLValue
data class GQLFloatValue(val value: Double): GQLValue
data class GQLStringValue(val value: String): GQLValue
data class GQLBooleanValue(val value: Boolean): GQLValue
data class GQLEnumValue(val value: String): GQLValue
data class GQLListValue(val values: List<GQLValue>): GQLValue
data class GQLObjectValue(val fields: List<GQLObjectField>): GQLValue
object GQLNullValue: GQLValue

data class GQLObjectField(val name: String, val value: GQLValue)
data class GQLArgument(val name: String, val value: GQLValue)