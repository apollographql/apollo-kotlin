package com.example

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.json.JsonWriter

public class MyQuery() : Query<MyQuery.Data> {
  public override fun id(): String = TODO()
  public override fun document(): String = TODO()
  public override fun name(): String = TODO()
  public override fun serializeVariables(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters): Unit = TODO()
  public override fun adapter(): Adapter<Data> = TODO()
  public override fun rootField(): CompiledField = TODO()

  public data class Data(
      public val launches: Launches,
  ) : Query.Data

  public data class Launches(
      public val cursor: String,
      public val launches: List<Launch?>,
      public val hasMore: Boolean,
  )

  public data class Launch(
      public val __typename: String,
      public val fragments: Fragments,
  ) {
    public data class Fragments(
        public val launchFields: LaunchFields,
    )
  }
}

public data class LaunchFields(
    public val id: String,
    public val site: String?,
    public val mission: Mission?,
) : Fragment.Data {
  public data class Mission(
      public val name: String?,
      public val missionPatch: String?,
  )
}

suspend fun main() {
  val apolloClient: ApolloClient? = null
  val data = apolloClient!!.query(MyQuery())
      .execute()
      .data!!

  val id = data.launches.launches[0]!!.fragments.launchFields.id
  val id2 = data.launches.launches[0]!!.apply { fragments.launchFields.id }
  val id3 = data.launches.launches[0]?.fragments?.launchFields.id

  //@formatter:off
  val launch = MyQuery.Launch(
      __typename = "Launch",
      fragments = Launch.Fragments(
          launchFields = LaunchFields(
              id = "id",
              site = "site",
              mission = LaunchFields.Mission(
                  name = "name",
                  missionPatch = "missionPatch",
              ),
          ),
      ),
  )
}
