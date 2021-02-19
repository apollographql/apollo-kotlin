plugins {
  kotlin("multiplatform")
}

configureMppDefaults(withJs = false)

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-runtime-common"))
        api(groovy.util.Eval.x(project, "x.dep.okio"))
        api(groovy.util.Eval.x(project, "x.dep.uuid"))
        api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
      }
    }

    val jvmMain by getting {
      dependencies {
        api(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
      }
    }

    val appleMain by getting {
      dependencies {
        api(project(":apollo-runtime-common"))
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(project(":apollo-testing-support"))
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test-junit"))
        implementation(groovy.util.Eval.x(project, "x.dep.truth"))
        implementation(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
      }
    }
  }
}

tasks.register("iOSSimTest") {
  dependsOn("iosSimTestBinaries")
  doLast {
    val binary = kotlin.targets.getByName<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>("iosSim").binaries.getTest("DEBUG").outputFile
    exec {
      commandLine = listOf("xcrun", "simctl", "spawn", "iPhone 8", binary.absolutePath)
    }
  }
}
