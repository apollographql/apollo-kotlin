package com.apollographql.ijplugin.studio.fieldinsights

typealias Milliseconds = Double

class FieldInsightsRepository {
  private val fieldInsights = mutableMapOf<String, MutableMap<String, Milliseconds>>()

  fun getLatency(typeName: String, fieldName: String): Milliseconds? {
    return fieldInsights[typeName]?.get(fieldName)
  }

  fun putLatency(typeName: String, fieldName: String, latencyMs: Double) {
    fieldInsights.getOrPut(typeName) { mutableMapOf() }[fieldName] = latencyMs
  }
}

fun newMockFieldInsightsRepository(): FieldInsightsRepository {
  return FieldInsightsRepository().apply {
    putLatency("Query", "computers", 100.0)
    putLatency("Computer", "id", 200.0)
    putLatency("Computer", "cpu", 5.0)
    putLatency("Computer", "year", 2.0)
    putLatency("Computer", "releaseDate", 42.0)
  }
}
