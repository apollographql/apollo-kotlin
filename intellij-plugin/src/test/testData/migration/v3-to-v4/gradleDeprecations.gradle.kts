dependencies {
  implementation("org.example:somelibrary:1.0.0")
}

apollo {
  generateModelBuilder.set(true)
  generateTestBuilders.set(true)
  languageVersion.set("1.4")
}

apollo {
  service("xxx") {
    generateModelBuilder.set(true)
    generateTestBuilders.set(true)
    languageVersion.set("1.4")
  }
}
