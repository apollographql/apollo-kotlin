plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.apollo.testingsupport)
  testImplementation(libs.apollo.mockserver)
  testImplementation(libs.kotlin.test)
}

apollo {
  service("service") {
    packageName.set("custom.scalars")
    // builtin
    mapScalar("Long", "kotlin.Long")
    mapScalarToKotlinFloat("CustomFloat")
    mapScalar("Any", "kotlin.Any")
    mapScalar("GeoPoint", "kotlin.Any")
    mapScalar("ID", "kotlin.Long")
    // runtime
    mapScalar("Address", "custom.scalars.Address")
    // compile time
    mapScalar("Int", "custom.scalars.MyInt", "custom.scalars.MyIntAdapter()")
    mapScalar("String", "custom.scalars.MyString", "custom.scalars.MyStringAdapter")
    mapScalarToKotlinString("Decimal")
  }
}
