import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
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


tasks.withType(KotlinCompilationTask::class.java).configureEach {
  compilerOptions {
    apiVersion.set(KotlinVersion.KOTLIN_1_9)
    languageVersion.set(KotlinVersion.KOTLIN_1_9)
  }
}
