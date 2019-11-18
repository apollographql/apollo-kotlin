apply(plugin = "kotlin")

dependencies {
  add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))

  add("testImplementation", groovy.util.Eval.x(project, "x.dep.junit"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.truth"))
}

apply {
  from(rootProject.file("gradle/gradle-mvn-push.gradle"))
}

apply {
  from(rootProject.file("gradle/bintray.gradle"))
}
