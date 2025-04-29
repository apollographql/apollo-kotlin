pluginManagement {
  includeBuild("../build-logic")
}

plugins {
  id("com.gradle.develocity") version "4.0.1" // sync with libraries.toml
  id("com.gradle.common-custom-user-data-gradle-plugin") version "2.2.1"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "apollo-tests"

listOf(
    "ast-benchmark",
    "browser-tests",
    "cache-variables-arguments",
    "catch",
    "compiler-plugins/add-field",
    "compiler-plugins/app",
    "compiler-plugins/capitalize-enum-values",
    "compiler-plugins/custom-flatten",
    "compiler-plugins/default-null-values",
    "compiler-plugins/getters-and-setters",
    "compiler-plugins/prefix-names",
    "compiler-plugins/schema-codegen",
    "compiler-plugins/typename-interface",
    "data-builders-java",
    "data-builders-kotlin",
    "defer",
    "deprecated-requires-opt-in",
    "enums",
    "escaping",
    "filesystem-sensitivity",
    "generated-methods",
    "gzip",
    "http-cache",
    "http-headers",
    "include-skip-operation-based",
    "input",
    "integration-tests",
    "intellij-plugin-test-project",
    "ios-test",
    "java-nullability",
    "js",
    "jsexport",
    "jvmoverloads",
    "kdoc",
    "kotlin-codegen",
    "model-builders-java",
    "models-operation-based",
    "models-operation-based-with-interfaces",
    "models-response-based",
    "multi-module-1/bidirectional",
    "multi-module-1/child",
    "multi-module-1/file-path",
    "multi-module-1/root",
    "multi-module-2/child",
    "multi-module-2/root",
    "multi-module-3/child",
    "multi-module-3/root",
    "multipart",
    "native-benchmarks",
    "no-query-document",
    "no-runtime",
    "normalization-tests",
    "number_scalar",
    "optimistic-data",
    "optional-variables",
    "outofbounds",
    "platform-api",
    "runtime",
    "rxjava",
    "sample-server",
    "scalar-adapters",
    "schema-changes",
    "schema-packagename",
    "schema-transform/app",
    "schema-transform/plugin",
    "semantic-non-null",
    "shared-framework",
    "strict-mode",
    "termination",
    "test-network-transport",
    "websockets",
).forEach {
  // Do not create intermediate projects as they will fail for apolloTestAgreggation
  // See https://stackoverflow.com/questions/21015353/gradle-intermediate-dir-of-multiproject-not-subproject
  val project = it.replace(File.separatorChar, '-')
  include(project)
  project(":$project").projectDir = rootProject.projectDir.resolve(it)
}

includeBuild("../")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libraries.toml"))
    }
  }
}

apply(from = "./gradle/repositories.gradle.kts")
apply(from = "./gradle/ge.gradle")
