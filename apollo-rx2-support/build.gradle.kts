apply(plugin = "java-library")
apply(plugin = "org.jetbrains.kotlin.jvm")

withConvention(JavaPluginConvention::class) {
  targetCompatibility = JavaVersion.VERSION_1_7
  sourceCompatibility = JavaVersion.VERSION_1_7
}

dependencies {
  add("compileOnly", groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))
  add("api", groovy.util.Eval.x(project, "x.dep.rx.java"))
  add("compileOnly", groovy.util.Eval.x(project, "x.dep.jetbrainsAnnotations"))
  add("compileOnly", project(":apollo-runtime"))
  add("compileOnly", project(":apollo-api"))
}

tasks.withType<Javadoc> {
  options.encoding = "UTF-8"
}

