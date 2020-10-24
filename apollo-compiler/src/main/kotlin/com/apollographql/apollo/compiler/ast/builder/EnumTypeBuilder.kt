package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.ast.EnumType
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.TypeDeclaration

internal fun TypeDeclaration.ast() = EnumType(
    name = name.capitalize().escapeKotlinReservedWord(),
    description = description,
    values = values.map { value ->
      EnumType.Value(
          constName = value.name.toUpperCase().escapeKotlinReservedWord(),
          value = value.name,
          description = value.description,
          deprecationReason = value.deprecationReason
      )
    }
)
