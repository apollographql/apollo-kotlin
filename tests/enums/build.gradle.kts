@file:OptIn(ApolloExperimental::class)

import com.apollographql.apollo.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.junit)
}

apollo {
  service("kotlin") {
    packageName.set("enums.kotlin")
    sealedClassesForEnumsMatching.set(listOf(".*avity", "FooSealed"))
  }

  service("java") {
    packageName.set("enums.java")
    classesForEnumsMatching.set(listOf(".*avity", "FooClass", "Color"))
    generateKotlinModels.set(false)
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
  }
  service("apollo") {
    packageName.set("enums.apollo")
    generateApolloEnums.set(true)
  }
}
