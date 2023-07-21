dependencies {
  implementation("org.example:somelibrary:1.0.0")
}

apollo {
  generateModelBuilder.set(true)
  generateTestBuilders.set(true)
}

apollo {
  service("xxx") {
    generateModelBuilder.set(true)
    generateTestBuilders.set(true)
  }
}
