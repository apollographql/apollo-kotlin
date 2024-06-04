import com.apollographql.apollo3.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo3")
}

apolloTest(
    withJs = false,
    withJvm = false,
    appleTargets = setOf("iosArm64", "iosX64")
)

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
      }
    }
  }
}

apollo {
  service("service") {
    packageName.set("ios.test")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
  }
}
