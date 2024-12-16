
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

dependencies {
  add("implementation", libs.apollo.api)
  add("implementation", "com.fragments:fragments:1.0.0")
}

apollo {
  service("service1") {
    packageName.set("com.service1")
    dependsOn("com.fragments:fragments:1.0.0")
  }
  service("service2") {
    packageName.set("com.service2")
    dependsOn("com.fragments:fragments:1.0.0")
  }
}

