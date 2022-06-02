import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
}

dependencies {
  implementation(projects.apolloAnnotations)
  implementation(projects.apolloAst)
  api(projects.apolloCompiler)
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.sealedRuntime"))
  implementation(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))

  implementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
}

tasks.withType(KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}