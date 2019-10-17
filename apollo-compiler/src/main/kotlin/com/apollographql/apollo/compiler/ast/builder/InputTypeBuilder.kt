package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.ast.EnumType
import com.apollographql.apollo.compiler.ast.FieldType
import com.apollographql.apollo.compiler.ast.InputType
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.TypeDeclaration

internal fun TypeDeclaration.ast(
    enums: List<EnumType>,
    customTypeMap: Map<String, String>,
    typesPackageName: String
) = InputType(
    name = name.capitalize().escapeKotlinReservedWord(),
    description = description,
    fields = fields.map { field ->
      val inputFieldType = resolveFieldType(
          graphQLType = field.type,
          enums = enums,
          customTypeMap = customTypeMap,
          typesPackageName = typesPackageName
      )
      InputType.Field(
          name = field.name.decapitalize().escapeKotlinReservedWord(),
          schemaName = field.name,
          type = inputFieldType,
          description = field.description,
          isOptional = !field.type.endsWith("!"),
          defaultValue = if (inputFieldType.isCustomType) null else field.defaultValue
      )
    }
)

private val FieldType.isCustomType: Boolean
  get() = this is FieldType.Scalar.Custom || (this as? FieldType.Array)?.rawType?.isCustomType ?: false
