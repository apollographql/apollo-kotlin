package test

import com.apollographql.apollo.api.Optional
import optional.variables.WithDirectiveDefaultValueParamsQuery
import optional.variables.WithDirectiveNonNullableParamsQuery
import optional.variables.WithDirectiveNullableParamsQuery
import optional.variables.WithoutDirectiveDefaultValueParamsQuery
import optional.variables.WithoutDirectiveNonNullableParamsQuery
import optional.variables.WithoutDirectiveNullableParamsQuery
import org.junit.Test

class VariablesTest {
  @Test
  fun withDirectiveNullableVariablesAreOptional() {
    // By default, everything is absent. It is ok to omit everything
    WithDirectiveNullableParamsQuery()
    // But we can pass arguments
    WithDirectiveNullableParamsQuery(param1 = Optional.Present(0), param2 = Optional.Present(3.0))
    // Including null
    WithDirectiveNullableParamsQuery(param1 = Optional.Present(null), param2 = Optional.Present(null))
  }

  @Test
  fun withDirectiveNonNullableVariablesAreNonOptional() {
    // This doesn't compile, we need to set params
    // NonNullableParamsQuery()
    // But we can pass arguments
    WithDirectiveNonNullableParamsQuery(param1 = 0, param2 = 0.0)
    // But not null
    // NonNullableParamsQuery(param1 = null, param2 = null)
  }

  @Test
  fun withDirectiveVariablesWithDefaultValuesAreOptional() {
    // By default, everything is absent. It is ok to omit everything
    WithDirectiveDefaultValueParamsQuery()
    // But we can pass arguments
    WithDirectiveDefaultValueParamsQuery(param1 = Optional.Present(0), param2 = Optional.Present(3.0))
    // Including null
    WithDirectiveDefaultValueParamsQuery(param1 = Optional.Present(null), param2 = Optional.Present(3.0))
  }


  @Test
  fun withoutDirective() {
    // No optional parameters, since generateOptionalOperationVariables is set to false
    WithoutDirectiveNullableParamsQuery(param1 = null, param2 = null)
    WithoutDirectiveNonNullableParamsQuery(param1 = 0, param2 = 0.0)

    // We still have optional in the presence of default values
    WithoutDirectiveDefaultValueParamsQuery()
    WithoutDirectiveDefaultValueParamsQuery(param1 = Optional.Present(0), param2 = Optional.Present(3.0))
    WithoutDirectiveDefaultValueParamsQuery(param1 = Optional.Present(null), param2 = Optional.Present(3.0))
  }
}
