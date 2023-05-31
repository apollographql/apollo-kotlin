//
// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL version '3.8.1'.
//
package com.example.generated

import com.apollographql.apollo3.annotations.ApolloAdaptableWith
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.obj
import com.example.generated.adapter.ComputersQuery_ResponseAdapter
import com.example.generated.fragment.ComputerFields
import com.example.generated.fragment.ScreenFields
import com.example.generated.selections.ComputersQuerySelections
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

public class ComputersQuery() : Query<ComputersQuery.Data> {
  public override fun equals(other: Any?): Boolean = other != null && other::class == this::class

  public override fun hashCode(): Int = this::class.hashCode()

  public override fun id(): String = OPERATION_ID

  public override fun document(): String = OPERATION_DOCUMENT

  public override fun name(): String = OPERATION_NAME

  public override fun serializeVariables(
      writer: JsonWriter,
      customScalarAdapters: CustomScalarAdapters,
  ): Unit {
    // This operation doesn't have any variable
  }

  public override fun adapter(): Adapter<Data> = ComputersQuery_ResponseAdapter.Data.obj()

  public override fun rootField(): CompiledField = CompiledField.Builder(
      name = "data",
      type = com.example.generated.type.Query.type
  )
      .selections(selections = ComputersQuerySelections.__root)
      .build()

  @ApolloAdaptableWith(ComputersQuery_ResponseAdapter.Data::class)
  public data class Data(
      public val computers: List<Computer>,
  ) : Query.Data

  public data class Computer(
      public val __typename: String,
      public val id: String,
      public val screen: Screen,
      /**
       * Synthetic field for inline fragment on Computer
       */
      public val onComputer: OnComputer,
      /**
       * Synthetic field for 'ComputerFields'
       */
      public val computerFields: ComputerFields,
  )

  public data class Screen(
      public val __typename: String,
      public val resolution: String,
      /**
       * Synthetic field for 'ScreenFields'
       */
      public val screenFields: ScreenFields,
  )

  public data class OnComputer(
      public val cpu: String,
      public val year: Int,
  )

  public companion object {
    public const val OPERATION_ID: String =
        "63e7493c67f6481b930ab719aac0d1531148d5408b0dbcbfbc14842fa147ed91"

    /**
     * The minimized GraphQL document being sent to the server to save a few bytes.
     * The un-minimized version is:
     *
     * query Computers {
     *   computers {
     *     __typename
     *     id
     *     ... on Computer {
     *       cpu
     *       year
     *     }
     *     screen {
     *       __typename
     *       resolution
     *       ...ScreenFields
     *     }
     *     ...ComputerFields
     *   }
     * }
     *
     * fragment ScreenFields on Screen {
     *   isColor
     * }
     *
     * fragment ComputerFields on Computer {
     *   cpu
     *   screen {
     *     resolution
     *   }
     *   releaseDate
     * }
     */
    public val OPERATION_DOCUMENT: String
      get() =
        "query Computers { computers { __typename id ... on Computer { cpu year } screen { __typename resolution ...ScreenFields } ...ComputerFields } }  fragment ScreenFields on Screen { isColor }  fragment ComputerFields on Computer { cpu screen { resolution } releaseDate }"

    public const val OPERATION_NAME: String = "Computers"
  }
}
