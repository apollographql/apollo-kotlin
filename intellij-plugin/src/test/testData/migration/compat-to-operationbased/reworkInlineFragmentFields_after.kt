package com.example

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.json.JsonWriter
import com.example.FruitListQuery.OnGranny


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
      public val asCherry: OnCherry?,
    /**
       * Synthetic field for inline fragment on Apple
       */
      public val asApple: OnApple?,
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
      public val asGolden: OnGolden?,
    /**
       * Synthetic field for inline fragment on Granny
       */
      public val asGranny: OnGranny?,
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

  val asApple: FruitListQuery.OnApple = data.fruitList!!.list[0].onApple!!
  val color = asApple.color
  val id = data.fruitList.list[0].onApple!!.id

  val asGranny: OnGranny = data.fruitList.list[0].onApple!!.onGranny!!
  val isGranny = asGranny.isGranny
  val id2 = data.fruitList.list[0].onApple!!.onGranny!!.id

  val newAsGranny: FruitListQuery.OnApple = FruitListQuery.OnApple(
      __typename = "Apple",
      id = "id",
      color = "color",
      onGolden = FruitListQuery.OnGolden(
          __typename = "Golden",
          id = "id",
          color = "color",
          isGolden = true,
      ),
      onGranny = FruitListQuery.OnGranny(
          __typename = "Granny",
          id = "id",
          color = "color",
          isGranny = true,
      ),
  )
}
