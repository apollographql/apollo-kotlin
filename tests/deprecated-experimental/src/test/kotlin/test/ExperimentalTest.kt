package test

import com.apollographql.apollo3.api.Optional
import none.GetNewFieldQuery as GetNewFieldQueryNone
import default.GetNewFieldQuery as GetNewFieldQueryDefault
import custom.GetNewFieldQuery as GetNewFieldQueryCustom

import none.type.Direction as DirectionNone
import default.type.Direction as DirectionDefault
import custom.type.Direction as DirectionCustom

import none.type.SomeInput as SomeInputNone
import default.type.SomeInput as SomeInputDefault
import custom.type.SomeInput as SomeInputCustom

import org.junit.Test

class ExperimentalTest {
  @Test
  fun kotlinEnums() {

    SomeInputNone(
        newInputField = Optional.Present(9),
        oldInputField = Optional.Present(0),
    ).apply {
      oldInputField
      newInputField
    }

    SomeInputDefault(
        newInputField = Optional.Present(9),
        oldInputField = Optional.Present(0),
    ).apply {
      oldInputField
      newInputField
    }

    SomeInputCustom(
        newInputField = Optional.Present(9),
        oldInputField = Optional.Present(0),
    ).apply {
      oldInputField
      newInputField
    }

    GetNewFieldQueryNone.Data(
        newField = DirectionNone.NORTH,
        oldField = DirectionNone.SOUTH
    ).apply {
      oldField
      newField
    }
    GetNewFieldQueryDefault.Data(
        newField = DirectionDefault.NORTH,
        oldField = DirectionDefault.SOUTH
    ).apply {
      oldField
      newField
    }
    GetNewFieldQueryCustom.Data(
        newField = DirectionCustom.NORTH,
        oldField = DirectionCustom.SOUTH
    ).apply {
      oldField
      newField
    }
  }
}
