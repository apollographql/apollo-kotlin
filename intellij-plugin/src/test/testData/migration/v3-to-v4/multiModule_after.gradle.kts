dependencies {
  implementation(project(":schema"))
}

apollo {
  service("service1") {
    packageName.set("com.example.service1")
    dependsOn(project(":schema"))
  }

  service("service2") {
    packageName.set("com.example.service2")
    dependsOn(project(":schema"))
  }
}


apollo {
  service("service") {
    packageName.set("com.example.service1")
    dependsOn(project(":schema"))
  }
}
