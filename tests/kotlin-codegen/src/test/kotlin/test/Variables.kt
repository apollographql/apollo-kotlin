package test

import com.apollographql.apollo.api.Optional
import variables.NonNullVariableWithDefaultValueQuery
import variables.NullableVariableQuery
import variables.NullableVariableWithDefaultValueQuery
import variables.NullableVariableWithOptionalFalseDirectiveQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class VariablesTest {
  @Test
  fun nullableVariableDefaultsToOptional() {
    /**
     * By default, we generate the `Optional` so that users can omit parameters
     * It's possible to omit a variable, and it is possible to send null
     */
    assertEquals(Optional.Absent, NullableVariableQuery().param)
    assertEquals(Optional.Present(42), NullableVariableQuery(Optional.Present(42)).param)
    assertEquals(Optional.Present(null), NullableVariableQuery(Optional.Present(null)).param)
  }


  @Test
  fun nullableVariableWithOptionalFalseDirectivesDoesntGenerateOptional() {
    /**
     * If the user opted-out @optional, it's not possible to omit the variable
     */
    assertEquals(null, NullableVariableWithOptionalFalseDirectiveQuery(null).param)
    assertEquals(42, NullableVariableWithOptionalFalseDirectiveQuery(42).param)
  }

  @Test
  fun nullableVariableWithDefaultValueGeneratesAbsentInKotlin() {
    /**
     * If the variable has a default value, it always gets generated as Optional (opting-out is ignored)
     */
    assertEquals(Optional.Absent, NullableVariableWithDefaultValueQuery().param)
    assertEquals(Optional.Present(42), NullableVariableWithDefaultValueQuery(Optional.Present(42)).param)
    // Because the variable is nullable, it is possible to send null
    assertEquals(Optional.Present(null), NullableVariableWithDefaultValueQuery(Optional.Present(null)).param)
  }

  @Test
  fun nonnullVariableWithDefaultValueGeneratesAbsentInKotlin() {
    /**
     * If the variable has a default value, it always gets generated as Optional (opting-out is ignored)
     */
    assertEquals(Optional.Absent, NonNullVariableWithDefaultValueQuery().param)
    assertEquals(Optional.Present(42), NonNullVariableWithDefaultValueQuery(Optional.Present(42)).param)
    // Because the variable is nonnull, we cannot send null
  }
}
