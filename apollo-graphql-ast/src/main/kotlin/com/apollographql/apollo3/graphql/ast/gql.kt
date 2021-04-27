/**
 * The GraphQL AST definition
 *
 * This is all in one file so we can use sealed classes. Extensions are in gqlxyz.kt
 */
package com.apollographql.apollo3.graphql.ast

import okio.BufferedSink

/**
 * A node in the GraphQL AST.
 *
 * The structure of the different nodes matches closely the one of the GraphQL specification
 * (https://spec.graphql.org/June2018/#sec-Appendix-Grammar-Summary.Document)
 *
 * See [GraphQLParser] for the different ways to get a [GQLDocument].
 *
 * Compared to the Antlr [com.apollographql.apollo3.compiler.parser.antlr.GraphQLParser.DocumentContext], a GQLDocument
 * is a lot simpler and allows for easy modifying a document (using .clone()) and outputing them to a [okio.BufferedSink].
 *
 * Whitespace tokens are not mapped to GQLNodes so some formatting will be lost during modification
 */
interface GQLNode {
  val sourceLocation: SourceLocation

  /**
   * The children of this node.
   *
   * Terminal nodes won't have any children.
   */
  val children: List<GQLNode>

  fun write(bufferedSink: BufferedSink)

  fun copyWithNewChildren(container: NodeContainer): GQLNode
}


interface GQLNamed {
  val name: String
}

interface GQLDescribed {
  val description: String?
}

interface GQLDefinition : GQLNode
interface GQLTypeSystemExtension : GQLNode
interface GQLTypeExtension : GQLTypeSystemExtension, GQLNamed

sealed class GQLSelection : GQLNode

/**
 * The top level node in a GraphQL document. This can be a schema document or an executable document
 * (or something else if need be)
 *
 * See [GraphQLParser] for the different ways to get a [GQLDocument].
 */
data class GQLDocument(
    val definitions: List<GQLDefinition>,
    val filePath: String?,
) : GQLNode {
  override val sourceLocation: SourceLocation = SourceLocation(0, 0, filePath)
  override val children = definitions

  override fun write(bufferedSink: BufferedSink) {
    definitions.join(bufferedSink = bufferedSink, separator = "\n")
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return GQLDocument(
        definitions = container.take(),
        filePath = filePath
    )
  }

  companion object
}

data class GQLOperationDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val operationType: String,
    val name: String?,
    val variableDefinitions: List<GQLVariableDefinition>,
    val directives: List<GQLDirective>,
    val selectionSet: GQLSelectionSet,
    override val description: String?,
) : GQLDefinition, GQLDescribed {
  override val children = variableDefinitions + directives + selectionSet

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8(operationType)
      if (name != null) {
        writeUtf8(" ")
        writeUtf8(name)
        if (variableDefinitions.isNotEmpty()) {
          variableDefinitions.join(bufferedSink = bufferedSink, separator = ", ", prefix = "(", postfix = ")")
        }
      }
      if (directives.isNotEmpty()) {
        writeUtf8(" ")
        directives.join(bufferedSink)
      }
      if (selectionSet.selections.isNotEmpty()) {
        writeUtf8(" ")
        selectionSet.write(bufferedSink)
      }
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        variableDefinitions = container.take(),
        directives = container.take(),
        selectionSet = container.take<GQLSelectionSet>().single(),
    )
  }
}

data class GQLFragmentDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val directives: List<GQLDirective>,
    val typeCondition: GQLNamedType,
    val selectionSet: GQLSelectionSet,
    override val description: String?,
) : GQLDefinition, GQLNamed, GQLDescribed {

  override val children = directives + selectionSet + typeCondition

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("fragment $name on ${typeCondition.name}")
      if (directives.isNotEmpty()) {
        writeUtf8(" ")
        directives.join(bufferedSink)
      }
      if (selectionSet.selections.isNotEmpty()) {
        writeUtf8(" ")
        selectionSet.write(bufferedSink)
      }
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        typeCondition = container.take<GQLNamedType>().single(),
        selectionSet = container.take<GQLSelectionSet>().single(),
    )
  }
}

data class GQLSchemaDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    val directives: List<GQLDirective>,
    val rootOperationTypeDefinitions: List<GQLOperationTypeDefinition>,
) : GQLDefinition {

  override val children = directives + rootOperationTypeDefinitions

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"${GraphQLString.encodeTripleQuoted(description)}\"\"\"\n")
      if (directives.isNotEmpty()) {
        directives.join(bufferedSink)
        writeUtf8(" ")
      }
      writeUtf8("schema ")
      rootOperationTypeDefinitions.join(bufferedSink, prefix = "{\n", separator = "\n", postfix = "\n}\n")
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        rootOperationTypeDefinitions = container.take()
    )
  }
}

sealed class GQLTypeDefinition : GQLDefinition, GQLNamed, GQLDescribed {
  fun isBuiltIn(): Boolean = builtInTypes.contains(this.name)

  /**
   * This duplicates some of what's in "builtins.graphqls" but it's easier to access
   */
  private companion object {
    val builtInTypes: Set<String> = setOf(
        "Int",
        "Float",
        "String",
        "Boolean",
        "ID",
        "__Schema",
        "__Type",
        "__Field",
        "__InputValue",
        "__EnumValue",
        "__TypeKind",
        "__Directive",
        "__DirectiveLocation"
    )
  }
}

data class GQLInterfaceTypeDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val description: String?,
    override val name: String,
    val implementsInterfaces: List<String>,
    val directives: List<GQLDirective>,
    val fields: List<GQLFieldDefinition>,
) : GQLTypeDefinition() {

  override val children: List<GQLNode> = directives + fields

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"${GraphQLString.encodeTripleQuoted(description)}\"\"\"\n")
      writeUtf8("interface $name")
      if (implementsInterfaces.isNotEmpty()) {
        writeUtf8(" implements ")
        writeUtf8(implementsInterfaces.joinToString(" "))
      }
      if (directives.isNotEmpty()) {
        writeUtf8(" ")
        directives.join(bufferedSink)
      }
      if (fields.isNotEmpty()) {
        writeUtf8(" ")
        fields.join(bufferedSink, prefix = "{\n", separator = "\n", postfix = "\n}\n")
      }
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        fields = container.take()
    )
  }
}

data class GQLObjectTypeDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val description: String?,
    override val name: String,
    val implementsInterfaces: List<String>,
    val directives: List<GQLDirective>,
    val fields: List<GQLFieldDefinition>,
) : GQLTypeDefinition() {

  override val children: List<GQLNode> = directives + fields

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"${GraphQLString.encodeTripleQuoted(description)}\"\"\"\n")
      writeUtf8("type $name")
      if (implementsInterfaces.isNotEmpty()) {
        writeUtf8(" implements ")
        writeUtf8(implementsInterfaces.joinToString(" "))
      }
      if (directives.isNotEmpty()) {
        writeUtf8(" ")
        directives.join(bufferedSink)
      }
      if (fields.isNotEmpty()) {
        fields.join(bufferedSink, prefix = " {\n", separator = "\n", postfix = "\n}\n")
      }
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        fields = container.take()
    )
  }
}

data class GQLInputObjectTypeDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val description: String?,
    override val name: String,
    val directives: List<GQLDirective>,
    val inputFields: List<GQLInputValueDefinition>,
) : GQLTypeDefinition() {

  override val children: List<GQLNode> = directives + inputFields

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"${GraphQLString.encodeTripleQuoted(description)}\"\"\"\n")
      writeUtf8("input $name")
      if (directives.isNotEmpty()) {
        writeUtf8(" ")
        directives.join(bufferedSink)
      }
      if (inputFields.isNotEmpty()) {
        writeUtf8(" ")
        inputFields.join(bufferedSink, prefix = "{\n", separator = "\n", postfix = "\n}\n")
      }
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        inputFields = container.take()
    )
  }
}

data class GQLScalarTypeDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val description: String?,
    override val name: String,
    val directives: List<GQLDirective>,
) : GQLTypeDefinition() {

  override val children = directives

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"${GraphQLString.encodeTripleQuoted(description)}\"\"\"\n")
      writeUtf8("scalar $name")
      if (directives.isNotEmpty()) {
        writeUtf8(" ")
        directives.join(bufferedSink)
      }
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take()
    )
  }
}

data class GQLEnumTypeDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val description: String?,
    override val name: String,
    val directives: List<GQLDirective>,
    val enumValues: List<GQLEnumValueDefinition>,
) : GQLTypeDefinition() {

  override val children: List<GQLNode> = directives + enumValues

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"${GraphQLString.encodeTripleQuoted(description)}\"\"\"\n")
      writeUtf8("enum $name")
      if (directives.isNotEmpty()) {
        writeUtf8(" ")
        directives.join(bufferedSink)
      }
      if (enumValues.isNotEmpty()) {
        writeUtf8(" ")
        enumValues.join(bufferedSink, prefix = "{\n", separator = "\n", postfix = "\n}\n")
      }
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        enumValues = container.take()
    )
  }
}

data class GQLUnionTypeDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val description: String?,
    override val name: String,
    val directives: List<GQLDirective>,
    val memberTypes: List<GQLNamedType>,
) : GQLTypeDefinition() {

  override val children: List<GQLNode> = directives + memberTypes

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"${GraphQLString.encodeTripleQuoted(description)}\"\"\"\n")
      writeUtf8("union $name")
      if (directives.isNotEmpty()) {
        writeUtf8(" ")
        directives.join(bufferedSink)
      }
      writeUtf8(" = ")
      memberTypes.join(bufferedSink, separator = "|")
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        memberTypes = container.take()
    )
  }
}

data class GQLDirectiveDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    override val name: String,
    val arguments: List<GQLInputValueDefinition>,
    val repeatable: Boolean,
    val locations: List<GQLDirectiveLocation>,
) : GQLDefinition, GQLNamed {

  override val children: List<GQLNode> = arguments

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"${GraphQLString.encodeTripleQuoted(description)}\"\"\"\n")
      writeUtf8("directive @$name")
      if (arguments.isNotEmpty()) {
        writeUtf8(" ")
        arguments.join(bufferedSink, prefix = "(", separator = ", ", postfix = ")")
      }
      if (repeatable) {
        writeUtf8(" repeatable")
      }
      writeUtf8(" on ${locations.joinToString("|")}")
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        arguments = container.take(),
    )
  }

  fun isBuiltIn(): Boolean = builtInDirectives.contains(this.name)

  companion object {
    val builtInDirectives: Set<String> = setOf(
        "include",
        "skip",
        "deprecated",
    )
  }
}

data class GQLSchemaExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val directives: List<GQLDirective>,
    val operationTypesDefinition: List<GQLOperationTypeDefinition>,
) : GQLDefinition, GQLTypeSystemExtension {

  override val children = directives + operationTypesDefinition

  override fun write(bufferedSink: BufferedSink) {
    TODO("Not yet implemented")
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        operationTypesDefinition = container.take()
    )
  }
}

data class GQLEnumTypeExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val directives: List<GQLDirective>,
    val enumValues: List<GQLEnumValueDefinition>,
) : GQLDefinition, GQLTypeExtension {

  override val children: List<GQLNode> = directives + enumValues

  override fun write(bufferedSink: BufferedSink) {
    TODO("Not yet implemented")
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        enumValues = container.take()
    )
  }
}

data class GQLObjectTypeExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val directives: List<GQLDirective>,
    val fields: List<GQLFieldDefinition>,
) : GQLDefinition, GQLTypeExtension {

  override val children: List<GQLNode> = directives + fields

  override fun write(bufferedSink: BufferedSink) {
    TODO("Not yet implemented")
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        fields = container.take()
    )
  }
}

data class GQLInputObjectTypeExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val directives: List<GQLDirective>,
    val inputFields: List<GQLInputValueDefinition>,
) : GQLDefinition, GQLTypeExtension {

  override val children: List<GQLNode> = directives + inputFields

  override fun write(bufferedSink: BufferedSink) {
    TODO("Not yet implemented")
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        inputFields = container.take()
    )
  }
}

data class GQLScalarTypeExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val directives: List<GQLDirective>,
) : GQLDefinition, GQLTypeExtension {

  override val children = directives

  override fun write(bufferedSink: BufferedSink) {
    TODO("Not yet implemented")
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take()
    )
  }
}

data class GQLInterfaceTypeExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val fields: List<GQLFieldDefinition>,
) : GQLDefinition, GQLTypeExtension, GQLNamed {

  override val children = fields

  override fun write(bufferedSink: BufferedSink) {
    TODO("Not yet implemented")
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        fields = container.take()
    )
  }
}

data class GQLUnionTypeExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val directives: List<GQLDirective>,
    val memberTypes: List<GQLNamedType>,
) : GQLDefinition, GQLTypeExtension {

  override val children: List<GQLNode> = directives + memberTypes

  override fun write(bufferedSink: BufferedSink) {
    TODO("Not yet implemented")
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        memberTypes = container.take()
    )
  }
}

data class GQLEnumValueDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    override val name: String,
    val directives: List<GQLDirective>,
) : GQLNode, GQLNamed {

  override val children = directives

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"${GraphQLString.encodeTripleQuoted(description)}\"\"\"\n")
      writeUtf8(name)
      if (directives.isNotEmpty()) {
        writeUtf8(" ")
        directives.join(bufferedSink)
      }

    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take()
    )
  }
}

data class GQLFieldDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    override val name: String,
    val arguments: List<GQLInputValueDefinition>,
    val type: GQLType,
    val directives: List<GQLDirective>,
) : GQLNode, GQLNamed {

  override val children: List<GQLNode> = directives + arguments

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"${GraphQLString.encodeTripleQuoted(description)}\"\"\"\n")
      writeUtf8(name)
      if (arguments.isNotEmpty()) {
        writeUtf8(" ")
        arguments.join(bufferedSink, prefix = "(", separator = ", ", postfix = ")")
      }
      writeUtf8(": ")
      type.write(bufferedSink)
      if (directives.isNotEmpty()) {
        writeUtf8(" ")
        directives.join(bufferedSink)
      }
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        arguments = container.take()
    )
  }
}

data class GQLInputValueDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    override val name: String,
    val directives: List<GQLDirective>,
    val type: GQLType,
    val defaultValue: GQLValue?,
) : GQLNode, GQLNamed {

  override val children = directives

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"${GraphQLString.encodeTripleQuoted(description)}\"\"\" ")
      writeUtf8("$name: ")
      type.write(bufferedSink)
      if (defaultValue != null) {
        writeUtf8(" = ")
        defaultValue.write(bufferedSink)
      }
      if (directives.isNotEmpty()) {
        writeUtf8(" ")
        directives.join(bufferedSink)
      }
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take()
    )
  }
}

/**
 * A variable definition is very similar to an InputValue definition except it doesn't
 * have a description
 */
data class GQLVariableDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val name: String,
    val type: GQLType,
    val defaultValue: GQLValue?,
    val directives: List<GQLDirective>,
) : GQLNode {

  override val children = listOfNotNull(defaultValue) + directives

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("${'$'}$name: ")
      type.write(bufferedSink)
      if (defaultValue != null) {
        writeUtf8(" = ")
        defaultValue.write(bufferedSink)
        writeUtf8(" ")
      }
      // TODO("support variable directives")
      // directives.join(bufferedSink)
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        defaultValue = container.takeSingle()
    )
  }
}

data class GQLOperationTypeDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val operationType: String,
    val namedType: String,
) : GQLNode {

  override val children = emptyList<GQLNode>()

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("$operationType: $namedType")
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLDirective(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val arguments: GQLArguments?,
) : GQLNode, GQLNamed {

  override val children = listOfNotNull(arguments)

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("@$name")
      arguments?.write(bufferedSink)
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        arguments = container.takeSingle()
    )
  }
}

data class GQLObjectField(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val name: String,
    val value: GQLValue,
) : GQLNode {

  override val children = listOf(value)

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("$name: ")
      value.write(bufferedSink)
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        value = container.takeSingle()!!
    )
  }
}

data class GQLArgument(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val name: String,
    val value: GQLValue,
) : GQLNode {

  override val children = listOf(value)

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("$name: ")
      value.write(bufferedSink)
    }
  }
  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        value = container.takeSingle()!!
    )
  }
}

data class GQLSelectionSet(
    val selections: List<GQLSelection>,
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
) : GQLNode {
  override val children = selections

  override fun write(bufferedSink: BufferedSink) {
    selections.join(bufferedSink, prefix = "{\n", separator = "\n", postfix = "\n}")
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        selections = container.take()
    )
  }
}

data class GQLArguments(
    val arguments: List<GQLArgument>,
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
) : GQLNode {
  override val children: List<GQLNode> = arguments

  override fun write(bufferedSink: BufferedSink) {
    arguments.join(bufferedSink, prefix = "(", separator = ", ", postfix = ")")
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        arguments = container.take()
    )
  }
}

data class GQLField(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val alias: String?,
    val name: String,
    val arguments: GQLArguments?,
    val directives: List<GQLDirective>,
    val selectionSet: GQLSelectionSet?,
) : GQLSelection() {

  override val children: List<GQLNode> = listOfNotNull(selectionSet) + listOfNotNull(arguments)

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (alias != null) {
        writeUtf8("$alias: ")
      }
      writeUtf8(name)
      arguments?.write(bufferedSink)
      if (directives.isNotEmpty()) {
        writeUtf8(" ")
        directives.join(bufferedSink)
      }
      if (selectionSet != null) {
        writeUtf8(" ")
        selectionSet.write(bufferedSink)
      }
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        selectionSet = container.takeSingle(),
        arguments = container.takeSingle()
    )
  }
}

data class GQLInlineFragment(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val typeCondition: GQLNamedType,
    val directives: List<GQLDirective>,
    val selectionSet: GQLSelectionSet,
) : GQLSelection() {

  override val children = directives + selectionSet + typeCondition

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("... on ${typeCondition.name}")
      if (directives.isNotEmpty()) {
        writeUtf8(" ")
        directives.join(bufferedSink)
      }
      if (selectionSet.selections.isNotEmpty()) {
        writeUtf8(" ")
        selectionSet.write(bufferedSink)
      }
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        selectionSet = container.takeSingle()!!,
        typeCondition = container.takeSingle()!!

    )
  }
}

data class GQLFragmentSpread(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val name: String,
    val directives: List<GQLDirective>,
) : GQLSelection() {

  override val children = directives

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("...${name}")
      if (directives.isNotEmpty()) {
        writeUtf8(" ")
        directives.join(bufferedSink)
      }

    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take()
    )
  }
}

sealed class GQLType : GQLNode

data class GQLNamedType(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
) : GQLType(), GQLNamed {

  override val children = emptyList<GQLNode>()

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8(name)
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLNonNullType(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val type: GQLType,
) : GQLType() {

  override val children = listOf(type)

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      type.write(bufferedSink)
      writeUtf8("!")
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        type = container.takeSingle()!!
    )
  }
}

data class GQLListType(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val type: GQLType,
) : GQLType() {

  override val children = listOf(type)

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("[")
      type.write(bufferedSink)
      writeUtf8("]")
    }
  }

  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        type = container.takeSingle()!!
    )
  }
}


sealed class GQLValue : GQLNode
data class GQLVariableValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val name: String,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("${'$'}$name")
    }
  }
  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLIntValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: Int,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8(value.toString())
    }
  }
  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLFloatValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: Double,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8(value.toString())
    }
  }
  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLStringValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: String,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("\"${GraphQLString.encodeSingleQuoted(value)}\"")
    }
  }
  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLBooleanValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: Boolean,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8(value.toString())
    }
  }
  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLEnumValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: String,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8(value)
    }
  }
  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLListValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val values: List<GQLValue>,
) : GQLValue() {

  override val children = values

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("[")
      values.join(bufferedSink, ",")
      writeUtf8("]")
    }
  }
  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        values = container.take()
    )
  }
}

data class GQLObjectValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val fields: List<GQLObjectField>,
) : GQLValue() {

  override val children = fields

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("{\n")
      fields.join(bufferedSink = bufferedSink, "\n")
      writeUtf8("\n}\n")
    }
  }
  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return copy(
        fields = container.take()
    )
  }
}

data class GQLNullValue(override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("null")
    }
  }
  override fun copyWithNewChildren(container: NodeContainer): GQLNode {
    return this
  }
}

private fun <T : GQLNode> List<T>.join(bufferedSink: BufferedSink, separator: String = " ", prefix: String = "", postfix: String = "") {
  bufferedSink.writeUtf8(prefix)
  forEachIndexed { index, t ->
    t.write(bufferedSink)
    if (index < size - 1) {
      bufferedSink.writeUtf8(separator)
    }
  }
  bufferedSink.writeUtf8(postfix)
}

enum class GQLDirectiveLocation {
  QUERY,
  MUTATION,
  SUBSCRIPTION,
  FIELD,
  FRAGMENT_DEFINITION,
  FRAGMENT_SPREAD,
  INLINE_FRAGMENT,
  VARIABLE_DEFINITION,
  TypeSystemDirectiveLocation,
  SCHEMA,
  SCALAR,
  OBJECT,
  FIELD_DEFINITION,
  ARGUMENT_DEFINITION,
  INTERFACE,
  UNION,
  ENUM,
  ENUM_VALUE,
  INPUT_OBJECT,
  INPUT_FIELD_DEFINITION,
}

fun GQLNode.transform(block: (GQLNode) -> GQLNode?): GQLNode? {

  val newChildren = children.mapNotNull {
    it.transform(block)
  }
  val container = NodeContainer(newChildren)
  val ret = block(this)?.copyWithNewChildren(container)
  container.assert()
  return ret
}

@Suppress("UNCHECKED_CAST")
class NodeContainer(nodes: List<GQLNode>) {
  var remainingNodes = nodes

  inline fun <reified T : GQLNode> take(): List<T> {
    val (ret, rem) = remainingNodes.partition { it is T }

    remainingNodes = rem
    return ret as List<T>
  }

  inline fun <reified T : GQLNode> takeSingle(): T? {
    val (ret, rem) = remainingNodes.partition { it is T }

    remainingNodes = rem

    check(ret.size <= 1)
    return ret.firstOrNull() as T?
  }

  fun assert() {
    check(remainingNodes.isEmpty()) {
      "Remaining nodes: $remainingNodes"
    }
  }
}