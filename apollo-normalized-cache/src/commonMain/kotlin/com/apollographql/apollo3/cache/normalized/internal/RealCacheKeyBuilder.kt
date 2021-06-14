package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.CompiledArgument.Companion.resolveVariables
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.CompiledVariable
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.Utils
import okio.Buffer
import okio.IOException

class RealCacheKeyBuilder : CacheKeyBuilder {

  override fun build(field: CompiledField, variables: Executable.Variables) = field.nameWithArguments(variables)
}
