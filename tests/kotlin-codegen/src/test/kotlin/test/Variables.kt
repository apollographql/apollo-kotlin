package test

import com.apollographql.apollo3.api.Optional
import variables.NonNullVariableWithDefaultValueQuery
import variables.NullableVariableQuery
import variables.NullableVariableWithDefaultValueQuery
import variables.NullableVariableWithOptionalDirectiveQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class VariablesTest {
  @Test
  fun nullableVariableDefaultsToNonOptional() {
    /**
     * By default, we remove the `Optional` so that users have to input a parameter
     * It's not possible to omit a variable. But it is possible to send null
     */
    assertEquals(42, NullableVariableQuery(42).param)
    assertEquals(null, NullableVariableQuery(null).param)
  }

  @Test
  fun nullableVariableWithOptionalDirectivesGeneratesOptional() {
    /**
     * If the user opted-in @optional, it's possible to omit the variable again
     */
    assertEquals(Optional.Absent, NullableVariableWithOptionalDirectiveQuery().param)
    assertEquals(Optional.Present(42), NullableVariableWithOptionalDirectiveQuery(Optional.Present(42)).param)
    assertEquals(Optional.Present(null), NullableVariableWithOptionalDirectiveQuery(Optional.Present(null)).param)
  }

  @Test
  fun nullableVariableWithDefaultValueGeneratesAbsentInKotlin() {
    /**
     * If the variable has a default value, it always get generated as Optional
     */
    assertEquals(Optional.Absent, NullableVariableWithDefaultValueQuery().param)
    assertEquals(Optional.Present(42), NullableVariableWithDefaultValueQuery(Optional.Present(42)).param)
    // Because the variable is nullable, it is possible to send null
    assertEquals(Optional.Present(null), NullableVariableWithDefaultValueQuery(Optional.Present(null)).param)
  }

  @Test
  fun nonnullVariableWithDefaultValueGeneratesAbsentInKotlin() {
    /**
     * If the variable has a default value, it always gets generated as Optional
     */
    assertEquals(Optional.Absent, NonNullVariableWithDefaultValueQuery().param)
    assertEquals(Optional.Present(42), NonNullVariableWithDefaultValueQuery(Optional.Present(42)).param)
    // Because the variable is nonnull, we cannot send null
  }
}