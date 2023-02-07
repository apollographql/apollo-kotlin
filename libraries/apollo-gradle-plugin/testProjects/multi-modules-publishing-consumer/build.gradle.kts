
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

dependencies {
  add("implementation", libs.apollo.api)
  add("implementation", "com.jvm:jvm-producer:1.0.0")
}

apollo {
  service("jvm") {
    packageName.set("com.consumer")
    dependsOn("com.jvm:jvm-producer-apollo:1.0.0")
  }
  service("jvm2") {
    packageName.set("com.consumer2")
    dependsOn("com.jvm:jvm-producer-apollo:1.0.0")
  }
}

