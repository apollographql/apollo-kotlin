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
    sourceFolder.set("mockserver")
    packageName.set("multipart")
  }
  service("router") {
    sourceFolder.set("router")
    packageName.set("router")
  }
}
