apollo {
  // TODO: This shortcut jumpstarts the migration from v2 to v3, but it is recommended to use settings idiomatic to v3 instead.
  // See https://www.apollographql.com/docs/kotlin/migration/3.0/
  useVersion2Compat()

  mapScalarToKotlinString("URL")
  mapScalar("LocalDate", "java.time.LocalDate")
  mapScalarToUpload("Upload")
  mapScalar("PaymentMethodsResponse", "com.adyen.checkout.components.model.PaymentMethodsApiResponse")
  mapScalarToKotlinString("CheckoutPaymentsAction")
  mapScalarToKotlinString("CheckoutPaymentAction")
  mapScalar("JSONString", "org.json.JSONObject")
  mapScalar("Instant", "java.time.Instant")
}
