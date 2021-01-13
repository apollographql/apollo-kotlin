rootProject.name = "apollo-android"

include("apollo-api")
include("apollo-cache-interceptor")
include("apollo-compiler")
include("apollo-coroutines-support")
include("apollo-gradle-plugin")
include("apollo-http-cache")
include("apollo-http-cache-api")
include("apollo-rx2-support")
include("apollo-rx3-support")
include("apollo-normalized-cache")
include("apollo-normalized-cache-api")
include("apollo-normalized-cache-sqlite")
include("apollo-runtime")
include("apollo-runtime-common")
include("apollo-runtime-kotlin")
include("apollo-testing-support")

if (System.getProperty("idea.sync.active") == null) {
  include("apollo-android-support")
  include("apollo-idling-resource")
}
