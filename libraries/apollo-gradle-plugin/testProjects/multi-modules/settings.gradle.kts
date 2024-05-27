rootProject.name = "multi-modules"

apply(from = "../../../../gradle/test.settings.gradle.kts")

include(":root", ":leaf")
