plugins {
  id("com.android.library")
  kotlin("android")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(groovy.util.Eval.x(project, "x.dep.androidxEspressoIdlingResource"))
  implementation("com.apollographql.apollo3:apollo-idling-resource")
  testImplementation("com.apollographql.apollo3:apollo-mockserver")
  testImplementation(groovy.util.Eval.x(project, "x.dep.androidSupportAnnotations"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.androidTestRunner"))
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