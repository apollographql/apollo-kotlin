package com.example

fun main() {
  // Can reference the main source set
  println(com.example.MainQuery::class.java.name)
  // Can reference the debug source set
  println(com.example.DebugQuery::class.java.name)
  // Can reference the demoDebug source set
  println(com.example.DemoDebugQuery::class.java.name)
}