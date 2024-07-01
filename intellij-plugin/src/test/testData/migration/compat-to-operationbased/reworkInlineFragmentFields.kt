package com.example

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.json.JsonWriter
import com.example.FruitListQuery.AsGranny


public class FruitListQuery() : Query<FruitListQuery.Data> {
  public override fun id(): String = TODO()
  public override fun document(): String = TODO()
  public override fun name(): String = TODO()
  public override fun serializeVariables(
      writer: JsonWriter,
      customScalarAdapters: CustomScalarAdapters
  ): Unit {
  }

  public override fun adapter(): Adapter<Data> = TODO()
  public override fun rootField(): CompiledField = TODO()

  public data class Data(
      public val fruitList: FruitList?,
  ) : Query.Data

  public data class FruitList(
      public val ok: Boolean,
      public val list: kotlin.collections.List<List>,
  )

  public data class List(
      public val __typename: String,
      public val id: String,
      /**
       * Synthetic field for inline fragment on Cherry
       */
      public val asCherry: AsCherry?,
      /**
       * Synthetic field for inline fragment on Apple
       */
      public val asApple: AsApple?,
  )

  public data class AsCherry(
      public val __typename: String,
      public val id: String,
      public val pit: Boolean,
  )

  public data class AsApple(
      public val __typename: String,
      public val id: String,
      public val color: String,
      /**
       * Synthetic field for inline fragment on Golden
       */
      public val asGolden: AsGolden?,
      /**
       * Synthetic field for inline fragment on Granny
       */
      public val asGranny: AsGranny?,
  )

  public data class AsGolden(
      public val __typename: String,
      public val id: String,
      public val color: String,
      public val isGolden: Boolean,
  )

  public data class AsGranny(
      public val __typename: String,
      public val id: String,
      public val color: String,
      public val isGranny: Boolean,
  )
}

suspend fun main() {
  val apolloClient: ApolloClient? = null
  val data = apolloClient!!.query(FruitListQuery())
      .execute()
      .data!!

  val asApple: FruitListQuery.AsApple = data.fruitList!!.list[0].asApple!!
  val color = asApple.color
  val id = data.fruitList.list[0].asApple!!.id

  val asGranny: AsGranny = data.fruitList.list[0].asApple!!.asGranny!!
  val isGranny = asGranny.isGranny
  val id2 = data.fruitList.list[0].asApple!!.asGranny!!.id

  val newAsGranny: FruitListQuery.AsApple = FruitListQuery.AsApple(
      __typename = "Apple",
      id = "id",
      color = "color",
      asGolden = FruitListQuery.AsGolden(
          __typename = "Golden",
          id = "id",
          color = "color",
          isGolden = true,
      ),
      asGranny = FruitListQuery.AsGranny(
          __typename = "Granny",
          id = "id",
          color = "color",
          isGranny = true,
      ),
  )
}
