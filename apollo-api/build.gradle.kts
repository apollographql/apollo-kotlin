apply(plugin = "java")
apply(plugin = "kotlin")

withConvention(JavaPluginConvention::class) {
  targetCompatibility = JavaVersion.VERSION_1_7
  sourceCompatibility = JavaVersion.VERSION_1_7
}

dependencies {
  add("compileOnly", groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))
  add("compileOnly", groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))

  add("implementation", groovy.util.Eval.x(project, "x.dep.jetbrainsAnnotations"))

  add("testImplementation", groovy.util.Eval.x(project, "x.dep.junit"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.truth"))
}

apply {
  from(rootProject.file("gradle/gradle-mvn-push.gradle"))
}
apply {
  from(rootProject.file("gradle/bintray.gradle"))
}
