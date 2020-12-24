package com.apollographql.apollo.compiler.backend.ast

internal typealias CustomScalarTypes = Map<String, CodeGenerationAst.CustomScalarType>

/**
 * Represents the minimum of possible AST for the code generator.
 *
 * This is the final phase in a chain of lowering process:
 *
 * GraphQL -->  FrontendIr --> BackendIr -> AST
 *
 * Rationale behind having this AST is that code generator doesn't need to do any special
 * transformations before generating models.
 */
internal data class CodeGenerationAst(
    val operationTypes: List<OperationType>,
    val fragmentTypes: List<FragmentType>,
    val inputTypes: List<InputType>,
    val enumTypes: List<EnumType>,
    val customScalarTypes: CustomScalarTypes
) {

  data class CustomScalarType(
      val name: String,
      val schemaType: String,
      val mappedType: String
  )

  data class OperationType(
      val name: String,
      val packageName: String,
      val type: Type,
      val operationName: String,
      val description: String,
      val operationId: String,
      val queryDocument: String,
      val variables: List<InputField>,
      val dataType: ObjectType,
  ) {
    enum class Type {
      QUERY, MUTATION, SUBSCRIPTION
    }
  }

  data class FragmentType(
      val name: String,
      val graphqlName: String,
      val description: String,
      val interfaceType: ObjectType,
      val defaultImplementationType: ObjectType,
      val fragmentDefinition: String,
      val typeRef: TypeRef,
  )

  data class ObjectType(
      val name: String,
      val description: String,
      val deprecationReason: String?,
      val fields: List<Field>,
      val nestedObjects: List<ObjectType>,
      val implements: Set<TypeRef>,
      val kind: Kind,
      val typeRef: TypeRef,
      val schemaTypename: String?,
      val fragmentAccessors: List<FragmentAccessor>,
  ) {
    val abstract: Boolean = kind == Kind.Interface || kind is Kind.Fragment

    data class FragmentAccessor(val name: String, val typeRef: TypeRef)

    sealed class Kind {
      object Interface : Kind()

      object Object : Kind()

      data class Fragment(
          val defaultImplementation: TypeRef,
          val possibleImplementations: Map<String, TypeRef>,
      ) : Kind()

      data class FragmentDelegate(val fragmentTypeRef: TypeRef) : Kind()
    }
  }

  data class Field(
      val name: String,
      val responseName: String,
      val schemaName: String,
      val type: FieldType,
      val description: String,
      val deprecationReason: String?,
      val arguments: Map<String, Any?>,
      val conditions: Set<Condition>,
      val override: Boolean
  ) {
    sealed class Condition {
      data class Directive(val variableName: String, val inverted: Boolean) : Condition()
    }
  }

  data class InputType(
      val graphqlName: String,
      val name: String,
      val description: String,
      val deprecationReason: String?,
      val fields: List<InputField>
  )

  data class InputField(
      val name: String,
      val schemaName: String,
      val deprecationReason: String?,
      val type: FieldType,
      val description: String,
      val defaultValue: Any?
  )

  data class EnumType(
      val graphqlName: String,
      val name: String,
      val description: String,
      val consts: List<EnumConst>
  )

  data class EnumConst(
      val constName: String,
      val value: String,
      val description: String,
      val deprecationReason: String?
  )

  sealed class FieldType {
    abstract val nullable: Boolean

    fun nonNullable(): FieldType {
      return when (this) {
        is Scalar.ID -> copy(nullable = false)
        is Scalar.String -> copy(nullable = false)
        is Scalar.Int -> copy(nullable = false)
        is Scalar.Boolean -> copy(nullable = false)
        is Scalar.Float -> copy(nullable = false)
        is Scalar.Enum -> copy(nullable = false)
        is Scalar.Custom -> copy(nullable = false)
        is Object -> copy(nullable = false)
        is Array -> copy(nullable = false)
      }
    }

    fun nullable(): FieldType {
      return when (this) {
        is Scalar.ID -> copy(nullable = true)
        is Scalar.String -> copy(nullable = true)
        is Scalar.Int -> copy(nullable = true)
        is Scalar.Boolean -> copy(nullable = true)
        is Scalar.Float -> copy(nullable = true)
        is Scalar.Enum -> copy(nullable = true)
        is Scalar.Custom -> copy(nullable = true)
        is Object -> copy(nullable = true)
        is Array -> copy(nullable = true)
      }
    }

    sealed class Scalar : FieldType() {
      data class ID(
          override val nullable: kotlin.Boolean,
          val type: kotlin.String,
          val customEnumType: TypeRef
      ) : Scalar()

      data class String(override val nullable: kotlin.Boolean) : Scalar()

      data class Int(override val nullable: kotlin.Boolean) : Scalar()

      data class Boolean(override val nullable: kotlin.Boolean) : Scalar()

      data class Float(override val nullable: kotlin.Boolean) : Scalar()

      data class Enum(
          override val nullable: kotlin.Boolean,
          val typeRef: TypeRef
      ) : Scalar()

      data class Custom(
          override val nullable: kotlin.Boolean,
          val schemaType: kotlin.String,
          val type: kotlin.String,
          val customEnumType: TypeRef
      ) : Scalar()
    }

    data class Object(
        override val nullable: Boolean,
        val typeRef: TypeRef
    ) : FieldType()

    data class Array(
        override val nullable: Boolean,
        val rawType: FieldType
    ) : FieldType()
  }

  data class TypeRef(
      val name: String,
      val packageName: String = "",
      val enclosingType: TypeRef? = null
  )

  companion object {
    fun customTypeRef(typesPackageName: String): TypeRef {
      return TypeRef(
          name = "CustomScalar",
          packageName = typesPackageName
      )
    }

    val typenameField = Field(
        name = "__typename",
        responseName = "__typename",
        schemaName = "__typename",
        type = FieldType.Scalar.String(nullable = false),
        description = "",
        deprecationReason = null,
        arguments = emptyMap(),
        conditions = emptySet(),
        override = false
    )
  }
}
