package test

import kotlin.test.Test

class SchemaPackageNameTest {
  @Suppress("UNUSED_EXPRESSION")
  @Test
  fun userOnUserNameError() {
    com.example.type.Foo
    com.example.GetFooQuery
  }
}
