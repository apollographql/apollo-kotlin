plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
  configureMppDefaults(withJs = false)

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-runtime-kotlin")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-testing-support")
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(project(":sample-server"))
      }
    }
  }
}

apollo {
  file("src/commonMain/graphql").listFiles().filter {
    it.isDirectory
  }.forEach {
    service(it.name) {
      addGraphqlDirectory(it)
      rootPackageName.set(it.name.replace("-", "."))
    }
  }
}