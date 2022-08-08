plugins {
  id(libs.plugins.android.library.get().toString())
  id(libs.plugins.kotlin.android.get().toString())
  id(libs.plugins.apollo.get().toString())
}

dependencies {
  implementation(libs.androidx.espresso.idlingResource)
  implementation(libs.apollo.idlingResource)
  testImplementation(libs.apollo.mockserver)
  testImplementation(libs.android.support.annotations)
  testImplementation(libs.android.test.runner)
}

android {
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  defaultConfig {
    minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString().toInt())
    targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString().toInt())
  }
}

apollo {
  packageName.set("idling.resource")
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
