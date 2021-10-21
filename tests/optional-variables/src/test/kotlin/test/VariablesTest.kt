package test

import com.apollographql.apollo3.api.Optional
import optional.variables.DefaultValueParamsQuery
import optional.variables.NonNullableParamsQuery
import optional.variables.NullableParamsQuery
import org.junit.Test

class VariablesTest {
  @Test
  fun nullableVariablesAreOptional() {
    // By default, everything is absent. It is ok to omit everything
    NullableParamsQuery()
    // But we can pass arguments
    NullableParamsQuery(param1 = Optional.Present(0), param2 = Optional.Present(3.0))
    // Including null
    NullableParamsQuery(param1 = Optional.Present(null), param2 = Optional.Present(null))
  }

  @Test
  fun nonNullableVariablesAreNonOptional() {
    // This doesn't compile, we need to set params
    // NonNullableParamsQuery()
    // But we can pass arguments
    NonNullableParamsQuery(param1 = 0, param2 = 0.0)
    // But not null
    // NonNullableParamsQuery(param1 = null, param2 = null)
  }

  @Test
  fun variablesWithDefaultValuesAreOptional() {
    // By default, everything is absent. It is ok to omit everything
    DefaultValueParamsQuery()
    // But we can pass arguments
    DefaultValueParamsQuery(param1 = Optional.Present(0), param2 = Optional.Present(3.0))
    // Including null
    DefaultValueParamsQuery(param1 = Optional.Present(null), param2 = Optional.Present(3.0))
  }
}