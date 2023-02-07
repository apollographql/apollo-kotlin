import kotlin.test.Test

class MainTest {
  @Test
  fun test() {
    listOf(
        "com.example.kotlin.type.FOO",
        "com.example.kotlin.type.Foo1",
        "com.example.kotlin.type.Foo2",
        "com.example.kotlin.type.Url1",
        "com.example.kotlin.type.URL",
        "com.example.java.type.FOO",
        "com.example.java.type.Foo1",
        "com.example.java.type.Foo2",
        "com.example.java.type.Url1",
        "com.example.java.type.URL",
    ).forEach {
      Class.forName(it)
    }
  }
}

