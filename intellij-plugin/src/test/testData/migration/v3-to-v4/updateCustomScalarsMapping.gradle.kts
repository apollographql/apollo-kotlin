apollo {
  customScalarsMapping.put("URL", "kotlin.String")
  customScalarsMapping.put("LocalDate", "java.time.LocalDate")
  customScalarsMapping.put("Upload", "com.apollographql.apollo.api.FileUpload")
  customScalarsMapping.put(
      "PaymentMethodsResponse",
      "com.adyen.checkout.components.model.PaymentMethodsApiResponse"
  )
  customScalarsMapping.put("CheckoutPaymentsAction", "kotlin.String")
  customScalarsMapping.put("CheckoutPaymentAction", "kotlin.String")
  customScalarsMapping.put("JSONString", "org.json.JSONObject")
  customScalarsMapping.put("Instant", "java.time.Instant")

  customScalarsMapping.set(
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

apollo {
  service("main") {
    customTypeMapping.put("URL", "kotlin.String")
    customTypeMapping.put("LocalDate", "java.time.LocalDate")
    customTypeMapping.put("Upload", "com.apollographql.apollo.api.FileUpload")
    customTypeMapping.put(
        "PaymentMethodsResponse",
        "com.adyen.checkout.components.model.PaymentMethodsApiResponse"
    )
    customTypeMapping.put("CheckoutPaymentsAction", "kotlin.String")
    customTypeMapping.put("CheckoutPaymentAction", "kotlin.String")
    customTypeMapping.put("JSONString", "org.json.JSONObject")
    customTypeMapping.put("Instant", "java.time.Instant")

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
}
