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
        implementation(libs.apollo.mockserver)
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
      if (it.name == "sample-server") {
        schemaFiles.from(file("../sample-server/src/main/resources/schema.graphqls"))
      }
      srcDir(it)
      packageName.set(it.name.replace("-", "."))
    }
  }
}
