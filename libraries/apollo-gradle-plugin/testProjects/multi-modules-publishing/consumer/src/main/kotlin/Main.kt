fun main() {
  /**
   * This just ensures the classes are built and accessible
   */
  // Classes from the dependency
  println(com.jvm.type.FieldInput::class.java)
  println(com.jvm.type.FieldInput2::class.java)
  println(com.jvm2.type.FieldInput::class.java)

  // Classes from this module
  println(com.consumer.GetField2Query::class.java)
  println(com.consumer2.GetField2Query::class.java)
}