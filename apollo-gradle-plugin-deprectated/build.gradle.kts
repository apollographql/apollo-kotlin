apply(plugin = "groovy")
apply(plugin = "idea")
apply(plugin = "java-gradle-plugin")

withConvention(JavaPluginConvention::class) {
  sourceSets.named("main").get().java.srcDirs()
  sourceSets.named("main").get().withConvention(GroovySourceSet::class) {
    groovy.srcDirs("src/main/java", "src/main/groovy")
  }
}

configurations {
  create("fixtureClasspath")
}

dependencies {
  add("compileOnly", groovy.util.Eval.x(project, "x.dep.android.plugin"))
  add("compileOnly", gradleApi())

  add("implementation", localGroovy())
  add("implementation", project(":apollo-compiler"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.guavaJre"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.moshi.moshi"))

  add("testImplementation", groovy.util.Eval.x(project, "x.dep.android.plugin"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.junit"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.spock").toString()) {
    exclude( module= "groovy-all")
  }
  add("fixtureClasspath", groovy.util.Eval.x(project, "x.dep.android.plugin"))
}

// Inspired by: https://github.com/square/sqldelight/blob/83145b28cbdd949e98e87819299638074bd21147/sqldelight-gradle-plugin/build.gradle#L18
// Append any extra dependencies to the test fixtures via a custom configuration classpath. This
// allows us to apply additional plugins in a fixture while still leveraging dependency resolution
// and de-duplication semantics.
tasks.withType<PluginUnderTestMetadata> {
  getPluginClasspath().from(configurations.named("fixtureClasspath"))
}

apply {
  from(rootProject.file("gradle/gradle-mvn-push.gradle"))
}
apply {
  from(rootProject.file("gradle/bintray.gradle"))
}

tasks.withType<Test> {
  jvmArgs("-Xmx512m")
}
