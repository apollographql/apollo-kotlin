rootProject.name = "apollo-composite"


// Integration Tests
include(":apollo-integration")
project(":apollo-integration").projectDir = file("../apollo-integration")

includeBuild("../")
