fun main() {
  /**
   * This just ensures the classes are built and accessible
   */
  // Classes from the schema
  println(com.service1.type.FieldInput::class.java)
  println(com.service1.type.FieldInput2::class.java)
  println(com.service2.type.FieldInput::class.java)
  println(com.service2.type.FieldInput2::class.java)

  // Fragments
  println(com.service1.fragment.QueryDetails::class.java)
  println(com.service2.fragment.QueryDetails::class.java)

  // Classes from this module
  println(com.service1.GetFieldQuery::class.java)
  println(com.service2.GetFieldQuery::class.java)
}