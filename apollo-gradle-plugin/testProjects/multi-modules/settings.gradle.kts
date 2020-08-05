rootProject.name = "multi-modules"

include(":lib", ":cli")

project(":lib").buildFileName = "lib.gradle.kts"
project(":cli").buildFileName = "cli.gradle.kts"

