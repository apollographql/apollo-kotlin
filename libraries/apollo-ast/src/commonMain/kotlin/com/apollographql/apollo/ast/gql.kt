package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental

/**
 * The GraphQL AST definition
 */

/**
 * A node in the GraphQL AST.
 *
 * The structure of the different nodes matches closely the one of the GraphQL specification
 * (https://spec.graphql.org/June2018/#sec-Appendix-Grammar-Summary.Document)
 *
 * Whitespace tokens are not mapped to GQLNodes so some formatting will be lost during modification
 */
sealed interface GQLNode {
  val sourceLocation: SourceLocation?

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

@ApolloExperimental
sealed interface TransformResult {
  object Delete : TransformResult
  object Continue : TransformResult

  /**
   * Replace the given node. This is terminal in that the node children will not be visited
   * unless you do it explicitly by calling [transform] recursively
   */
  class Replace(val newNode: GQLNode) : TransformResult
}

@ApolloExperimental
fun interface NodeTransformer {
  fun transform(node: GQLNode): TransformResult
}

@ApolloExperimental
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

internal fun interface NodeTransformer2 {
  fun transform(node: GQLNode): GQLNode?
}

/**
 * [transform] above has a lot of limitations. Most importantly, replacing a node will not visit all
 * its children.
 * This version allows this.
 * TODO: revisit this API so that it does not make a new copy of every node
 */
internal fun GQLNode.transform2(transformer: NodeTransformer2): GQLNode? {
  val newChildren = children.mapNotNull { it.transform2(transformer) }
  val nodeContainer = NodeContainer(newChildren)

  return transformer.transform(
      copyWithNewChildrenInternal(nodeContainer).also {
        nodeContainer.assert()
      }
  )
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

interface GQLHasDirectives {
  val directives: List<GQLDirective>
}

sealed interface GQLDefinition : GQLNode
sealed interface GQLExecutableDefinition : GQLDefinition
sealed interface GQLTypeSystemExtension : GQLDefinition
sealed interface GQLTypeExtension : GQLTypeSystemExtension, GQLNamed

sealed class GQLSelection : GQLNode

/**
 * The top level node in a GraphQL document. This can be a schema document or an executable document
 * (or something else if need be)
 *
 * See [parseAsGQLDocument] for how to obtain a [GQLDocument].
 */
class GQLDocument(
    val definitions: List<GQLDefinition>,
    override val sourceLocation: SourceLocation?,
) : GQLNode {
  override val children = definitions

  override fun writeInternal(writer: SDLWriter) {
    definitions.join(writer = writer, separator = "\n")
  }

  fun copy(
      definitions: List<GQLDefinition> = this.definitions,
      sourceLocation: SourceLocation? = this.sourceLocation,
  ): GQLDocument {
    return GQLDocument(
        definitions = definitions,
        sourceLocation = sourceLocation
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        definitions = container.take(),
        sourceLocation = sourceLocation
    )
  }

  companion object
}

class GQLOperationDefinition(
    override val sourceLocation: SourceLocation? = null,
    val operationType: String,
    val name: String?,
    val variableDefinitions: List<GQLVariableDefinition>,
    override val directives: List<GQLDirective>,
    val selections: List<GQLSelection>,
    override val description: String?, // spec extension
) : GQLExecutableDefinition, GQLDescribed, GQLHasDirectives {
  @Suppress("DEPRECATION")
  @Deprecated("Use selections directly")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  val selectionSet: GQLSelectionSet
    get() {
      return GQLSelectionSet(
          sourceLocation = null,
          selections = selections
      )
    }

  override val children = variableDefinitions + directives + selections

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
      if (selections.isNotEmpty()) {
        write(" ")
        selections.writeSelections(writer)
      }
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      operationType: String = this.operationType,
      name: String? = this.name,
      variableDefinitions: List<GQLVariableDefinition> = this.variableDefinitions,
      directives: List<GQLDirective> = this.directives,
      selections: List<GQLSelection> = this.selections,
      description: String? = this.description,
  ): GQLOperationDefinition {
    return GQLOperationDefinition(
        sourceLocation = sourceLocation,
        operationType = operationType,
        name = name,
        variableDefinitions = variableDefinitions,
        directives = directives,
        selections = selections,
        description = description,
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        variableDefinitions = container.take(),
        directives = container.take(),
        selections = container.take(),
    )
  }
}

class GQLFragmentDefinition(
    override val sourceLocation: SourceLocation? = null,
    override val name: String,
    override val directives: List<GQLDirective>,
    val typeCondition: GQLNamedType,
    val selections: List<GQLSelection>,
    override val description: String?, // spec extension
) : GQLExecutableDefinition, GQLNamed, GQLDescribed, GQLHasDirectives {
  @Suppress("DEPRECATION")
  @Deprecated("Use selections directly")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  val selectionSet: GQLSelectionSet
    get() {
      return GQLSelectionSet(
          sourceLocation = null,
          selections = selections
      )
    }

  override val children = directives + selections + typeCondition

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("fragment $name on ${typeCondition.name}")
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      if (selections.isNotEmpty()) {
        write(" ")
        selections.writeSelections(writer)
      }
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      name: String = this.name,
      directives: List<GQLDirective> = this.directives,
      typeCondition: GQLNamedType = this.typeCondition,
      selections: List<GQLSelection> = this.selections,
      description: String? = this.description,
  ): GQLFragmentDefinition {
    return GQLFragmentDefinition(
        sourceLocation = sourceLocation,
        name = name,
        directives = directives,
        typeCondition = typeCondition,
        selections = selections,
        description = description,
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        typeCondition = container.take<GQLNamedType>().single(),
        selections = container.take(),
    )
  }
}

class GQLSchemaDefinition(
    override val sourceLocation: SourceLocation? = null,
    override val description: String?,
    override val directives: List<GQLDirective>,
    val rootOperationTypeDefinitions: List<GQLOperationTypeDefinition>,
) : GQLDefinition, GQLDescribed, GQLHasDirectives {

  override val children = directives + rootOperationTypeDefinitions

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writeDescription(description)
      write("schema ")
      if (directives.isNotEmpty()) {
        directives.join(writer)
        write(" ")
      }
      write("{\n")
      indent()
      rootOperationTypeDefinitions.join(writer, separator = "")
      unindent()
      write("}\n")
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      description: String? = this.description,
      directives: List<GQLDirective> = this.directives,
      rootOperationTypeDefinitions: List<GQLOperationTypeDefinition> = this.rootOperationTypeDefinitions,
  ): GQLSchemaDefinition {
    return GQLSchemaDefinition(
        sourceLocation = sourceLocation,
        description = description,
        directives = directives,
        rootOperationTypeDefinitions = rootOperationTypeDefinitions,
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        rootOperationTypeDefinitions = container.take(),
    )
  }
}

sealed class GQLTypeDefinition : GQLDefinition, GQLNamed, GQLDescribed, GQLHasDirectives {
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

class GQLInterfaceTypeDefinition(
    override val sourceLocation: SourceLocation? = null,
    override val description: String?,
    override val name: String,
    val implementsInterfaces: List<String>,
    override val directives: List<GQLDirective>,
    val fields: List<GQLFieldDefinition>,
) : GQLTypeDefinition() {

  override val children: List<GQLNode> = directives + fields

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writeDescription(description)
      write("interface $name")
      if (implementsInterfaces.isNotEmpty()) {
        write(" implements ")
        write(implementsInterfaces.joinToString(" & "))
      }
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      write(" ")
      write("{\n")
      indent()
      fields.join(writer, separator = "\n\n")
      unindent()
      write("\n}\n")
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      description: String? = this.description,
      name: String = this.name,
      implementsInterfaces: List<String> = this.implementsInterfaces,
      directives: List<GQLDirective> = this.directives,
      fields: List<GQLFieldDefinition> = this.fields,
  ): GQLInterfaceTypeDefinition = GQLInterfaceTypeDefinition(
      sourceLocation = sourceLocation,
      description = description,
      name = name,
      implementsInterfaces = implementsInterfaces,
      directives = directives,
      fields = fields,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        fields = container.take(),
    )
  }
}

class GQLObjectTypeDefinition(
    override val sourceLocation: SourceLocation? = null,
    override val description: String?,
    override val name: String,
    val implementsInterfaces: List<String>,
    override val directives: List<GQLDirective>,
    val fields: List<GQLFieldDefinition>,
) : GQLTypeDefinition() {

  override val children: List<GQLNode> = directives + fields

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writeDescription(description)
      write("type $name")
      if (implementsInterfaces.isNotEmpty()) {
        write(" implements ")
        write(implementsInterfaces.joinToString(" & "))
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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      description: String? = this.description,
      name: String = this.name,
      implementsInterfaces: List<String> = this.implementsInterfaces,
      directives: List<GQLDirective> = this.directives,
      fields: List<GQLFieldDefinition> = this.fields,
  ): GQLObjectTypeDefinition {
    return GQLObjectTypeDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        implementsInterfaces = implementsInterfaces,
        directives = directives,
        fields = fields,
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        fields = container.take(),
    )
  }
}

class GQLInputObjectTypeDefinition(
    override val sourceLocation: SourceLocation? = null,
    override val description: String?,
    override val name: String,
    override val directives: List<GQLDirective>,
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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      description: String? = this.description,
      name: String = this.name,
      directives: List<GQLDirective> = this.directives,
      inputFields: List<GQLInputValueDefinition> = this.inputFields,
  ): GQLInputObjectTypeDefinition {
    return GQLInputObjectTypeDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        directives = directives,
        inputFields = inputFields,
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        inputFields = container.take(),
    )
  }
}

class GQLScalarTypeDefinition(
    override val sourceLocation: SourceLocation? = null,
    override val description: String?,
    override val name: String,
    override val directives: List<GQLDirective>,
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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      description: String? = this.description,
      name: String = this.name,
      directives: List<GQLDirective> = this.directives,
  ): GQLScalarTypeDefinition = GQLScalarTypeDefinition(
      sourceLocation = sourceLocation,
      description = description,
      name = name,
      directives = directives,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take()
    )
  }
}

class GQLEnumTypeDefinition(
    override val sourceLocation: SourceLocation? = null,
    override val description: String?,
    override val name: String,
    override val directives: List<GQLDirective>,
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
      write(" ")
      write("{\n")
      indent()
      enumValues.join(writer, separator = "\n")
      unindent()
      write("}\n")
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      description: String? = this.description,
      name: String = this.name,
      directives: List<GQLDirective> = this.directives,
      enumValues: List<GQLEnumValueDefinition> = this.enumValues,
  ): GQLEnumTypeDefinition = GQLEnumTypeDefinition(
      sourceLocation = sourceLocation,
      description = description,
      name = name,
      directives = directives,
      enumValues = enumValues,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        enumValues = container.take(),
    )
  }
}

class GQLUnionTypeDefinition(
    override val sourceLocation: SourceLocation? = null,
    override val description: String?,
    override val name: String,
    override val directives: List<GQLDirective>,
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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      description: String? = this.description,
      name: String = this.name,
      directives: List<GQLDirective> = this.directives,
      memberTypes: List<GQLNamedType> = this.memberTypes,
  ): GQLUnionTypeDefinition = GQLUnionTypeDefinition(
      sourceLocation = sourceLocation,
      description = description,
      name = name,
      directives = directives,
      memberTypes = memberTypes,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        memberTypes = container.take(),
    )
  }
}

/**
 * @param name the name of the directive without the '@'
 */
class GQLDirectiveDefinition(
    override val sourceLocation: SourceLocation? = null,
    override val description: String?,
    override val name: String,
    val arguments: List<GQLInputValueDefinition>,
    val repeatable: Boolean,
    val locations: List<GQLDirectiveLocation>,
) : GQLDefinition, GQLDescribed, GQLNamed {
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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      description: String? = this.description,
      name: String = this.name,
      arguments: List<GQLInputValueDefinition> = this.arguments,
      repeatable: Boolean = this.repeatable,
      locations: List<GQLDirectiveLocation> = this.locations,
  ): GQLDirectiveDefinition = GQLDirectiveDefinition(
      sourceLocation = sourceLocation,
      description = description,
      name = name,
      arguments = arguments,
      repeatable = repeatable,
      locations = locations,
  )

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
        "specifiedBy"
    )
  }
}

class GQLSchemaExtension(
    override val sourceLocation: SourceLocation? = null,
    override val directives: List<GQLDirective>,
    val operationTypeDefinitions: List<GQLOperationTypeDefinition>,
) : GQLDefinition, GQLTypeSystemExtension, GQLHasDirectives {

  override val children = directives + operationTypeDefinitions

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("extend schema ")
      if (directives.isNotEmpty()) {
        directives.join(writer)
        write(" ")
      }
      if (operationTypeDefinitions.isNotEmpty()) {
        write("{\n")
        indent()
        operationTypeDefinitions.join(writer, separator = "")
        unindent()
        write("}\n")
      }
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      directives: List<GQLDirective> = this.directives,
      operationTypesDefinition: List<GQLOperationTypeDefinition> = this.operationTypeDefinitions,
  ): GQLSchemaExtension = GQLSchemaExtension(
      sourceLocation = sourceLocation,
      directives = directives,
      operationTypeDefinitions = operationTypesDefinition,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        operationTypesDefinition = container.take()
    )
  }
}

class GQLEnumTypeExtension(
    override val sourceLocation: SourceLocation? = null,
    override val name: String,
    override val directives: List<GQLDirective>,
    val enumValues: List<GQLEnumValueDefinition>,
) : GQLDefinition, GQLTypeExtension, GQLHasDirectives {

  override val children: List<GQLNode> = directives + enumValues

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("extend enum $name")
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      write(" ")
      if (enumValues.isNotEmpty()) {
        write("{\n")
        indent()
        enumValues.join(writer, separator = "\n")
        unindent()
        write("}\n")
      }
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      name: String = this.name,
      directives: List<GQLDirective> = this.directives,
      enumValues: List<GQLEnumValueDefinition> = this.enumValues,
  ): GQLEnumTypeExtension = GQLEnumTypeExtension(
      sourceLocation = sourceLocation,
      name = name,
      directives = directives,
      enumValues = enumValues,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        enumValues = container.take(),
    )
  }
}

class GQLObjectTypeExtension(
    override val sourceLocation: SourceLocation? = null,
    override val name: String,
    val implementsInterfaces: List<String>,
    override val directives: List<GQLDirective>,
    val fields: List<GQLFieldDefinition>,
) : GQLDefinition, GQLTypeExtension, GQLHasDirectives {

  override val children: List<GQLNode> = directives + fields

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("extend type $name")
      if (implementsInterfaces.isNotEmpty()) {
        write(" implements ")
        write(implementsInterfaces.joinToString(" & "))
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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      name: String = this.name,
      implementsInterfaces: List<String> = this.implementsInterfaces,
      directives: List<GQLDirective> = this.directives,
      fields: List<GQLFieldDefinition> = this.fields,
  ): GQLObjectTypeExtension = GQLObjectTypeExtension(
      sourceLocation = sourceLocation,
      name = name,
      implementsInterfaces = implementsInterfaces,
      directives = directives,
      fields = fields,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        fields = container.take(),
    )
  }
}

class GQLInputObjectTypeExtension(
    override val sourceLocation: SourceLocation? = null,
    override val name: String,
    override val directives: List<GQLDirective>,
    val inputFields: List<GQLInputValueDefinition>,
) : GQLDefinition, GQLTypeExtension, GQLHasDirectives {

  override val children: List<GQLNode> = directives + inputFields

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("extend input $name")
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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      name: String = this.name,
      directives: List<GQLDirective> = this.directives,
      inputFields: List<GQLInputValueDefinition> = this.inputFields,
  ): GQLInputObjectTypeExtension = GQLInputObjectTypeExtension(
      sourceLocation = sourceLocation,
      name = name,
      directives = directives,
      inputFields = inputFields,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        inputFields = container.take()
    )
  }
}

class GQLScalarTypeExtension(
    override val sourceLocation: SourceLocation? = null,
    override val name: String,
    override val directives: List<GQLDirective>,
) : GQLDefinition, GQLTypeExtension, GQLHasDirectives {

  override val children = directives

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("extend scalar $name")
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      write("\n")
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      name: String = this.name,
      directives: List<GQLDirective> = this.directives,
  ): GQLScalarTypeExtension = GQLScalarTypeExtension(
      sourceLocation = sourceLocation,
      name = name,
      directives = directives,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take()
    )
  }
}

class GQLInterfaceTypeExtension(
    override val sourceLocation: SourceLocation? = null,
    override val name: String,
    val implementsInterfaces: List<String>,
    override val directives: List<GQLDirective>,
    val fields: List<GQLFieldDefinition>,
) : GQLDefinition, GQLTypeExtension, GQLNamed, GQLHasDirectives {

  override val children = fields

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("extend interface $name")
      if (implementsInterfaces.isNotEmpty()) {
        write(" implements ")
        write(implementsInterfaces.joinToString(" & "))
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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      name: String = this.name,
      implementsInterfaces: List<String> = this.implementsInterfaces,
      directives: List<GQLDirective> = this.directives,
      fields: List<GQLFieldDefinition> = this.fields,
  ): GQLInterfaceTypeExtension = GQLInterfaceTypeExtension(
      sourceLocation = sourceLocation,
      name = name,
      implementsInterfaces = implementsInterfaces,
      directives = directives,
      fields = fields,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        fields = container.take()
    )
  }
}

class GQLUnionTypeExtension(
    override val sourceLocation: SourceLocation? = null,
    override val name: String,
    override val directives: List<GQLDirective>,
    val memberTypes: List<GQLNamedType>,
) : GQLDefinition, GQLTypeExtension, GQLHasDirectives {

  override val children: List<GQLNode> = directives + memberTypes

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("extend union $name")
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      write(" = ")
      memberTypes.join(writer, separator = "|")
      write("\n")
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      name: String = this.name,
      directives: List<GQLDirective> = this.directives,
      memberTypes: List<GQLNamedType> = this.memberTypes,
  ): GQLUnionTypeExtension = GQLUnionTypeExtension(
      sourceLocation = sourceLocation,
      name = name,
      directives = directives,
      memberTypes = memberTypes,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        memberTypes = container.take()
    )
  }
}

class GQLEnumValueDefinition(
    override val sourceLocation: SourceLocation? = null,
    override val description: String?,
    override val name: String,
    override val directives: List<GQLDirective>,
) : GQLNode, GQLDescribed, GQLNamed, GQLHasDirectives {

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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      description: String? = this.description,
      name: String = this.name,
      directives: List<GQLDirective> = this.directives,
  ): GQLEnumValueDefinition {
    return GQLEnumValueDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        directives = directives,
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take()
    )
  }
}

class GQLFieldDefinition(
    override val sourceLocation: SourceLocation? = null,
    override val description: String?,
    override val name: String,
    val arguments: List<GQLInputValueDefinition>,
    val type: GQLType,
    override val directives: List<GQLDirective>,
) : GQLNode, GQLDescribed, GQLNamed, GQLHasDirectives {

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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      description: String? = this.description,
      name: String = this.name,
      arguments: List<GQLInputValueDefinition> = this.arguments,
      type: GQLType = this.type,
      directives: List<GQLDirective> = this.directives,
  ): GQLFieldDefinition {
    return GQLFieldDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        arguments = arguments,
        type = type,
        directives = directives,
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        arguments = container.take(),
    )
  }
}

class GQLInputValueDefinition(
    override val sourceLocation: SourceLocation? = null,
    override val description: String?,
    override val name: String,
    override val directives: List<GQLDirective>,
    val type: GQLType,
    val defaultValue: GQLValue?,
) : GQLNode, GQLDescribed, GQLNamed, GQLHasDirectives {

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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      description: String? = this.description,
      name: String = this.name,
      directives: List<GQLDirective> = this.directives,
      type: GQLType = this.type,
      defaultValue: GQLValue? = this.defaultValue,
  ): GQLInputValueDefinition {
    return GQLInputValueDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        directives = directives,
        type = type,
        defaultValue = defaultValue,
    )
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
class GQLVariableDefinition(
    override val sourceLocation: SourceLocation? = null,
    override val name: String,
    val type: GQLType,
    val defaultValue: GQLValue?,
    override val directives: List<GQLDirective>,
) : GQLNode, GQLNamed, GQLHasDirectives {

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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      name: String = this.name,
      type: GQLType = this.type,
      defaultValue: GQLValue? = this.defaultValue,
      directives: List<GQLDirective> = this.directives,
  ): GQLVariableDefinition {
    return GQLVariableDefinition(
        sourceLocation = sourceLocation,
        name = name,
        type = type,
        defaultValue = defaultValue,
        directives = directives,
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        defaultValue = container.takeSingle(),
    )
  }
}

/**
 * @param operationType one of "query", "mutation", "subscription"
 * @param namedType the name of the root object type, i.e. "Query", ...
 */
class GQLOperationTypeDefinition(
    override val sourceLocation: SourceLocation? = null,
    val operationType: String,
    val namedType: String,
) : GQLNode {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("$operationType: $namedType\n")
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      operationType: String = this.operationType,
      namedType: String = this.namedType,
  ): GQLOperationTypeDefinition {
    return GQLOperationTypeDefinition(
        sourceLocation = sourceLocation,
        operationType = operationType,
        namedType = namedType,
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

/**
 * @param name the name of the directive without the '@'. Example: "include"
 */
class GQLDirective(
    override val sourceLocation: SourceLocation? = null,
    override val name: String,
    val arguments: List<GQLArgument>,
) : GQLNode, GQLNamed {

  override val children = arguments

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("@$name")
      if (arguments.isNotEmpty()) {
        arguments.writeArguments(writer)
      }
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      name: String = this.name,
      arguments: List<GQLArgument> = this.arguments,
  ): GQLDirective {
    return GQLDirective(
        sourceLocation = sourceLocation,
        name = name,
        arguments = arguments,
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        arguments = container.take()
    )
  }
}

class GQLObjectField(
    override val sourceLocation: SourceLocation? = null,
    override val name: String,
    val value: GQLValue,
) : GQLNode, GQLNamed {

  override val children = listOf(value)

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("$name: ")
      writer.write(value)
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      name: String = this.name,
      value: GQLValue = this.value,
  ): GQLObjectField {
    return GQLObjectField(
        sourceLocation = sourceLocation,
        name = name,
        value = value,
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        value = container.takeSingle()!!
    )
  }
}

class GQLArgument(
    override val sourceLocation: SourceLocation? = null,
    override val name: String,
    val value: GQLValue,
) : GQLNode, GQLNamed {

  override val children = listOf(value)

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("$name: ")
      writer.write(value)
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      name: String = this.name,
      value: GQLValue = this.value,
  ): GQLArgument {
    return GQLArgument(
        sourceLocation = sourceLocation,
        name = name,
        value = value,
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        value = container.takeSingle()!!
    )
  }
}

@Deprecated("For brevity, GQLSelectionSet has been removed. Use `selections` directly")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
class GQLSelectionSet(
    val selections: List<GQLSelection>,
    override val sourceLocation: SourceLocation? = null,
) : GQLNode {
  override val children = selections

  override fun writeInternal(writer: SDLWriter) {
    selections.writeSelections(writer)
  }

  @Suppress("DEPRECATION")
  fun copy(
      selections: List<GQLSelection> = this.selections,
      sourceLocation: SourceLocation? = this.sourceLocation,
  ): GQLSelectionSet {
    return GQLSelectionSet(
        selections = selections,
        sourceLocation = sourceLocation
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        selections = container.take()
    )
  }
}

@Deprecated("For brevity, GQLArguments has been removed. Use `arguments` directly")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
class GQLArguments(
    val arguments: List<GQLArgument>,
    override val sourceLocation: SourceLocation? = null,
) : GQLNode {
  override val children: List<GQLNode> = arguments

  override fun writeInternal(writer: SDLWriter) {
    arguments.join(writer, prefix = "(", separator = ", ", postfix = ")")
  }

  @Suppress("DEPRECATION")
  fun copy(
      arguments: List<GQLArgument> = this.arguments,
      sourceLocation: SourceLocation? = this.sourceLocation,
  ): GQLArguments {
    return GQLArguments(
        arguments = arguments,
        sourceLocation = sourceLocation
    )
  }

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        arguments = container.take()
    )
  }
}

private fun List<GQLSelection>.writeSelections(writer: SDLWriter) {
  if (isEmpty()) {
    return
  }
  with(writer) {
    write("{\n")
    indent()
    join(writer, separator = "")
    unindent()
    write("}\n")
  }
}

private fun List<GQLArgument>.writeArguments(writer: SDLWriter) {
  if (isEmpty()) {
    return
  }
  join(writer, prefix = "(", separator = ", ", postfix = ")")
}

class GQLField @ApolloExperimental constructor(
    override val sourceLocation: SourceLocation? = null,
    val alias: String?,
    override val name: String,
    val arguments: List<GQLArgument>,
    override val directives: List<GQLDirective>,
    val selections: List<GQLSelection>,
) : GQLSelection(), GQLNamed, GQLHasDirectives {
  @Suppress("DEPRECATION")
  @Deprecated("Use selections directly")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  val selectionSet: GQLSelectionSet
    get() {
      return GQLSelectionSet(
          sourceLocation = null,
          selections = selections
      )
    }

  override val children: List<GQLNode> = selections + arguments + directives

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      if (alias != null) {
        write("$alias: ")
      }
      write(name)
      if (arguments.isNotEmpty()) {
        arguments.writeArguments(writer)
      }
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      if (selections.isNotEmpty()) {
        write(" ")
        selections.writeSelections(writer)
      } else {
        write("\n")
      }
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      alias: String? = this.alias,
      name: String = this.name,
      arguments: List<GQLArgument> = this.arguments,
      directives: List<GQLDirective> = this.directives,
      selections: List<GQLSelection> = this.selections,
  ) = GQLField(
      sourceLocation = sourceLocation,
      alias = alias,
      name = name,
      arguments = arguments,
      directives = directives,
      selections = selections,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        selections = container.take(),
        arguments = container.take(),
        directives = container.take(),
    )
  }
}

class GQLInlineFragment(
    override val sourceLocation: SourceLocation? = null,
    val typeCondition: GQLNamedType?,
    override val directives: List<GQLDirective>,
    val selections: List<GQLSelection>,
) : GQLSelection(), GQLHasDirectives {
  @Suppress("DEPRECATION")
  @Deprecated("Use selections directly")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  val selectionSet: GQLSelectionSet
    get() {
      return GQLSelectionSet(
          sourceLocation = null,
          selections = selections
      )
    }

  override val children = directives + selections + listOfNotNull(typeCondition)

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("...")
      if (typeCondition != null) {
        write(" on ${typeCondition.name}")
      }
      if (directives.isNotEmpty()) {
        write(" ")
        directives.join(writer)
      }
      if (selections.isNotEmpty()) {
        write(" ")
        selections.writeSelections(writer)
      }
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      typeCondition: GQLNamedType? = this.typeCondition,
      directives: List<GQLDirective> = this.directives,
      selections: List<GQLSelection> = this.selections,
  ) = GQLInlineFragment(
      sourceLocation = sourceLocation,
      typeCondition = typeCondition,
      directives = directives,
      selections = selections,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take(),
        selections = container.take(),
        typeCondition = container.takeSingle(),
    )
  }
}

class GQLFragmentSpread(
    override val sourceLocation: SourceLocation? = null,
    override val name: String,
    override val directives: List<GQLDirective>,
) : GQLSelection(), GQLNamed, GQLHasDirectives {

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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      name: String = this.name,
      directives: List<GQLDirective> = this.directives,
  ) = GQLFragmentSpread(
      sourceLocation = sourceLocation,
      name = name,
      directives = directives,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        directives = container.take()
    )
  }
}

sealed class GQLType : GQLNode

class GQLNamedType(
    override val sourceLocation: SourceLocation? = null,
    override val name: String,
) : GQLType(), GQLNamed {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write(name)
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      name: String = this.name,
  ) = GQLNamedType(
      sourceLocation = sourceLocation,
      name = name,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

class GQLNonNullType(
    override val sourceLocation: SourceLocation? = null,
    val type: GQLType,
) : GQLType() {

  override val children = listOf(type)

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      writer.write(type)
      write("!")
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      type: GQLType = this.type,
  ) = GQLNonNullType(
      sourceLocation = sourceLocation,
      type = type,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        type = container.takeSingle()!!
    )
  }
}

class GQLListType(
    override val sourceLocation: SourceLocation? = null,
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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      type: GQLType = this.type,
  ) = GQLListType(
      sourceLocation = sourceLocation,
      type = type,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        type = container.takeSingle()!!
    )
  }
}


sealed class GQLValue : GQLNode
class GQLVariableValue(
    override val sourceLocation: SourceLocation? = null,
    override val name: String,
) : GQLValue(), GQLNamed {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("${'$'}$name")
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      name: String = this.name,
  ) = GQLVariableValue(
      sourceLocation = sourceLocation,
      name = name,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

class GQLIntValue(
    override val sourceLocation: SourceLocation? = null,
    val value: String,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write(value.toString())
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      value: String = this.value,
  ) = GQLIntValue(
      sourceLocation = sourceLocation,
      value = value,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

class GQLFloatValue(
    override val sourceLocation: SourceLocation? = null,
    val value: String,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write(value)
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      value: String = this.value,
  ) = GQLFloatValue(
      sourceLocation = sourceLocation,
      value = value,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

class GQLStringValue(
    override val sourceLocation: SourceLocation? = null,
    val value: String,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("\"${value.encodeToGraphQLSingleQuoted()}\"")
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      value: String = this.value,
  ) = GQLStringValue(
      sourceLocation = sourceLocation,
      value = value,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

class GQLBooleanValue(
    override val sourceLocation: SourceLocation? = null,
    val value: Boolean,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write(value.toString())
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      value: Boolean = this.value,
  ) = GQLBooleanValue(
      sourceLocation = sourceLocation,
      value = value,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

class GQLEnumValue(
    override val sourceLocation: SourceLocation? = null,
    val value: String,
) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write(value)
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      value: String = this.value,
  ) = GQLEnumValue(
      sourceLocation = sourceLocation,
      value = value,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return this
  }
}

class GQLListValue(
    override val sourceLocation: SourceLocation? = null,
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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      values: List<GQLValue> = this.values,
  ) = GQLListValue(
      sourceLocation = sourceLocation,
      values = values,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        values = container.take()
    )
  }
}

class GQLObjectValue(
    override val sourceLocation: SourceLocation? = null,
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

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
      fields: List<GQLObjectField> = this.fields,
  ) = GQLObjectValue(
      sourceLocation = sourceLocation,
      fields = fields,
  )

  override fun copyWithNewChildrenInternal(container: NodeContainer): GQLNode {
    return copy(
        fields = container.take()
    )
  }
}

class GQLNullValue(override val sourceLocation: SourceLocation? = null) : GQLValue() {

  override val children = emptyList<GQLNode>()

  override fun writeInternal(writer: SDLWriter) {
    with(writer) {
      write("null")
    }
  }

  fun copy(
      sourceLocation: SourceLocation? = this.sourceLocation,
  ) = GQLNullValue(
      sourceLocation = sourceLocation,
  )

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
