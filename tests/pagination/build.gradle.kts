plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo3")
}

apolloTest(
    withJs = false
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
        implementation(libs.atomicfu)
        implementation(libs.apollo.testingsupport)
        implementation(libs.apollo.normalizedcache.incubating)
        implementation(libs.apollo.normalizedcache.sqlite.incubating)
      }
    }
  }
}

apollo {
  service("embed") {
    packageName.set("embed")
    sourceFolder.set("embed")
  }

  service("pagination.offsetBasedWithArray") {
    packageName.set("pagination.offsetBasedWithArray")
    sourceFolder.set("pagination/offsetBasedWithArray")
    generateDataBuilders.set(true)
  }
  service("pagination.offsetBasedWithPage") {
    packageName.set("pagination.offsetBasedWithPage")
    sourceFolder.set("pagination/offsetBasedWithPage")
    generateDataBuilders.set(true)
  }
  service("pagination.cursorBased") {
    packageName.set("pagination.cursorBased")
    sourceFolder.set("pagination/cursorBased")
    generateDataBuilders.set(true)
  }
  service("pagination.connection") {
    packageName.set("pagination.connection")
    sourceFolder.set("pagination/connection")
    generateDataBuilders.set(true)
  }
}
