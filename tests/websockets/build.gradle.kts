plugins {
  id(libs.plugins.apollo.get().toString())
  id(libs.plugins.kotlin.multiplatform.get().toString())
}

configureMppTestsDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.apollo.runtime)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.apollo.testingSupport)
        implementation(libs.apollo.normalizedCache)
        implementation(libs.turbine)
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(projects.sampleServer)
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
