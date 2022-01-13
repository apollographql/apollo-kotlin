import com.apollographql.apollo3.compiler.NoArgConstructorAdapterInitializer
import com.apollographql.apollo3.compiler.SingletonAdapterInitializer

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  testImplementation("com.apollographql.apollo3:apollo-testing-support")
  testImplementation(groovy.util.Eval.x(project, "x.dep.kotlinJunit"))
}

apollo {
  packageName.set("custom.scalars")
  mapScalar("Long", "kotlin.Long")
  mapScalar("CustomFloat", "kotlin.Float")
  mapScalar("Any", "kotlin.Any")
  mapScalar("GeoPoint", "kotlin.Any")
  mapScalar("Address", "custom.scalars.Address")
  mapScalar("ID", "kotlin.Long")
  mapScalar("Int", "kotlin.Int", NoArgConstructorAdapterInitializer("custom.scalars.MyIntAdapter"))
  mapScalar("String", "kotlin.String", SingletonAdapterInitializer("custom.scalars.MyStringAdapter"))
}
