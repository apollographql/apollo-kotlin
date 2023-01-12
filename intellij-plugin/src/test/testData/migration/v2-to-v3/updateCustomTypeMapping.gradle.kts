apollo {
  customTypeMapping.set(
    mapOf(
      "URL" to "kotlin.String",
      "LocalDate" to "java.time.LocalDate",
      "Upload" to "com.apollographql.apollo.api.FileUpload",
      "PaymentMethodsResponse" to "com.adyen.checkout.components.model.PaymentMethodsApiResponse",
      "CheckoutPaymentsAction" to "kotlin.String",
      "CheckoutPaymentAction" to "kotlin.String",
      "JSONString" to "org.json.JSONObject",
      "Instant" to "java.time.Instant",
    )
  )
}
