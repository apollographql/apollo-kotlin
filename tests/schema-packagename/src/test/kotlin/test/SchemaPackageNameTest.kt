package test

import kotlin.test.Test

class SchemaPackageNameTest {
  @Test
  fun userOnUserNameError() {
    com.example.type.Foo
    com.example.GetFooQuery
  }
}
