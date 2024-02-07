import kotlin.test.Test

class MainTest {
  @Test
  fun test() {
    println(multimodule1.root.fragment.QueryDetails::class.java)
    // A is used in the bidirectional module and registered automatically
    println(multimodule1.root.type.A::class.java)
    // B is not used at all and must not be generated
    try {
      val clazz = Class.forName("multimodule1.root.type.B")
      error("An exception was expected but got $clazz instead")
    } catch (e: ClassNotFoundException) {
    }
  }
}