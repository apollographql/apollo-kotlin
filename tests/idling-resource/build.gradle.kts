plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.androidx.espresso.idlingresource)
  implementation(libs.apollo.idlingresource)
  testImplementation(libs.apollo.mockserver)
  testImplementation(libs.androidx.annotation)
  testImplementation(libs.android.test.runner)
}

android {
  compileSdk = libs.versions.android.sdkversion.compile.get().toInt()
  namespace = "com.apollographql.apollo.idling.resource.test"

  defaultConfig {
    minSdk = libs.versions.android.sdkversion.min.get().toInt()
  }
}

apollo {
  service("service") {
    packageName.set("idling.resource")
  }
}

tasks.configureEach {
  /**
   * /Users/mbonnin/git/apollo-kotlin/tests/idling-resource/src/test/java/IdlingResourceTest.kt: Error: Unexpected failure during lint analysis of IdlingResourceTest.kt (this is a bug in lint or one of the libraries it depends on)
   *
   * Message: () -> kotlin.String
   * Stack: IllegalStateException:KtLightClassForFacade$Companion.createForFacadeNoCache(KtLightClassForFacade.kt:274)←FacadeCache$FacadeCacheData$cache$1.createValue(FacadeCache.kt:30)←FacadeCache$FacadeCacheData$cache$1.createValue(FacadeCache.kt:28)←SLRUCache.get(SLRUCache.java:47)←FacadeCache.get(FacadeCache.kt:47)←KtLightClassForFacade$Companion.createForFacade(KtLightClassForFacade.kt:284)←CliKotlinAsJavaSupport.getFacadeClassesInPackage(CliKotlinAsJavaSupport.kt:43)←LightClassUtilsKt.findFacadeClass(lightClassUtils.kt:59)
   *
   * In general, there is so little Android code here, it's not really worth running lint
   */
  if (name.startsWith("lint")) {
    enabled = false
  }
}
