apollo {
  service("main") {
    mapScalar("Date", "kotlinx.datetime.LocalDate", "com.apollographql.apollo3.adapter.KotlinxLocalDateAdapter")
    mapScalar("DateTime", "kotlinx.datetime.Instant", "com.apollographql.apollo3.adapter.KotlinxInstantAdapter")
    mapScalar("MyType", "com.example.MyType", "com.example.adapter.MyTypeAdapter")
    mapScalarToKotlinString("UUID")
  }
}
