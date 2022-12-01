plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
  implementation(golatac.lib("apollo.adapters"))
  testImplementation(golatac.lib("apollo.testingsupport"))
  testImplementation(golatac.lib("kotlin.test"))
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
    // decimal
    mapScalar(
        "Decimal",
        "com.apollographql.apollo3.adapter.BigDecimal",
        "com.apollographql.apollo3.adapter.BigDecimalAdapter"
    )
  }
}
