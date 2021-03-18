package com.apollographql.apollo3.compiler.backend.ast

import com.apollographql.apollo3.compiler.unified.AstExtFragmentImplementation
import com.apollographql.apollo3.compiler.unified.AstExtFragmentInterfaces
import com.apollographql.apollo3.compiler.unified.AstExtInterface
import com.apollographql.apollo3.compiler.unified.AstExtOperation

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
    val customScalarTypes: CustomScalarTypes,
    val extOperations: List<AstExtOperation> = emptyList(),
    val extFragmentInterfaces: List<AstExtFragmentInterfaces> = emptyList(),
    val extFragmentImplementations: List<AstExtFragmentImplementation> = emptyList(),
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
      val description: String,
      val interfaceType: ObjectType,
      val implementationType: ObjectType,
      val fragmentDefinition: String,
      val variables: List<InputField>,
  )

  /**
   * @param isShape: true if this is a one of the shapes a polymorphic field
   */
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
      val isShape: Boolean,
      val abstract: Boolean
  ) {
    data class FragmentAccessor(val name: String, val typeRef: TypeRef)

    sealed class Kind {
      object Interface : Kind()

      object Object : Kind()

      data class ObjectWithFragments(
          val defaultImplementation: TypeRef?,
          val possibleImplementations: List<FragmentImplementation>,
      ) : Kind()

      data class FragmentDelegate(val fragmentTypeRef: TypeRef) : Kind()
    }

    data class FragmentImplementation(
        val typeConditions: List<String>,
        val typeRef: TypeRef
    )
  }

  /**
   * @param requiresBuffering: true if this is a polymorphic field that will use synthetic fragments
   * that require a [JsonReader]/[JsonWriter] that can buffer.
   */
  data class Field(
      val name: String,
      val responseName: String,
      val schemaName: String,
      val type: FieldType,
      val description: String,
      val deprecationReason: String?,
      val arguments: Map<String, Any?>,
      val conditions: Set<Condition>,
      val override: Boolean,
      val requiresBuffering: Boolean
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

  /**
   * Represents either an input field or a variable
   *
   */
  data class InputField(
      val name: String,
      val schemaName: String,
      val deprecationReason: String?,
      val type: FieldType,
      val description: String,
      val isRequired: Boolean
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
        is InputObject -> copy(nullable = false)
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
        is InputObject -> copy(nullable = true)
        is Array -> copy(nullable = true)
      }
    }

    /**
     * A Scalar Kotlin type. For the purpose of codegen, scalar has a slightly different meaning than in GraphQL.
     * Kotlin scalars include enums for which we will not generate a query-scoped class.
     */
    sealed class Scalar : FieldType() {
      /**
       * The GraphQL type name as it appears in the schema.
       * Example: for a 'friend: Character' field, schemaTypeName will be "Character"
       */
      abstract val schemaTypeName: kotlin.String

      data class String(override val nullable: kotlin.Boolean) : Scalar() {
        override val schemaTypeName = "String"
      }

      data class Int(override val nullable: kotlin.Boolean) : Scalar() {
        override val schemaTypeName = "Int"
      }

      data class Float(override val nullable: kotlin.Boolean) : Scalar() {
        override val schemaTypeName = "Float"
      }

      data class Boolean(override val nullable: kotlin.Boolean) : Scalar() {
        override val schemaTypeName = "Boolean"
      }

      data class ID(
          override val nullable: kotlin.Boolean,
          val type: kotlin.String,
      ) : Scalar() {
        override val schemaTypeName = "ID"
      }

      data class Enum(
          override val nullable: kotlin.Boolean,
          override val schemaTypeName: kotlin.String,
          val typeRef: TypeRef
      ) : Scalar()

      data class Custom(
          override val nullable: kotlin.Boolean,
          override val schemaTypeName: kotlin.String,
          val type: kotlin.String,
          val typeRef: TypeRef,
      ) : Scalar()
    }

    data class InputObject(
        override val nullable: Boolean,
        val schemaTypeName: kotlin.String,
        val typeRef: TypeRef
    ) : FieldType()

    data class Object(
        override val nullable: Boolean,
        val schemaTypeName: kotlin.String,
        val typeRef: TypeRef
    ) : FieldType()

    data class Array(
        override val nullable: Boolean,
        val rawType: FieldType
    ) : FieldType() {
      val leafType: FieldType
        get() = if (rawType is Array) rawType.leafType else rawType
    }
  }

  /**
   * A type reference
   */
  data class TypeRef(
      /**
       * The package name
       */
      val packageName: String = "",
      /**
       * The enclosing type reference if it exists or null if it is a top level type
       */
      val enclosingType: TypeRef? = null,
      /**
       * The Kotlin name, this is usually the responseName from the query in upper camel case
       * but can escaped for keywords/invalid chars if needed.
       * Example: for a 'friend: Character' field, name will be "Friend"
       */
      val name: String,
      /**
       * Indicates if this is the reference to named fragment 'Data' type
       */
      val isNamedFragmentDataRef: Boolean = false
  )

  companion object {
    val typenameField = Field(
        name = "__typename",
        responseName = "__typename",
        schemaName = "__typename",
        type = FieldType.Scalar.String(nullable = false),
        description = "",
        deprecationReason = null,
        arguments = emptyMap(),
        conditions = emptySet(),
        override = false,
        requiresBuffering = false
    )
  }
}
