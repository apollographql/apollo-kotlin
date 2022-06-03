import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
}

dependencies {
  implementation(projects.apolloAnnotations)
  implementation(projects.apolloAst)
  api(projects.apolloCompiler)
  implementation(groovy.util.Eval.x(project, "x.dep.moshiMoshi"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshiSealedRuntime"))
  implementation(groovy.util.Eval.x(project, "x.dep.okHttpOkHttp"))

  implementation(groovy.util.Eval.x(project, "x.dep.moshiMoshi"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
}

tasks.withType(KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}