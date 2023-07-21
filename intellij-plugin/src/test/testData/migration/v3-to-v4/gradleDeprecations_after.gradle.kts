dependencies {
  implementation("org.example:somelibrary:1.0.0")
}

apollo {
  service("service") {
    generateModelBuilders.set(true)
    generateDataBuilders.set(true)
  }
}

apollo {
  service("xxx") {
    generateModelBuilders.set(true)
    generateDataBuilders.set(true)
  }
}
