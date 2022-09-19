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
  service("kotlin") {
    sourceFolder.set("kotlin")
    packageName.set("enums.kotlin")
    sealedClassesForEnumsMatching.set(listOf(".*avity", "FooSealed"))
  }

  service("java") {
    sourceFolder.set("java")
    packageName.set("enums.java")
    generateKotlinModels.set(false)
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    classesForEnumsMatching.set(listOf(".*avity", "FooClass"))
  }
}
