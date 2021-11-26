package com.apollographql.apollo3.ast

/**
 * The GraphQL AST definition
 *
 * This is all in one file so we can use sealed classes. Extensions are in gqlxyz.kt
 */

/**
 * A node in the GraphQL AST.
 *
 * The structure of the different nodes matches closely the one of the GraphQL specification
 * (https://spec.graphql.org/June2018/#sec-Appendix-Grammar-Summary.Document)
 *
 * Compared to the Antlr [com.apollographql.apollo3.generated.antlr.GraphQLParser.DocumentContext], a GQLDocument
 * is a lot simpler and allows for easy modifying a document (using [GQLNode.transform]()) and outputing them to a [okio.BufferedSink].
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

  /**
   * Internal-only. Copies this code using the given children
   *
   *  Write the node to the given writer
   *
   * The general convention is that [GQLNode] should output their trailing line if they know
   * they will need one
   */
  fun writeInternal(writer: SDLWriter)

  /**
   * Internal-only. Copies this code using the given children
   *
   * To transform an AST, use [GQLNode.transform] instead
   */
  fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode
}

sealed interface TransformResult {
  object Delete : TransformResult
  object Continue : TransformResult
  class Replace(val newNode: GQLNode) : TransformResult
}

fun interface NodeTransformer {
  fun transform(node: GQLNode): TransformResult
}

fun GQLNode.transform(transformer: NodeTransformer): GQLNode? {
  return when (val result = transformer.transform(this)) {
    is TransformResult.Delete -> null
    is TransformResult.Replace -> result.newNode
    is TransformResult.Continue -> {
      val newChildren = children.mapNotNull { it.transform(transformer) }
      val nodeContainer = NodeContainer(newChildren)
      copyWithNewChildrenInternal(nodeContainer).also {
        nodeContainer.assert()
      }
    }
  }
}

/**
 * A [GQLNode] that has a name
 */
interface GQLNamed {
  val name: String
}

/**
 * A [GQLNode] that has a description
 */
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
 * See [parseAsGQLDocument] for how to obtain a [GQLDocument].
 */
data class GQLDocument(
    val definitions: List<GQLDefinition>,
    val filePath: String?,
) : GQLNode {
  override val sourceLocation: SourceLocation = SourceLocation(0, 0, filePath)
  override val children = definitions

  override fun writeInternal(writer: SDLWriter) {
    definitions.join(writer = writer, separator = "\n")
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write(operationType)
      if (name != null) {
        write(" ")
        write(name)
        if (variableDefinitions.isNotEmpty()) {
          variableDefinitions.join(writer = writer, separator = ", ", prefix = "(", postfix = ")")
        }
      }
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      if (selectionSet.selections.isNotEmpty()) {
        write(" ")
        writer.write(selectionSet)
      }
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("fragment $name on ${typeCondition.name}")
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      if (selectionSet.selections.isNotEmpty()) {
        write(" ")
        writer.write(selectionSet)
      }
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writeDescription(description)
      if (directives.isNotEmpty()) {
        directives.join(writer)
        write(" ")
      }
      write("schema ")
      write("{\n")
      indent()
      rootOperationTypeDefinitions.join(writer, separator = "")
      unindent()
      write("}\n")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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
  companion object {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writeDescription(description)
      write("interface $name")
      if (implementsInterfaces.isNotEmpty()) {
        write(" implements ")
        write(implementsInterfaces.joinToString(" "))
      }
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      if (fields.isNotEmpty()) {
        write(" ")
        write("{\n")
        indent()
        fields.join(writer, separator = "\n\n")
        unindent()
        write("\n}\n")
      }
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writeDescription(description)
      write("type $name")
      if (implementsInterfaces.isNotEmpty()) {
        write(" implements ")
        write(implementsInterfaces.joinToString(" "))
      }
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      if (fields.isNotEmpty()) {
        write(" ")
        write("{\n")
        indent()
        fields.join(writer, separator = "\n\n")
        unindent()
        write("\n}\n")
      }
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writeDescription(description)
      write("input $name")
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      if (inputFields.isNotEmpty()) {
        write(" ")
        write("{\n")
        indent()
        inputFields.join(writer, separator = "\n")
        unindent()
        write("}\n")
      }
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writeDescription(description)
      write("scalar $name")
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      write("\n")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writeDescription(description)
      write("enum $name")
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      if (enumValues.isNotEmpty()) {
        write(" ")
        write("{\n")
        indent()
        enumValues.join(writer, separator = "\n")
        unindent()
        write("}\n")
      }
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writeDescription(description)
      write("union $name")
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      write(" = ")
      memberTypes.join(writer, separator = "|")
      write("\n")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writeDescription(description)
      write("directive @$name")
      if (arguments.isNotEmpty()) {
        write(" ")
        arguments.join(writer, prefix = "(", separator = ", ", postfix = ")") {
          it.write(writer, true)
        }
      }
      if (repeatable) {
        write(" repeatable")
      }
      write(" on ${locations.joinToString("|")}")
      write("\n")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    TODO("Not yet implemented")
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    TODO("Not yet implemented")
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        enumValues = container.take()
    )
  }
}

data class GQLObjectTypeExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val implementsInterfaces: List<String>,
    val directives: List<GQLDirective>,
    val fields: List<GQLFieldDefinition>,
) : GQLDefinition, GQLTypeExtension {

  override val children: List<GQLNode> = directives + fields

  override fun writeInternal(writer: SDLWriter) {
    TODO("Not yet implemented")
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    TODO("Not yet implemented")
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    TODO("Not yet implemented")
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take()
    )
  }
}

data class GQLInterfaceTypeExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val implementsInterfaces: List<String>,
    val fields: List<GQLFieldDefinition>,
) : GQLDefinition, GQLTypeExtension, GQLNamed {

  override val children = fields

  override fun writeInternal(writer: SDLWriter) {
    TODO("Not yet implemented")
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    TODO("Not yet implemented")
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writeDescription(description)
      write(name)
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      write("\n")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writeDescription(description)
      write(name)
      if (arguments.isNotEmpty()) {
        arguments.join(writer, prefix = "(", separator = ", ", postfix = ")") {
          it.write(writer, true)
        }
      }
      write(": ")
      writer.write(type)
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  /**
   * @param inline whether the input value definition is used inline (for an example a field argument)
   * or as a block (for an example for input fields)
   */
  fun write(writer: SDLWriter, inline: Boolean) {
    with(writer) {
      if (inline) {
        writeInlineString(description)
        if (description != null) {
          write(" ")
        }
      } else {
        writeDescription(description)
      }
      write("$name: ")
      writer.write(type)
      if (defaultValue != null) {
        write(" = ")
        writer.write(defaultValue)
      }
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      if (!inline) {
        write("\n")
      }
    }
  }

  override fun writeInternal(writer: SDLWriter) {
    write(writer, false)
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("${'$'}$name: ")
      writer.write(type)
      if (defaultValue != null) {
        write(" = ")
        writer.write(defaultValue)
        write(" ")
      }
      // TODO("support variable directives")
      // directives.join(writer)
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("$operationType: $namedType\n")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLDirective(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val arguments: GQLArguments?,
) : GQLNode, GQLNamed {

  override val children = listOfNotNull(arguments)

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("@$name")
      if (arguments != null) {
        writer.write(arguments)
      }
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("$name: ")
      writer.write(value)
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("$name: ")
      writer.write(value)
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("{\n")
      indent()
      selections.join(writer, separator = "")
      unindent()
      write("}\n")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    arguments.join(writer, prefix = "(", separator = ", ", postfix = ")")
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override val children: List<GQLNode> = listOfNotNull(selectionSet) + listOfNotNull(arguments) + directives

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      if (alias != null) {
        write("$alias: ")
      }
      write(name)
      if (arguments != null) {
        writer.write(arguments)
      }
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      if (selectionSet != null) {
        write(" ")
        writer.write(selectionSet)
      } else {
        write("\n")
      }
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        selectionSet = container.takeSingle(),
        arguments = container.takeSingle(),
        directives = container.take()
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("... on ${typeCondition.name}")
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      if (selectionSet.selections.isNotEmpty()) {
        write(" ")
        writer.write(selectionSet)
      }
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("...${name}")
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      write("\n")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write(name)
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLNonNullType(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val type: GQLType,
) : GQLType() {

  override val children = listOf(type)

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writer.write(type)
      write("!")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("[")
      writer.write(type)
      write("]")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("${'$'}$name")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLIntValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: Int,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write(value.toString())
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLFloatValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: Double,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write(value.toString())
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLStringValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: String,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("\"${value.encodeToGraphQLSingleQuoted()}\"")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLBooleanValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: Boolean,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write(value.toString())
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLEnumValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: String,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write(value)
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

data class GQLListValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val values: List<GQLValue>,
) : GQLValue() {

  override val children = values

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("[")
      values.join(writer, ",")
      write("]")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
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

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("{\n")
      indent()
      fields.join(writer = writer, "\n")
      unindent()
      write("\n}\n")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        fields = container.take()
    )
  }
}

data class GQLNullValue(override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("null")
    }
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

private fun <T : GQLNode> List<T>.join(
    writer: SDLWriter,
    separator: String = " ",
    prefix: String = "",
    postfix: String = "",
    block: (T) -> Unit = { writer.write(it) },
) {
  writer.write(prefix)
  forEachIndexed { index, t ->
    block(t)
    if (index < size - 1) {
      writer.write(separator)
    }
  }
  writer.write(postfix)
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