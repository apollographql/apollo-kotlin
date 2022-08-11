plugins {
  `embedded-kotlin`
}

group = "com.apollographql.apollo3.benchmark"

repositories {
  mavenCentral()
  google()
  maven {
    url = uri("../../build/localMaven")
  }
}

dependencies {
  compileOnly(gradleApi())
  implementation(libs.kotlin.plugin)
  implementation("com.squareup.okio:okio-jvm:3.2.0")
  implementation(libs.ksp)
  implementation("me.lucko:jar-relocator:1.5")

  if (true) {
    implementation(libs.apollo.plugin)
  } else {
    implementation("com.apollographql.apollo3:apollo-gradle-plugin:3.4.0")
  }
  implementation("androidx.benchmark:benchmark-gradle-plugin:1.1.0")
  implementation("com.android.tools.build:gradle:7.4.0-alpha09")
  implementation("org.ow2.asm:asm-commons:9.2")
}
