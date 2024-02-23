configurePublishing()


val versionJson = tasks.register("versionJson") {
  inputs.property("version", findProperty("VERSION_NAME"))
  outputs.file(layout.buildDirectory.file("version.json"))

  doLast {
    outputs.files.singleFile.writeText("{\"version\":\"${inputs.properties.get("version")}\"}")
  }
}


val jar = tasks.register("javadocJar", Jar::class.java) {
  dependsOn(":dokkaHtmlMultiModule")

  from(rootProject.file("build/dokkaHtml/kdoc"))
  from(versionJson)
  archiveClassifier.set("javadoc")
}



extensions.getByType(PublishingExtension::class.java).publications.create("default", MavenPublication::class.java) {
  artifact(jar) {
    classifier = "javadoc"
  }
}