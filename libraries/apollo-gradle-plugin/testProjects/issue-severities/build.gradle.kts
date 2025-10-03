plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
  alias(libs.plugins.compat.patrouille)
}

dependencies {
  implementation(apollo.deps.api)
}

apollo {
  service("default") {
    packageName.set("default")
  }
  service("ignore") {
    packageName.set("ignore")
    issueSeverity("UnusedFragment", "ignore")
  }
  service("warn") {
    packageName.set("warn")
    issueSeverity("UnusedFragment", "warn")
  }
  service("error") {
    packageName.set("error")
    issueSeverity("UnusedFragment", "error")
  }
  service("invalid") {
    packageName.set("invalid")
    issueSeverity("UnusedFragment", "invalid")
  }
}

compatPatrouille {
  java(17)
}

