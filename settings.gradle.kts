rootProject.name = "apollo-android"

include("apollo-compiler")
include("apollo-gradle-plugin")
include("apollo-runtime")
include("apollo-api")
include("apollo-rx2-support")
include("apollo-rx3-support")
include("apollo-coroutines-support")
include("apollo-http-cache")
include("apollo-http-cache-api")
include("apollo-normalized-cache")
include("apollo-normalized-cache-api")
include("apollo-runtime-kotlin")
include("apollo-cache-interceptor")
include("apollo-testing-support")
include("apollo-normalized-cache-sqlite")

if (System.getProperty("idea.sync.active") == null) {
  include("apollo-android-support")
  include("apollo-idling-resource")
}
