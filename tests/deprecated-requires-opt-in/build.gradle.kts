plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
}

/**
 * Because of:
 *
 * w: file:///Users/mbonnin/git/apollo-kotlin/tests/deprecated-requires-opt-in/build/generated/source/apollo/custom/custom/adapter/GetNewFieldQuery_ResponseAdapter.kt:51:76 'oldField: Direction' is deprecated. No longer supported
 * w: file:///Users/mbonnin/git/apollo-kotlin/tests/deprecated-requires-opt-in/build/generated/source/apollo/default/default/adapter/GetNewFieldQuery_ResponseAdapter.kt:51:76 'oldField: Direction' is deprecated. No longer supported
 * w: file:///Users/mbonnin/git/apollo-kotlin/tests/deprecated-requires-opt-in/build/generated/source/apollo/none/none/adapter/GetNewFieldQuery_ResponseAdapter.kt:51:76 'oldField: Direction' is deprecated. No longer supported
 *
 * This is in generated code, the only way to remove these warnings is to remove the field usage which we can't do here so we just ignore warnings
 */
allWarningsAsErrors(false)

apollo {
  service("default") {
    srcDir("graphql")
    packageName.set("default")
    languageVersion.set("1.5")
  }
  service("none") {
    srcDir("graphql")
    requiresOptInAnnotation.set("none")
    packageName.set("none")
    languageVersion.set("1.5")
  }
  service("custom") {
    srcDir("graphql")
    requiresOptInAnnotation.set("com.example.MyRequiresOptIn")
    packageName.set("custom")
    languageVersion.set("1.5")
  }
}

