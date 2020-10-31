package com.apollographql.apollo.compiler.parser.graphql.ast

import com.apollographql.apollo.compiler.ir.SourceLocation

interface GQLNode {
  val sourceLocation: SourceLocation
}

interface GQLNamed {
  val name: String
}

interface GQLDefinition : GQLNode
interface GQLTypeSystemExtension : GQLNode
interface GQLTypeExtension : GQLTypeSystemExtension, GQLNamed

interface GQLSelection : GQLNode

data class GQLDocument(override val sourceLocation: SourceLocation, val definitions: List<GQLDefinition>) : GQLNode

data class GQLOperationDefinition(override val sourceLocation: SourceLocation, val name: String?, val variableDefinitions: List<GQLVariableDefinition>, val directives: List<GQLDirective>, val selections: List<GQLSelection>) : GQLDefinition
data class GQLFragmentDefinition(override val sourceLocation: SourceLocation, val name: String, val directives: List<GQLDirective>, val typeCondition: GQLNamedType, val selections: List<GQLSelection>?) : GQLDefinition
data class GQLSchemaDefinition(override val sourceLocation: SourceLocation, val description: String, val directives: List<GQLDirective>, val rootOperationTypeDefinitions: List<GQLOperationTypeDefinition>) : GQLDefinition

sealed class GQLTypeDefinition: GQLDefinition, GQLNamed
data class GQLInterfaceTypeDefinition(override val sourceLocation: SourceLocation, val description: String, override val name: String, val implementsInterfaces: List<String>, val fields: List<GQLFieldDefinition>) : GQLTypeDefinition()
data class GQLObjectTypeDefinition(override val sourceLocation: SourceLocation, val description: String, override val name: String, val directives: List<GQLDirective>, val fields: List<GQLFieldDefinition>, val implementsInterfaces: List<String>) : GQLTypeDefinition()
data class GQLInputObjectTypeDefinition(override val sourceLocation: SourceLocation, val description: String, override val name: String, val directives: List<GQLDirective>, val inputFields: List<GQLInputValueDefinition>) : GQLTypeDefinition()
data class GQLScalarTypeDefinition(override val sourceLocation: SourceLocation, val description: String, override val name: String, val directives: List<GQLDirective>) : GQLTypeDefinition()
data class GQLEnumTypeDefinition(override val sourceLocation: SourceLocation, val description: String, override val name: String, val directives: List<GQLDirective>, val enumValues: List<GQLEnumValueDefinition>) : GQLTypeDefinition()
data class GQLUnionTypeDefinition(override val sourceLocation: SourceLocation, val description: String, override val name: String, val directives: List<GQLDirective>, val memberTypes: List<GQLNamedType>) : GQLTypeDefinition()
data class GQLDirectiveDefinition(override val sourceLocation: SourceLocation, val description: String, val name: String, val arguments: List<GQLInputValueDefinition>, val repeatable: Boolean) : GQLDefinition
data class GQLSchemaExtension(override val sourceLocation: SourceLocation, val directives: List<GQLDirective>, val operationTypesDefinition: List<GQLOperationTypeDefinition>) : GQLDefinition, GQLTypeSystemExtension
data class GQLEnumTypeExtension(override val sourceLocation: SourceLocation, override val name: String, val directives: List<GQLDirective>, val enumValues: List<GQLEnumValueDefinition>) : GQLDefinition, GQLTypeExtension
data class GQLObjectTypeExtension(override val sourceLocation: SourceLocation, override val name: String, val directives: List<GQLDirective>, val fields: List<GQLFieldDefinition>) : GQLDefinition, GQLTypeExtension
data class GQLInputObjectTypeExtension(override val sourceLocation: SourceLocation, override val name: String, val directives: List<GQLDirective>, val inputFields: List<GQLInputValueDefinition>) : GQLDefinition, GQLTypeExtension
data class GQLScalarTypeExtension(override val sourceLocation: SourceLocation, override val name: String, val directives: List<GQLDirective>) : GQLDefinition, GQLTypeExtension
data class GQLInterfaceTypeExtension(override val sourceLocation: SourceLocation, override val name: String, val fields: List<GQLFieldDefinition>) : GQLDefinition, GQLTypeExtension, GQLNamed
data class GQLUnionTypeExtension(override val sourceLocation: SourceLocation, override val name: String, val directives: List<GQLDirective>, val memberTypes: List<GQLNamedType>) : GQLDefinition, GQLTypeExtension

data class GQLEnumValueDefinition(override val sourceLocation: SourceLocation, val description: String, override val name: String, val directives: List<GQLDirective>) : GQLNode, GQLNamed
data class GQLFieldDefinition(override val sourceLocation: SourceLocation, val description: String, override val name: String, val arguments: List<GQLInputValueDefinition>, val type: GQLType, val directives: List<GQLDirective>) : GQLNode, GQLNamed
data class GQLInputValueDefinition(override val sourceLocation: SourceLocation, val description: String, override val name: String, val directives: List<GQLDirective>, val type: GQLType, val defaultValue: GQLValue?) : GQLNode, GQLNamed
data class GQLVariableDefinition(override val sourceLocation: SourceLocation, val name: String, val type: GQLType, val defaultValue: GQLValue) : GQLNode
data class GQLOperationTypeDefinition(override val sourceLocation: SourceLocation, val operationType: String, val namedType: String) : GQLNode
data class GQLDirective(override val sourceLocation: SourceLocation, override val name: String, val arguments: List<GQLArgument>) : GQLNode, GQLNamed
data class GQLObjectField(override val sourceLocation: SourceLocation, val name: String, val value: GQLValue) : GQLNode
data class GQLArgument(override val sourceLocation: SourceLocation, val name: String, val value: GQLValue) : GQLNode

data class GQLField(override val sourceLocation: SourceLocation, val alias: String?, val name: String, val arguments: List<GQLArgument>, val directives: List<GQLDirective>, val selections: List<GQLSelection>) : GQLSelection
data class GQLInlineFragment(override val sourceLocation: SourceLocation, val typeCondition: GQLNamedType, val directives: List<GQLDirective>, val selectionSet: List<GQLSelection>) : GQLSelection
data class GQLFragmentSpread(override val sourceLocation: SourceLocation, val name: String, val directives: List<GQLDirective>) : GQLSelection

sealed class GQLType : GQLNode
data class GQLNamedType(override val sourceLocation: SourceLocation, override val name: String) : GQLType(), GQLNamed
data class GQLNonNullType(override val sourceLocation: SourceLocation, val type: GQLType) : GQLType()
data class GQLListType(override val sourceLocation: SourceLocation, val type: GQLType) : GQLType()


sealed class GQLValue : GQLNode
data class GQLVariableValue(override val sourceLocation: SourceLocation, val name: String) : GQLValue()
data class GQLIntValue(override val sourceLocation: SourceLocation, val value: Int) : GQLValue()
data class GQLFloatValue(override val sourceLocation: SourceLocation, val value: Double) : GQLValue()
data class GQLStringValue(override val sourceLocation: SourceLocation, val value: String) : GQLValue()
data class GQLBooleanValue(override val sourceLocation: SourceLocation, val value: Boolean) : GQLValue()
data class GQLEnumValue(override val sourceLocation: SourceLocation, val value: String) : GQLValue()
data class GQLListValue(override val sourceLocation: SourceLocation, val values: List<GQLValue>) : GQLValue()
data class GQLObjectValue(override val sourceLocation: SourceLocation, val fields: List<GQLObjectField>) : GQLValue()
data class GQLNullValue(override val sourceLocation: SourceLocation) : GQLValue()

