plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

apolloTest {
  mpp {}
}

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
        implementation(libs.apollo.normalizedcache)
        implementation(libs.turbine)
      }
    }

    findByName("jvmTest")?.apply {
      dependencies {
        implementation(project(":sample-server"))
      }
    }
  }
}

apollo {
  file("src/commonMain/graphql").listFiles()?.filter {
    it.isDirectory
  }?.forEach {
    service(it.name) {
      srcDir(it)
      packageName.set(it.name.replace("-", "."))
    }
  }
}
