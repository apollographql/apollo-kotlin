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
  dependencies {
    implementation(groovy.util.Eval.x(project, "x.dep.kotlinPlugin"))
    implementation(groovy.util.Eval.x(project, "x.dep.kspGradlePlugin"))

    if (true) {
      implementation(groovy.util.Eval.x(project, "x.dep.apolloPlugin"))
    } else {
      implementation("com.apollographql.apollo3:apollo-gradle-plugin:3.4.0")
    }
    implementation("androidx.benchmark:benchmark-gradle-plugin:1.1.0")
    implementation("com.android.tools.build:gradle:7.4.0-alpha08")
  }
}
