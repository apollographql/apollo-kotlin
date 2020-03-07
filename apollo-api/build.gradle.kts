plugins {
  `java-library`
  kotlin("jvm")
}

java {
  targetCompatibility = JavaVersion.VERSION_1_8
  sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  add("compileOnly", groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))
  add("compileOnly", groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))

  add("api", groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))
  add("api", groovy.util.Eval.x(project, "x.dep.okio"))

  add("testImplementation", groovy.util.Eval.x(project, "x.dep.junit"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.truth"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
}

tasks.withType<Checkstyle> {
  exclude("**/BufferedSourceJsonReader.java")
  exclude("**/JsonScope.java")
  exclude("**/JsonUtf8Writer.java")
}
