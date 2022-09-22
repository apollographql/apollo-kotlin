plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.api"))
  testImplementation(golatac.lib("kotlin.test.junit"))
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
