plugins {
  kotlin("jvm")
  id("application")
}

dependencies {
  implementation(kotlin("stdlib"))
  testImplementation(kotlin("test-junit"))

  implementation(project(":node1"))
  implementation(project(":node2"))
}

application {
  mainClass.set("LeafKt")
}
