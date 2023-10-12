import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
}

apollo {
  service("kotlin15") {
    packageName.set("enums.kotlin15")
    sealedClassesForEnumsMatching.set(listOf(".*avity", "FooSealed"))
    languageVersion.set("1.5")
  }

  service("kotlin19") {
    packageName.set("enums.kotlin19")
    sealedClassesForEnumsMatching.set(listOf(".*avity", "FooSealed"))
  }

  service("java") {
    packageName.set("enums.java")
    classesForEnumsMatching.set(listOf(".*avity", "FooClass"))
    generateKotlinModels.set(false)
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
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
