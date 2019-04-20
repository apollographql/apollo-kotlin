package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.ast.*
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.normalizeJsonValue
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.Condition
import com.apollographql.apollo.compiler.ir.Field

internal fun Field.ast(context: Context): ObjectType.Field {
  return when {
    isArrayTypeField -> array(context)
    isObjectTypeField -> `object`(context)
    else -> scalar(context)
  }
}

private fun Field.scalar(context: Context): ObjectType.Field {
  return ObjectType.Field(
      name = responseName.decapitalize().escapeKotlinReservedWord(),
      responseName = responseName,
      schemaName = fieldName,
      type = resolveFieldType(
          graphQLType = type,
          enums = context.enums,
          customTypeMap = context.customTypeMap,
          typesPackageName = context.typesPackageName
      ),
      description = description ?: "",
      isOptional = !type.endsWith("!") || isConditional,
      isDeprecated = isDeprecated ?: false,
      deprecationReason = deprecationReason ?: "",
      arguments = args?.associate { it.name to it.value.normalizeJsonValue(it.type) } ?: emptyMap(),
      conditions = normalizedConditions
  )
}

private fun Field.array(context: Context): ObjectType.Field {
  val fieldType = if (fields?.isNotEmpty() == true) {
    val objectType = FieldType.Object(
        context.registerObjectType(
            type = responseName.replace("[", "").replace("[", "").replace("!", ""),
            schemaType = type.replace("[", "").replace("[", "").replace("!", ""),
            fragmentSpreads = fragmentSpreads ?: emptyList(),
            inlineFragments = inlineFragments ?: emptyList(),
            fields = fields
        )
    )
    var result: FieldType.Array = FieldType.Array(rawType = objectType)
    repeat(type.count { it == '[' } - 1) {
      result = FieldType.Array(rawType = result)
    }
    result
  } else {
    resolveFieldType(
        graphQLType = type,
        enums = context.enums,
        customTypeMap = context.customTypeMap,
        typesPackageName = context.typesPackageName
    )
  }
  return ObjectType.Field(
      name = responseName.decapitalize().escapeKotlinReservedWord(),
      responseName = responseName,
      schemaName = fieldName,
      type = fieldType,
      description = description ?: "",
      isOptional = !type.endsWith("!") || isConditional,
      isDeprecated = isDeprecated ?: false,
      deprecationReason = deprecationReason ?: "",
      arguments = args?.associate { it.name to it.value.normalizeJsonValue(it.type) } ?: emptyMap(),
      conditions = normalizedConditions
  )
}

private fun Field.`object`(context: Context): ObjectType.Field {
  val typeRef = context.registerObjectType(
      type = responseName.replace("[", "").replace("[", "").replace("!", ""),
      schemaType = type.replace("[", "").replace("[", "").replace("!", ""),
      fragmentSpreads = fragmentSpreads ?: emptyList(),
      inlineFragments = inlineFragments ?: emptyList(),
      fields = fields ?: emptyList()
  )
  return ObjectType.Field(
      name = responseName.decapitalize().escapeKotlinReservedWord(),
      responseName = responseName,
      schemaName = fieldName,
      type = FieldType.Object(typeRef),
      description = description ?: "",
      isOptional = !type.endsWith("!") || isConditional || (inlineFragments?.isNotEmpty() == true),
      isDeprecated = isDeprecated ?: false,
      deprecationReason = deprecationReason ?: "",
      arguments = args?.associate { it.name to it.value.normalizeJsonValue(it.type) } ?: emptyMap(),
      conditions = normalizedConditions
  )
}

private val Field.isObjectTypeField: Boolean
  get() = isNonScalar()

private val Field.isArrayTypeField: Boolean
  get() = type.removeSuffix("!").let { it.startsWith('[') && it.endsWith(']') }

private val Field.normalizedConditions: List<ObjectType.Field.Condition>
  get() {
    return if (isConditional) {
      conditions?.filter { it.kind == Condition.Kind.BOOLEAN.rawValue }?.map {
        ObjectType.Field.Condition.Directive(
            variableName = it.variableName,
            inverted = it.inverted
        )
      } ?: emptyList()
    } else {
      emptyList()
    }
  }