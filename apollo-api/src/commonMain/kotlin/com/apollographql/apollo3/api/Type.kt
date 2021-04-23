package com.apollographql.apollo3.api


/**
 * Represents a mapping from a custom GraphQL scalar type to a Java/Kotlin class
 */
class CustomScalar(
    /**
     * GraphQL schema custom scalar type name (e.g. `ID`, `URL`, `DateTime` etc.)
     */
    name: String,

    /**
     * Fully qualified class name this GraphQL scalar type is mapped to (e.g. `java.lang.String`, `java.net.URL`, `java.util.DateTime`)
     */
    val className: String,
) : SchemaType(name)

class Object(
    name: String,
    vararg val implements: Interface,
) : SchemaType(name)

class Interface(
    name: String,
    vararg val implements: Interface,
) : SchemaType(name)

class Union(
    name: String,
    vararg val members: Object,
) : SchemaType(name)

sealed class SchemaType(val name: String) {
  override fun toString(): String {
    return when (this) {
      is CustomScalar -> "CustomScalar($name)"
      is Object -> "Object($name)"
      is Interface -> "Interface($name)"
      is Union -> "Union($name)"
    }
  }
}
private fun possibleTypesInternal(allTypes: List<SchemaType>, type: SchemaType): List<Object> {
  return when (type) {
    is Object -> listOf(type)
    is Union -> type.members.toList()
    is Interface -> {
      allTypes.flatMap { possibleImplementation ->
        when (possibleImplementation) {
          is Object -> {
            if (possibleImplementation.implements.any { it.name == type.name }) {
              possibleTypesInternal(allTypes, possibleImplementation)
            } else {
              emptyList()
            }
          }
          is Interface -> {
            if (possibleImplementation.implements.any { it.name == type.name }) {
              possibleTypesInternal(allTypes, possibleImplementation)
            } else {
              emptyList()
            }
          }
          else -> emptyList()
        }
      }
    }
    is CustomScalar -> error("Custom scalar can only have one possible type")
  }
}

fun possibleTypes(allTypes: List<SchemaType>, type: SchemaType): List<Object> {
  return possibleTypesInternal(allTypes, type).distinctBy { it.name }.sortedBy { it.name }
}
