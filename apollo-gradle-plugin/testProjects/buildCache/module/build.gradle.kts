plugins {
  kotlin("jvm")
  id("com.apollographql.apollo")
}

apollo {
  generateKotlinModels.set(true)
}
