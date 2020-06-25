package com.apollographql.apollo.compiler.parser.sdl

data class GraphSdlSchema(
    val schema: Schema,
    val typeDefinitions: Map<String, TypeDefinition>
) {

  data class Schema(
      val description: String?,
      val directives: List<Directive>,
      val queryRootOperationType: TypeRef.Named,
      val mutationRootOperationType: TypeRef.Named,
      val subscriptionRootOperationType: TypeRef.Named
  )

  sealed class TypeDefinition {
    abstract val name: String
    abstract val description: String?
    abstract val directives: List<Directive>

    data class Enum(
        override val name: String,
        override val description: String?,
        override val directives: List<Directive>,
        val enumValues: List<Value>
    ) : TypeDefinition() {

      data class Value(
          val name: String,
          val description: String?,
          val directives: List<Directive>
      )
    }

    data class Object(
        override val name: String,
        override val description: String?,
        override val directives: List<Directive>,
        val fields: List<Field>,
        val interfaces: List<TypeRef.Named>
    ) : TypeDefinition()

    data class Interface(
        override val name: String,
        override val description: String?,
        override val directives: List<Directive>,
        val fields: List<Field>
    ) : TypeDefinition()

    data class Field(
        val name: String,
        val description: String?,
        val directives: List<Directive>,
        val type: TypeRef,
        val arguments: List<Argument>
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
        override val description: String?,
        override val directives: List<Directive>,
        val fields: List<InputField>
    ) : TypeDefinition()

    data class InputField(
        val name: String,
        val description: String?,
        val directives: List<Directive>,
        val defaultValue: Any?,
        val type: TypeRef
    )

    data class Union(
        override val name: String,
        override val description: String?,
        override val directives: List<Directive>,
        val typeRefs: List<TypeRef.Named>
    ) : TypeDefinition()


    data class Scalar(
        override val name: String,
        override val description: String?,
        override val directives: List<Directive>
    ) : TypeDefinition()
  }

  data class Directive(
      val name: String,
      val arguments: Map<String, String>
  )

  sealed class TypeRef {
    data class List(val typeRef: TypeRef) : TypeRef()

    data class NonNull(val typeRef: TypeRef) : TypeRef()

    data class Named(val typeName: String) : TypeRef()
  }
}
