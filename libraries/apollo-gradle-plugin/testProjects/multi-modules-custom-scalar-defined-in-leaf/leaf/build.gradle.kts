plugins {
  id("org.jetbrains.kotlin.jvm")
  alias(libs.plugins.apollo)
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test.junit)

  implementation(project(":root"))
}

apollo {
  service("service") {
    packageName.set("com.library")
    mapScalar("Long", "java.lang.Long")
    /**
     * We need to call `dependsOn` here to know at graph build time that the schema is coming
     * from a dependency and do the checks for the redundant scalar configuration.
     *
     * TODO v5: add a specific `schema` block so that we can know this
     */
    dependsOn(project(":root"))
  }
}

