package test

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

    SomeInputNone(newInputField = 9).newInputField
    SomeInputDefault(newInputField = 9).newInputField
    SomeInputCustom(newInputField = 9).newInputField

    GetNewFieldQueryNone.Data(newField = DirectionNone.NORTH).newField
    GetNewFieldQueryDefault.Data(newField = DirectionDefault.NORTH).newField
    GetNewFieldQueryCustom.Data(newField = DirectionCustom.NORTH).newField
  }
}
