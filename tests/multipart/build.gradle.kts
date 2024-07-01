plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo")
}

apolloTest()

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        implementation(libs.apollo.runtime)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(libs.apollo.testingsupport)
        implementation(libs.apollo.mockserver)
        implementation(libs.turbine)
      }
    }
  }
}


apollo {
  service("mockserver") {
    srcDir("src/commonMain/graphql/mockserver")
    packageName.set("multipart")
  }
  service("router") {
    srcDir("src/commonMain/graphql/router")
    packageName.set("router")
  }
}
