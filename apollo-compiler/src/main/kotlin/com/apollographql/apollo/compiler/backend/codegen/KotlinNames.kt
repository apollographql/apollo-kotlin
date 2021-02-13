package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.compiler.escapeKotlinReservedWord

/**
 * This file contains GraphQL -> Kotlin transformations
 */
internal fun kotlinNameForEnumValue(graphqlEnumValue: String) = graphqlEnumValue.toUpperCase()
internal fun kotlinNameForEnum(graphqlEnum: String) = graphqlEnum.escapeKotlinReservedWord()