package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.compiler.escapeKotlinReservedWord

/**
 * This file contains GraphQL -> Kotlin transformations
 *
 * this is mostly empty right now but it'd be nice to centralize everything here so we can have a central place to
 * control name generation
 */
internal fun kotlinNameForEnumValue(graphqlEnumValue: String) = graphqlEnumValue.toUpperCase()
internal fun kotlinNameForEnum(graphqlEnum: String) = graphqlEnum.escapeKotlinReservedWord()
internal fun kotlinNameForField(responseName: String) = responseName.escapeKotlinReservedWord()
internal fun kotlinNameForAdapterField(responseName: String) = "${responseName.escapeKotlinReservedWord()}Adapter"