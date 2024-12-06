plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  service("kotlin") {
    packageName.set("com.example.kotlin")
  }
  service("java") {
    packageName.set("com.example.java")
    generateKotlinModels.set(false)
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
  }
}
