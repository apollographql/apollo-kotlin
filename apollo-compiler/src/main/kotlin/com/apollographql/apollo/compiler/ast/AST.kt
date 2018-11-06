package com.apollographql.apollo.compiler.ast

data class AST(
    val enums: List<EnumType>,
    val customTypes: Map<String, String>,
    val inputTypes: List<InputType>,
    val fragments: List<FragmentType>,
    val operations: List<OperationType>
) {
  data class OperationType(
      val name: String,
      val type: Type,
      val definition: String,
      val operationId: String,
      val queryDocument: String,
      val variables: InputType,
      val data: TypeRef,
      val nestedObjects: Map<TypeRef, ObjectType>,
      val filePath: String
  ) {
    enum class Type {
      QUERY, MUTATION, SUBSCRIPTION
    }
  }

  data class TypeRef(val name: String, val packageName: String = "")

  sealed class FieldType {

    sealed class Scalar : FieldType() {
      object String : Scalar()
      object Int : Scalar()
      object Boolean : Scalar()
      object Float : Scalar()
      data class Enum(val typeRef: TypeRef) : Scalar()
      data class Custom(
          val schemaType: kotlin.String,
          val mappedType: kotlin.String,
          val customEnumConst: kotlin.String,
          val customEnumType: TypeRef
      ) : Scalar()
    }

    data class Fragments(val name: String, val fields: List<Field>) : FieldType() {
      data class Field(
          val name: String,
          val type: TypeRef,
          val isOptional: Boolean
      )
    }

    data class Object(val typeRef: TypeRef) : FieldType()

    data class InlineFragment(val typeRef: TypeRef) : FieldType()

    data class Array(val rawType: FieldType) : FieldType()
  }

  data class ObjectType(
      val className: String,
      val schemaName: String,
      val fields: List<Field>,
      val fragmentsType: ObjectType?
  ) {
    data class Field(
        val name: String,
        val responseName: String,
        val schemaName: String,
        val type: FieldType,
        val description: String,
        val isOptional: Boolean,
        val isDeprecated: Boolean,
        val deprecationReason: String,
        val arguments: Map<String, Any>,
        val conditions: List<Condition>
    ) {
      sealed class Condition {
        data class Directive(val variableName: String, val inverted: Boolean) : Condition()
        data class Type(val type: String) : Condition()
      }
    }
  }

  data class EnumType(
      val name: String,
      val description: String,
      val values: List<Value>
  ) {
    data class Value(
        val constName: String,
        val value: String,
        val description: String,
        val isDeprecated: Boolean,
        val deprecationReason: String
    )
  }

  data class InputType(
      val name: String,
      val description: String,
      val fields: List<Field>
  ) {

    data class Field(
        val name: String,
        val schemaName: String,
        val type: FieldType,
        val description: String,
        val isOptional: Boolean,
        val defaultValue: Any?
    )
  }

  data class FragmentType(
      val name: String,
      val definition: String,
      val possibleTypes: List<String>,
      val fields: List<ObjectType.Field>,
      val nestedObjects: Map<TypeRef, ObjectType>
  )
}
