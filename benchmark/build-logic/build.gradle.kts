plugins {
  `embedded-kotlin`
}

group = "com.apollographql.apollo3.benchmark"

apply(from = "../../gradle/dependencies.gradle")

repositories {
  mavenCentral()
  google()
  maven {
    url = uri("../../build/localMaven")
  }
}

dependencies {
  compileOnly(gradleApi())
  implementation(groovy.util.Eval.x(project, "x.dep.kotlinPlugin"))
  implementation("com.squareup.okio:okio-jvm:3.2.0")
  implementation(groovy.util.Eval.x(project, "x.dep.kspGradlePlugin"))
  implementation("me.lucko:jar-relocator:1.5")

  if (true) {
    implementation(groovy.util.Eval.x(project, "x.dep.apolloPlugin"))
  } else {
    implementation("com.apollographql.apollo3:apollo-gradle-plugin:3.4.0")
  }
  implementation("androidx.benchmark:benchmark-gradle-plugin:1.1.0")
  implementation("com.android.tools.build:gradle:7.4.0-alpha08")
  implementation("org.ow2.asm:asm-commons:9.2")
}
