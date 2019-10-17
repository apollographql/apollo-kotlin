rootProject.name = "composite"

include(":java-sample")
project(":java-sample").projectDir = file("../samples/java-sample")
include(":kotlin-sample")
project(":kotlin-sample").projectDir = file("../samples/kotlin-sample")
include(":apollo-integration")
project(":apollo-integration").projectDir = file("../apollo-integration")

includeBuild("../")
