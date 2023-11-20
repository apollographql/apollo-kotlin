package com.apollographql.apollo3.execution

import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLSelection

class MergedField(val first: GQLField, val selections: List<GQLSelection>)