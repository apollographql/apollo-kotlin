package com.apollographql.apollo.compiler

import kotlinx.serialization.Serializable

@Serializable
class UsedCoordinates {
  private val typeToFieldsToArguments = mutableMapOf<String, MutableMap<String, MutableSet<String>>>()

  fun putType(type: String) {
    val usedFields = typeToFieldsToArguments[type]
    if (usedFields == null) {
      typeToFieldsToArguments[type] = mutableMapOf()
    }
  }

  fun putField(type: String, field: String) {
    val usedFields = typeToFieldsToArguments[type]
    if (usedFields == null) {
      typeToFieldsToArguments[type] = mutableMapOf()
    }
    if (typeToFieldsToArguments[type]!![field] == null) {
      typeToFieldsToArguments[type]!![field] = mutableSetOf()
    }
  }

  fun putAllFields(type: String, fields: Map<String, Set<String>>) {
    val usedFields = typeToFieldsToArguments[type]
    if (usedFields == null) {
      typeToFieldsToArguments[type] = mutableMapOf()
    }
    for ((field, arguments) in fields) {
      if (typeToFieldsToArguments[type]!![field] == null) {
        typeToFieldsToArguments[type]!![field] = mutableSetOf()
      }
      typeToFieldsToArguments[type]!![field]!!.addAll(arguments)
    }
  }

  fun putArgument(type: String, field: String, argument: String) {
    val usedFields = typeToFieldsToArguments[type]
    if (usedFields == null) {
      typeToFieldsToArguments[type] = mutableMapOf()
    }
    if (typeToFieldsToArguments[type]!![field] == null) {
      typeToFieldsToArguments[type]!![field] = mutableSetOf()
    }
    typeToFieldsToArguments[type]!![field]!!.add(argument)
  }

  fun getTypes(): Set<String> {
    return typeToFieldsToArguments.keys
  }

  fun getFields(type: String): Map<String, Set<String>> {
    return typeToFieldsToArguments[type].orEmpty()
  }

  fun hasField(type: String, field: String): Boolean {
    return typeToFieldsToArguments[type]?.containsKey(field) ?: false
  }

  fun hasArgument(type: String, field: String, argument: String): Boolean {
    return typeToFieldsToArguments[type]?.get(field)?.contains(argument) ?: false
  }

  fun mergeWith(other: UsedCoordinates): UsedCoordinates {
    val newUsedCoordinates = UsedCoordinates()
    (typeToFieldsToArguments.entries + other.typeToFieldsToArguments.entries)
        .groupBy { it.key }
        .mapValues { (_, v) -> v.map { it.value } }
        .forEach { (type, fieldsList) ->
          val fields = fieldsList.reduce { acc, fields ->
            val newFields = mutableMapOf<String, MutableSet<String>>()
            (acc.entries + fields.entries)
                .groupBy { it.key }
                .mapValues { (_, v) -> v.map { it.value } }
                .forEach { (field, argumentsList) ->
                  val arguments = argumentsList.reduce { acc, arguments ->
                    (acc + arguments).toMutableSet()
                  }
                  newFields[field] = arguments
                }
            newFields
          }
          newUsedCoordinates.typeToFieldsToArguments[type] = fields
        }
    return newUsedCoordinates
  }

  fun asMap(): Map<String, Map<String, Set<String>>> = typeToFieldsToArguments
}

fun Map<String, Map<String, Set<String>>>.toUsedCoordinates(): UsedCoordinates {
  val usedCoordinates = UsedCoordinates()
  for ((type, fields) in this) {
    usedCoordinates.putType(type)
    for ((field, arguments) in fields) {
      usedCoordinates.putField(type, field)
      for (argument in arguments) {
        usedCoordinates.putArgument(type, field, argument)
      }
    }
  }
  return usedCoordinates
}

fun Set<String>.toUsedCoordinates(): UsedCoordinates {
  val usedCoordinates = UsedCoordinates()
  for (type in this) {
    usedCoordinates.putType(type)
  }
  return usedCoordinates
}
