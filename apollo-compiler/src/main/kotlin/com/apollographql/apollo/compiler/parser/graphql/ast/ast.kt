package com.apollographql.apollo.compiler.parser.graphql.ast

import com.apollographql.apollo.compiler.ir.SourceLocation
import okio.BufferedSink

interface GQLNode {
  val sourceLocation: SourceLocation
  fun write(bufferedSink: BufferedSink)
}

interface GQLNamed {
  val name: String
}

interface GQLDefinition : GQLNode
interface GQLTypeSystemExtension : GQLNode
interface GQLTypeExtension : GQLTypeSystemExtension, GQLNamed

interface GQLSelection : GQLNode

data class GQLDocument(val definitions: List<GQLDefinition>) : GQLNode {
  override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN

  override fun write(bufferedSink: BufferedSink) {
    definitions.join(bufferedSink = bufferedSink, separator = "\n\n")
  }

  companion object
}

data class GQLOperationDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val operationType: String,
    val name: String?,
    val variableDefinitions: List<GQLVariableDefinition>,
    val directives: List<GQLDirective>,
    val selections: List<GQLSelection>
) : GQLDefinition {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8(operationType)
      writeUtf8(" ")
      if (name != null) {
        writeUtf8(name)
        writeUtf8(" ")
        variableDefinitions.join(bufferedSink)
        writeUtf8(" ")
      }
      directives.join(bufferedSink)
      writeUtf8(" ")
      selections.join(bufferedSink, prefix = "{", separator = "\n", postfix = "\n}")
    }
  }
}

data class GQLFragmentDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val name: String,
    val directives: List<GQLDirective>,
    val typeCondition: GQLNamedType,
    val selections: List<GQLSelection>
) : GQLDefinition {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("fragment ")
      writeUtf8(name)
      writeUtf8(" ")
      writeUtf8(typeCondition.name)
      writeUtf8(" ")
      directives.join(bufferedSink)
      writeUtf8(" ")
      selections.join(bufferedSink, prefix = "{\n", separator = "\n", postfix = "\n}\n")
    }
  }
}

data class GQLSchemaDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String,
    val directives: List<GQLDirective>,
    val rootOperationTypeDefinitions: List<GQLOperationTypeDefinition>
) : GQLDefinition {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"$description\"\"\"\n")
      directives.join(bufferedSink)
      writeUtf8("schema ")
      rootOperationTypeDefinitions.join(bufferedSink, prefix = "{\n", separator = "\n", postfix = "\n}\n")
    }
  }

}

sealed class GQLTypeDefinition : GQLDefinition, GQLNamed
data class GQLInterfaceTypeDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    override val name: String,
    val implementsInterfaces: List<String>,
    val directives: List<GQLDirective>,
    val fields: List<GQLFieldDefinition>
) : GQLTypeDefinition() {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"$description\"\"\"\n")
      writeUtf8("interface $name ${implementsInterfaces.joinToString(" ")} ")
      directives.join(bufferedSink)
      fields.join(bufferedSink, prefix = "{\n", separator = "\n", postfix = "\n}\n")
    }

  }
}

data class GQLObjectTypeDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    override val name: String,
    val implementsInterfaces: List<String>,
    val directives: List<GQLDirective>,
    val fields: List<GQLFieldDefinition>,
) : GQLTypeDefinition() {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"$description\"\"\"\n")
      writeUtf8("type $name ${implementsInterfaces.joinToString(" ")} ")
      directives.join(bufferedSink)
      fields.join(bufferedSink, prefix = "{\n", separator = "\n", postfix = "\n}\n")
    }
  }
}

data class GQLInputObjectTypeDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    override val name: String,
    val directives: List<GQLDirective>,
    val inputFields: List<GQLInputValueDefinition>
) : GQLTypeDefinition() {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"$description\"\"\"\n")
      writeUtf8("input $name ")
      directives.join(bufferedSink)
      inputFields.join(bufferedSink, prefix = "{\n", separator = "\n", postfix = "\n}\n")
    }
  }
}

data class GQLScalarTypeDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    override val name: String,
    val directives: List<GQLDirective>
) : GQLTypeDefinition() {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"$description\"\"\"\n")
      writeUtf8("scalar $name ")
      directives.join(bufferedSink)
    }
  }
}

data class GQLEnumTypeDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    override val name: String,
    val directives: List<GQLDirective>,
    val enumValues: List<GQLEnumValueDefinition>
) : GQLTypeDefinition() {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"$description\"\"\"\n")
      writeUtf8("enum $name ")
      directives.join(bufferedSink)
      enumValues.join(bufferedSink, prefix = "{\n", separator = "\n", postfix = "\n}\n")
    }
  }
}

data class GQLUnionTypeDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    override val name: String,
    val directives: List<GQLDirective>,
    val memberTypes: List<GQLNamedType>
) : GQLTypeDefinition() {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"$description\"\"\"\n")
      writeUtf8("union $name ")
      directives.join(bufferedSink)
      writeUtf8(" = ")
      memberTypes.join(bufferedSink, separator = "|")
    }
  }
}

data class GQLDirectiveDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    val name: String,
    val arguments: List<GQLInputValueDefinition>,
    val repeatable: Boolean,
    val locations: List<String>
) : GQLDefinition {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"$description\"\"\"\n")
      writeUtf8("directive @$name ")
      arguments.join(bufferedSink, prefix = "(", separator = ", ", postfix = ")")
      if (repeatable) {
        writeUtf8("repeatable ")
      }
      writeUtf8("on ${locations.joinToString("|")}")
    }
  }
}

data class GQLSchemaExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val directives: List<GQLDirective>,
    val operationTypesDefinition: List<GQLOperationTypeDefinition>
) : GQLDefinition, GQLTypeSystemExtension {
  override fun write(bufferedSink: BufferedSink) {
    TODO("Not yet implemented")
  }
}

data class GQLEnumTypeExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val directives: List<GQLDirective>,
    val enumValues: List<GQLEnumValueDefinition>
) : GQLDefinition, GQLTypeExtension {
  override fun write(bufferedSink: BufferedSink) {
    TODO("Not yet implemented")
  }
}

data class GQLObjectTypeExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val directives: List<GQLDirective>,
    val fields: List<GQLFieldDefinition>
) : GQLDefinition, GQLTypeExtension {
  override fun write(bufferedSink: BufferedSink) {
    TODO("Not yet implemented")
  }
}

data class GQLInputObjectTypeExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val directives: List<GQLDirective>,
    val inputFields: List<GQLInputValueDefinition>
) : GQLDefinition, GQLTypeExtension {
  override fun write(bufferedSink: BufferedSink) {
    TODO("Not yet implemented")
  }
}

data class GQLScalarTypeExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val directives: List<GQLDirective>
) : GQLDefinition, GQLTypeExtension {
  override fun write(bufferedSink: BufferedSink) {
    TODO("Not yet implemented")
  }
}

data class GQLInterfaceTypeExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val fields: List<GQLFieldDefinition>
) : GQLDefinition, GQLTypeExtension, GQLNamed {
  override fun write(bufferedSink: BufferedSink) {
    TODO("Not yet implemented")
  }
}

data class GQLUnionTypeExtension(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val directives: List<GQLDirective>,
    val memberTypes: List<GQLNamedType>
) : GQLDefinition, GQLTypeExtension {
  override fun write(bufferedSink: BufferedSink) {
    TODO("Not yet implemented")
  }
}

data class GQLEnumValueDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    override val name: String,
    val directives: List<GQLDirective>
) : GQLNode, GQLNamed {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"$description\"\"\"\n")
      writeUtf8("$name ")
      directives.join(bufferedSink)
    }
  }
}

data class GQLFieldDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    override val name: String,
    val arguments: List<GQLInputValueDefinition>,
    val type: GQLType,
    val directives: List<GQLDirective>
) : GQLNode, GQLNamed {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"$description\"\"\"\n")
      writeUtf8("$name ")
      arguments.join(bufferedSink, prefix = "(", separator = ", ", postfix = ")")
      writeUtf8(": ")
      type.write(bufferedSink)
      directives.join(bufferedSink)
    }
  }
}

data class GQLInputValueDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val description: String?,
    override val name: String,
    val directives: List<GQLDirective>,
    val type: GQLType,
    val defaultValue: GQLValue?
) : GQLNode, GQLNamed {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      if (description != null) writeUtf8("\"\"\"$description\"\"\"\n")
      writeUtf8("$name: ")
      type.write(bufferedSink)
      if (defaultValue != null) {
        writeUtf8(" = ")
        defaultValue.write(bufferedSink)
        writeUtf8(" ")
      }
      directives.join(bufferedSink)
    }
  }
}

data class GQLVariableDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val name: String,
    val type: GQLType,
    val defaultValue: GQLValue?,
    val directives: List<GQLDirective>
) : GQLNode {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("${'$'}$name: ")
      type.write(bufferedSink)
      if (defaultValue != null) {
        writeUtf8(" = ")
        defaultValue.write(bufferedSink)
        writeUtf8(" ")
      }
      directives.join(bufferedSink)
    }
  }
}

data class GQLOperationTypeDefinition(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val operationType: String,
    val namedType: String
) : GQLNode {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("$operationType: $namedType")
    }
  }
}

data class GQLDirective(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String,
    val arguments: List<GQLArgument>
) : GQLNode, GQLNamed {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("@$name")
      arguments.join(bufferedSink, prefix = "(", separator = ",", postfix = ")")
    }
  }
}

data class GQLObjectField(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val name: String,
    val value: GQLValue
) : GQLNode {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("$name: ")
      value.write(bufferedSink)
    }
  }
}

data class GQLArgument(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val name: String,
    val value: GQLValue
) : GQLNode {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink) {
      writeUtf8("$name: ")
      value.write(bufferedSink)
    }
  }
}

data class GQLField(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val alias: String?,
    val name: String,
    val arguments: List<GQLArgument>,
    val directives: List<GQLDirective>,
    val selections: List<GQLSelection>
) : GQLSelection {
  override fun write(bufferedSink: BufferedSink) {
    with(bufferedSink){
      if (alias != null) {
        writeUtf8("$alias: ")
      }
      writeUtf8("$name ")
      arguments.join(bufferedSink, prefix = "(", separator = ", ", postfix = ")")
      writeUtf8(" ")
      directives.join(bufferedSink)
      selections.join(bufferedSink, prefix = "{\n", separator = "\n", postfix = "\n}")
    }
  }
}

data class GQLInlineFragment(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val typeCondition: GQLNamedType,
    val directives: List<GQLDirective>,
    val selectionSet: List<GQLSelection>
) : GQLSelection

data class GQLFragmentSpread(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val name: String,
    val directives: List<GQLDirective>
) : GQLSelection

sealed class GQLType : GQLNode
data class GQLNamedType(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    override val name: String
) : GQLType(), GQLNamed

data class GQLNonNullType(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val type: GQLType
) : GQLType()

data class GQLListType(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val type: GQLType
) : GQLType()


sealed class GQLValue : GQLNode
data class GQLVariableValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val name: String
) : GQLValue()

data class GQLIntValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: Int
) : GQLValue()

data class GQLFloatValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: Double
) : GQLValue()

data class GQLStringValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: String
) : GQLValue()

data class GQLBooleanValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: Boolean
) : GQLValue()

data class GQLEnumValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val value: String
) : GQLValue()

data class GQLListValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val values: List<GQLValue>
) : GQLValue()

data class GQLObjectValue(
    override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN,
    val fields: List<GQLObjectField>
) : GQLValue()

data class GQLNullValue(override val sourceLocation: SourceLocation = SourceLocation.UNKNOWN) : GQLValue()

private fun <T: GQLNode> List<T>.join(bufferedSink: BufferedSink, separator: String = " ", prefix: String = "", postfix: String = "") {
  if (isEmpty()) {
    return
  }
  bufferedSink.writeUtf8(prefix)
  forEachIndexed { index, t ->
    t.write(bufferedSink)
    if (index < size - 1) {
      bufferedSink.writeUtf8(separator)
    }
  }
  bufferedSink.writeUtf8(postfix)
}
