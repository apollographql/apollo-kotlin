package com.apollographql.apollo3.interfaces

import com.apollographql.apollo3.IdFieldCacheKeyResolver
import com.apollographql.apollo3.ResponseNormalizationTest
import com.apollographql.apollo3.ResponseNormalizationTest.Companion.records
import com.apollographql.apollo3.Utils
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.fromResponse
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.cache.normalized.internal.normalize
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.normalizer.HeroParentTypeDependentFieldQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.google.common.truth.Truth
import org.junit.Test

class ResponseNormalizationTest {
  @Test
  fun list_of_objects_with_null_object() {
    val records = records(AllPlanetsQuery(), "AllPlanetsListOfObjectWithNullObject.json")
    val fieldKey = "allPlanets({\"first\":300})"
    var record: Record?

    record = records.get("$fieldKey.planets.0")
    Truth.assertThat(record?.get("filmConnection")).isNull()
    record = records.get("$fieldKey.planets.0.filmConnection")
    Truth.assertThat(record).isNull()
    record = records.get("$fieldKey.planets.1.filmConnection")
    Truth.assertThat(record).isNotNull()
  }


  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentFieldHuman() {
    val records = records(HeroParentTypeDependentFieldQuery(Input.Present(Episode.EMPIRE)), "HeroParentTypeDependentFieldHumanResponse.json")

    val lukeRecord = records.get("${ResponseNormalizationTest.TEST_FIELD_KEY_EMPIRE}.friends.0")
    Truth.assertThat(lukeRecord!!["name"]).isEqualTo("Han Solo")
    Truth.assertThat(lukeRecord["height({\"unit\":\"FOOT\"})"]).isEqualTo(5.905512)
  }
}