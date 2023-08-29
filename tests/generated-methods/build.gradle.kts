import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
}

apollo {
  service("kotlin19") {
    packageName.set("generatedMethods.kotlin19")
    generateDataBuilders.set(true)
    generateMethods.set(listOf("toString", "equalsHashCode", "copy"))
  }
}

//kotlin {
//  compilerOptions {
//    languageVersion.set(KotlinVersion.KOTLIN_1_9)
//    apiVersion.set(KotlinVersion.KOTLIN_1_9)
//  }
//}


tasks.withType(KotlinCompile::class.java).configureEach {
  kotlinOptions {
    apiVersion = "1.9"
    languageVersion = "1.9"
  }
}
