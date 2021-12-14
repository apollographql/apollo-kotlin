plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.multiplatform")
}

configureMppTestsDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-runtime")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-testing-support")
        implementation("com.apollographql.apollo3:apollo-normalized-cache")
        implementation(groovy.util.Eval.x(project, "x.dep.turbine"))
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