dependencies {
  implementation("org.example:somelibrary:1.0.0")
}

apollo {
  generateModelBuilder.set(true)
  generateTestBuilders.set(true)
  languageVersion.set("1.4")

  testDirConnection {
    // Make test builders available to main (not just test or androidTest) to be used by our mock data
    connectToAndroidSourceSet("main")
  }
}

apollo {
  service("xxx") {
    generateModelBuilder.set(true)
    generateTestBuilders.set(true)
    languageVersion.set("1.4")

    testDirConnection {
      // Make test builders available to main (not just test or androidTest) to be used by our mock data
      connectToAndroidSourceSet("main")
    }
  }
}
