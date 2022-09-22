plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.adapters)
  implementation(libs.guava.jre)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.apollo.mockserver)
  testImplementation(libs.apollo.testingsupport)
}

apollo {
  service("apollo") {
    packageName.set("optionals.apollo")
    generateKotlinModels.set(false)
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("apolloOptional")
    generateModelBuilder.set(true)
  }

  service("java") {
    packageName.set("optionals.java")
    generateKotlinModels.set(false)
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("javaOptional")
    generateModelBuilder.set(true)
  }

  service("guava") {
    packageName.set("optionals.guava")
    generateKotlinModels.set(false)
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("guavaOptional")
    generateModelBuilder.set(true)
  }
}
