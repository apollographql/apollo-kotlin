#!/usr/bin/env kotlin


import java.io.File

println("HOME: ${System.getenv("HOME")}")

val coreDir = File("/Users/runner/Library/Logs/DiagnosticReports/")

if (!coreDir.exists()) {
  println("${coreDir.absolutePath} does not exists")
} else {
  println("${coreDir.absolutePath}:")
  coreDir.walk().forEach {
    println("- ${it.absolutePath}")
  }
}