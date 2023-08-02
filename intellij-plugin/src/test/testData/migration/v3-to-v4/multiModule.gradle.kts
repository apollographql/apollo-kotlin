dependencies {
  apolloMetadata(project(":schema"))
  implementation(project(":schema"))
}

apollo {
  service("service1") {
    packageName.set("com.example.service1")
  }

  service("service2") {
    packageName.set("com.example.service2")
  }
}


apollo {
  packageName.set("com.example.service1")
}
