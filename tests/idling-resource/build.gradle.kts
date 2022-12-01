plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("androidx.espresso.idlingresource"))
  implementation(golatac.lib("apollo.idlingresource"))
  testImplementation(golatac.lib("apollo.mockserver"))
  testImplementation(golatac.lib("android.support.annotations"))
  testImplementation(golatac.lib("android.test.runner"))
}

android {
  compileSdk = golatac.version("android.sdkversion.compile").toInt()

  defaultConfig {
    minSdk = golatac.version("android.sdkversion.min").toInt()
    targetSdk = golatac.version("android.sdkversion.target").toInt()
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
