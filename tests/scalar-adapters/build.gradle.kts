plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation("com.apollographql.apollo3:apollo-adapters")
  testImplementation("com.apollographql.apollo3:apollo-testing-support")
  testImplementation(groovy.util.Eval.x(project, "x.dep.kotlinJunit"))
}

apollo {
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
