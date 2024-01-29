plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo3")
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
