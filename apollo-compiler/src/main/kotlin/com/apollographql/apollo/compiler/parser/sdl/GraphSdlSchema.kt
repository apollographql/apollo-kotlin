package com.apollographql.apollo.compiler.parser.sdl

import com.apollographql.apollo.compiler.ir.SourceLocation
import com.apollographql.apollo.compiler.parser.error.ParseException
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.parser.sdl.GraphSDLSchemaParser.parse
import java.io.File

data class GraphSdlSchema(
    val schema: Schema,
    val typeDefinitions: Map<String, TypeDefinition>
) {

  data class Schema(
      val description: String?,
      val directives: List<Directive>,
      val queryRootOperationType: String,
      val mutationRootOperationType: String?,
      val subscriptionRootOperationType: String?
  )

  sealed class TypeDefinition {
    abstract val name: String
    abstract val description: String
    abstract val directives: List<Directive>

    data class Enum(
        override val name: String,
        override val description: String,
        override val directives: List<Directive>,
        val enumValues: List<Value>
    ) : TypeDefinition() {

      data class Value(
          val name: String,
          val description: String?,
          val directives: List<Directive>,
          val sourceLocation: SourceLocation
      )
    }

    data class Object(
        override val name: String,
        override val description: String,
        override val directives: List<Directive>,
        val fields: List<Field>,
        val interfaces: List<TypeRef.Named>
    ) : TypeDefinition()

    data class Interface(
        override val name: String,
        override val description: String,
        override val directives: List<Directive>,
        val fields: List<Field>
    ) : TypeDefinition()

    data class Field(
        val name: String,
        val description: String?,
        val directives: List<Directive>,
        val type: TypeRef,
        val arguments: List<Argument>,
        val sourceLocation: SourceLocation
    ) {
      data class Argument(
          val name: String,
          val description: String?,
          val directives: List<Directive>,
          val defaultValue: Any?,
          val type: TypeRef
      )
    }

    data class InputObject(
        override val name: String,
        override val description: String,
        override val directives: List<Directive>,
        val fields: List<InputField>
    ) : TypeDefinition()

    data class InputField(
        val name: String,
        val description: String?,
        val directives: List<Directive>,
        val defaultValue: Any?,
        val type: TypeRef,
        val sourceLocation: SourceLocation
    )

    data class Union(
        override val name: String,
        override val description: String,
        override val directives: List<Directive>,
        val typeRefs: List<TypeRef.Named>
    ) : TypeDefinition()


    data class Scalar(
        override val name: String,
        override val description: String,
        override val directives: List<Directive>
    ) : TypeDefinition()
  }

  data class Directive(
      val name: String,
      val arguments: Map<String, String>,
      val sourceLocation: SourceLocation
  )

  sealed class TypeRef {
    data class List(val typeRef: TypeRef) : TypeRef()

    data class NonNull(val typeRef: TypeRef) : TypeRef()

    data class Named(val typeName: String, val sourceLocation: SourceLocation) : TypeRef()
  }

  companion object {
    @JvmStatic
    @JvmName("parse")
    operator fun invoke(schemaFile: File): GraphSdlSchema {
      return schemaFile.parse().apply {
        validate()
      }
    }

    private fun GraphSdlSchema.validate() {
      typeDefinitions.values
          .filterIsInstance<TypeDefinition.Object>()
          .forEach { o  ->
        o.interfaces.forEach { i ->
          if (typeDefinitions.get(i.typeName) !is TypeDefinition.Interface) {
            throw ParseException(
                message = "Object `${o.name}` cannot implement non-interface `${i.typeName}`",
                sourceLocation = i.sourceLocation
            )
          }
        }
      }
    }
  }
}

fun GraphSdlSchema.toIntrospectionSchema(): IntrospectionSchema {
  return IntrospectionSchema(
      queryType = schema.queryRootOperationType,
      mutationType = schema.mutationRootOperationType,
      subscriptionType = schema.subscriptionRootOperationType,
      types = typeDefinitions.mapValues { (_, typeDefinition) ->
        when (typeDefinition) {
          is GraphSdlSchema.TypeDefinition.Enum -> typeDefinition.toIntrospectionType()
          is GraphSdlSchema.TypeDefinition.Object -> typeDefinition.toIntrospectionType(schema = this)
          is GraphSdlSchema.TypeDefinition.Interface -> typeDefinition.toIntrospectionType(schema = this)
          is GraphSdlSchema.TypeDefinition.InputObject -> typeDefinition.toIntrospectionType(schema = this)
          is GraphSdlSchema.TypeDefinition.Union -> typeDefinition.toIntrospectionType(schema = this)
          is GraphSdlSchema.TypeDefinition.Scalar -> typeDefinition.toIntrospectionType()
        }
      }
  )
}

private class DeprecateDirective(val reason: String?)

private fun GraphSdlSchema.TypeDefinition.Enum.toIntrospectionType(): IntrospectionSchema.Type.Enum {
  return IntrospectionSchema.Type.Enum(
      name = name,
      description = description,
      enumValues = enumValues.map { enumValue ->
        val deprecated = enumValue.directives.findDeprecatedDirective()
        IntrospectionSchema.Type.Enum.Value(
            name = enumValue.name,
            description = enumValue.description,
            isDeprecated = deprecated != null,
            deprecationReason = deprecated?.reason
        )
      }
  )
}

private fun GraphSdlSchema.TypeDefinition.Object.toIntrospectionType(schema: GraphSdlSchema): IntrospectionSchema.Type.Object {
  return IntrospectionSchema.Type.Object(
      name = name,
      description = description,
      fields = fields.map { field -> field.toIntrospectionType(schema) }
  )
}

private fun GraphSdlSchema.TypeDefinition.Interface.toIntrospectionType(schema: GraphSdlSchema): IntrospectionSchema.Type.Interface {
  return IntrospectionSchema.Type.Interface(
      name = name,
      description = description,
      fields = fields.map { field -> field.toIntrospectionType(schema) },
      possibleTypes = possibleTypes(schema)
  )
}

private fun GraphSdlSchema.TypeDefinition.Union.toIntrospectionType(schema: GraphSdlSchema): IntrospectionSchema.Type.Union {
  return IntrospectionSchema.Type.Union(
      name = name,
      description = description,
      fields = null,
      possibleTypes = typeRefs.map { typeRef -> typeRef.toIntrospectionType(schema) }
  )
}

private fun GraphSdlSchema.TypeDefinition.InputObject.toIntrospectionType(schema: GraphSdlSchema): IntrospectionSchema.Type.InputObject {
  return IntrospectionSchema.Type.InputObject(
      name = name,
      description = description,
      inputFields = fields.map { field -> field.toIntrospectionType(schema) }
  )
}

private fun GraphSdlSchema.TypeDefinition.Field.toIntrospectionType(schema: GraphSdlSchema): IntrospectionSchema.Field {
  val deprecated = directives.findDeprecatedDirective()
  return IntrospectionSchema.Field(
      name = name,
      description = description,
      isDeprecated = deprecated != null,
      deprecationReason = deprecated?.reason,
      type = type.toIntrospectionType(schema),
      args = arguments.map { argument -> argument.toIntrospectionType(schema) }
  )
}

private fun GraphSdlSchema.TypeDefinition.InputField.toIntrospectionType(schema: GraphSdlSchema): IntrospectionSchema.InputField {
  val deprecated = directives.findDeprecatedDirective()
  return IntrospectionSchema.InputField(
      name = name,
      description = description,
      isDeprecated = deprecated != null,
      deprecationReason = deprecated?.reason,
      type = type.toIntrospectionType(schema),
      defaultValue = defaultValue
  )
}

private fun GraphSdlSchema.TypeDefinition.Scalar.toIntrospectionType(): IntrospectionSchema.Type.Scalar {
  return IntrospectionSchema.Type.Scalar(
      name = name,
      description = description
  )
}

private fun GraphSdlSchema.TypeRef.toIntrospectionType(schema: GraphSdlSchema): IntrospectionSchema.TypeRef {
  return when (this) {
    is GraphSdlSchema.TypeRef.Named -> {
      IntrospectionSchema.TypeRef(
          kind = schema.typeDefinitions[typeName]?.toIntrospectionType() ?: throw ParseException(
              message = "Undefined GraphQL schema type `$typeName`",
              sourceLocation = sourceLocation
          ),
          name = typeName
      )
    }

    is GraphSdlSchema.TypeRef.NonNull -> IntrospectionSchema.TypeRef(
        name = null,
        kind = IntrospectionSchema.Kind.NON_NULL,
        ofType = typeRef.toIntrospectionType(schema)
    )

    is GraphSdlSchema.TypeRef.List -> IntrospectionSchema.TypeRef(
        name = null,
        kind = IntrospectionSchema.Kind.LIST,
        ofType = typeRef.toIntrospectionType(schema)
    )
  }
}

private fun GraphSdlSchema.TypeDefinition.toIntrospectionType(): IntrospectionSchema.Kind {
  return when (this) {
    is GraphSdlSchema.TypeDefinition.Enum -> IntrospectionSchema.Kind.ENUM
    is GraphSdlSchema.TypeDefinition.Object -> IntrospectionSchema.Kind.OBJECT
    is GraphSdlSchema.TypeDefinition.Interface -> IntrospectionSchema.Kind.INTERFACE
    is GraphSdlSchema.TypeDefinition.InputObject -> IntrospectionSchema.Kind.INPUT_OBJECT
    is GraphSdlSchema.TypeDefinition.Union -> IntrospectionSchema.Kind.UNION
    is GraphSdlSchema.TypeDefinition.Scalar -> IntrospectionSchema.Kind.SCALAR
  }
}

private fun GraphSdlSchema.TypeDefinition.Field.Argument.toIntrospectionType(schema: GraphSdlSchema): IntrospectionSchema.Field.Argument {
  val deprecated = directives.findDeprecatedDirective()
  return IntrospectionSchema.Field.Argument(
      name = name,
      description = description,
      isDeprecated = deprecated != null,
      deprecationReason = deprecated?.reason,
      type = type.toIntrospectionType(schema),
      defaultValue = defaultValue
  )
}

private fun List<GraphSdlSchema.Directive>.findDeprecatedDirective(): DeprecateDirective? {
  return find { directive -> directive.name == "deprecated" }?.let { directive ->
    DeprecateDirective(directive.arguments["reason"]?.removePrefix("\"")?.removeSuffix("\"") ?: "No longer supported")
  }
}

private fun GraphSdlSchema.TypeDefinition.Interface.possibleTypes(schema: GraphSdlSchema): List<IntrospectionSchema.TypeRef> {
  return schema.typeDefinitions.values
      .filter { typeDefinition ->
        typeDefinition is GraphSdlSchema.TypeDefinition.Object && typeDefinition.interfaces.firstOrNull { interfaceTypeRef ->
          interfaceTypeRef.typeName == name
        } != null
      }
      .map { typeDefinition ->
        IntrospectionSchema.TypeRef(
            kind = IntrospectionSchema.Kind.OBJECT,
            name = typeDefinition.name
        )
      }
}
