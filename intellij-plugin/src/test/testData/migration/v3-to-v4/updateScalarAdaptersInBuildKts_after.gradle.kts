apollo {
  service("main") {
    mapScalar("Date", "kotlinx.datetime.LocalDate", "com.apollographql.apollo.adapter.KotlinxLocalDateAdapter")
    mapScalar("DateTime", "kotlinx.datetime.Instant", "com.apollographql.apollo.adapter.KotlinxInstantAdapter")
    mapScalar("MyType", "com.example.MyType", "com.example.adapter.MyTypeAdapter")
    mapScalarToKotlinString("UUID")
  }
}
