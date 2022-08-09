plugins {
  id("apollo.library.jvm")
}

dependencies {
  implementation(projects.apolloApi)
  api(libs.rx.java2)
  api(libs.kotlinx.coroutines.rx2)

  api(projects.apolloRuntime)
  api(projects.apolloNormalizedCache)
}

apolloConvention {
  javaModuleName.set("com.apollographql.apollo3.rx2")
}
