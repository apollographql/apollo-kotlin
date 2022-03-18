import kotlin.test.Test

class MainTest {
  @Test
  fun test() {

    val os = System.getProperty("os.name").lowercase()
    if (os.contains("mac")) {
      /**
       * Case insensitive filesystem
       */
      listOf(
          "com.example.kotlin.type.FOO",
          "com.example.kotlin.type.Foo1",
          "com.example.kotlin.type.foo2",
          "com.example.kotlin.type.Url1",
          "com.example.kotlin.type.URL",
          "com.example.java.type.FOO",
          "com.example.java.type.Foo1",
          "com.example.java.type.foo2",
          "com.example.java.type.Url1",
          "com.example.java.type.URL",
      )
    } else {
      /**
       * Case sensitive filesystem
       */
      listOf(
          "com.example.kotlin.type.FOO",
          "com.example.kotlin.type.Foo",
          "com.example.kotlin.type.foo",
          "com.example.kotlin.type.Url",
          "com.example.kotlin.type.URL",
          "com.example.java.type.FOO",
          "com.example.java.type.Foo",
          "com.example.java.type.foo",
          "com.example.java.type.Url",
          "com.example.java.type.URL",
      )
    }.forEach {
      Class.forName(it)
    }
  }
}

