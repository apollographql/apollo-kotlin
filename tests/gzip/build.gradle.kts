import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

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
    val commonMain by getting {
      dependencies {
        implementation(golatac.lib("apollo.runtime"))
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(golatac.lib("apollo.testingsupport"))
      }
    }
  }
}

apollo {
  service("service") {
    packageName.set("gzip")
  }
}
