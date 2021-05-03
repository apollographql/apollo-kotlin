package com.apollographql.apollo3.api

/**
 * A list of [MergedField] for a given typeCondition
 *
 * @param type: the GraphQL type that has these fields. This is always a concrete type or null for the fallback [FieldSet] that
 * should be used for unknown (yet) types or types that don't fall into any of the others [FieldSet]
 */
class FieldSet(val type: String?, val mergedFields: Array<MergedField>)