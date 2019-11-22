// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.fragments_with_type_condition_nullable

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.internal.SimpleOperationResponseParser
import com.apollographql.apollo.internal.QueryDocumentMinifier
import com.example.fragments_with_type_condition_nullable.fragment.DroidDetails
import com.example.fragments_with_type_condition_nullable.fragment.HumanDetails
import java.io.IOException
import kotlin.Array
import kotlin.String
import kotlin.Suppress
import kotlin.jvm.Throws
import okio.BufferedSource

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter")
class TestQuery : Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
  override fun operationId(): String = OPERATION_ID
  override fun queryDocument(): String = QUERY_DOCUMENT
  override fun wrapData(data: Data?): Data? = data
  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES
  override fun name(): OperationName = OPERATION_NAME
  override fun responseFieldMapper(): ResponseFieldMapper<Data> = ResponseFieldMapper {
    Data(it)
  }

  @Throws(IOException::class)
  override fun parse(source: BufferedSource, scalarTypeAdapters: ScalarTypeAdapters): Response<Data>
      = SimpleOperationResponseParser.parse(source, this, scalarTypeAdapters)

  data class R2(
    val __typename: String,
    val fragments: Fragments
  ) {
    fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeString(RESPONSE_FIELDS[0], __typename)
      fragments.marshaller().marshal(it)
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null),
          ResponseField.forString("__typename", "__typename", null, false, null)
          )

      operator fun invoke(reader: ResponseReader): R2 {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val fragments = reader.readConditional(RESPONSE_FIELDS[1]) { conditionalType, reader ->
          val humanDetails = if (HumanDetails.POSSIBLE_TYPES.contains(conditionalType))
              HumanDetails(reader) else null
          val droidDetails = if (DroidDetails.POSSIBLE_TYPES.contains(conditionalType))
              DroidDetails(reader) else null
          Fragments(
            humanDetails = humanDetails,
            droidDetails = droidDetails
          )
        }

        return R2(
          __typename = __typename,
          fragments = fragments
        )
      }
    }

    data class Fragments(
      val humanDetails: HumanDetails?,
      val droidDetails: DroidDetails?
    ) {
      fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
        humanDetails?.marshaller()?.marshal(it)
        droidDetails?.marshaller()?.marshal(it)
      }
    }
  }

  data class Luke(
    val __typename: String,
    val fragments: Fragments
  ) {
    fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeString(RESPONSE_FIELDS[0], __typename)
      fragments.marshaller().marshal(it)
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null),
          ResponseField.forString("__typename", "__typename", null, false, null)
          )

      operator fun invoke(reader: ResponseReader): Luke {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val fragments = reader.readConditional(RESPONSE_FIELDS[1]) { conditionalType, reader ->
          val humanDetails = if (HumanDetails.POSSIBLE_TYPES.contains(conditionalType))
              HumanDetails(reader) else null
          val droidDetails = if (DroidDetails.POSSIBLE_TYPES.contains(conditionalType))
              DroidDetails(reader) else null
          Fragments(
            humanDetails = humanDetails,
            droidDetails = droidDetails
          )
        }

        return Luke(
          __typename = __typename,
          fragments = fragments
        )
      }
    }

    data class Fragments(
      val humanDetails: HumanDetails?,
      val droidDetails: DroidDetails?
    ) {
      fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
        humanDetails?.marshaller()?.marshal(it)
        droidDetails?.marshaller()?.marshal(it)
      }
    }
  }

  data class Data(
    val r2: R2?,
    val luke: Luke?
  ) : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeObject(RESPONSE_FIELDS[0], r2?.marshaller())
      it.writeObject(RESPONSE_FIELDS[1], luke?.marshaller())
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forObject("r2", "hero", null, true, null),
          ResponseField.forObject("luke", "hero", null, true, null)
          )

      operator fun invoke(reader: ResponseReader): Data {
        val r2 = reader.readObject<R2>(RESPONSE_FIELDS[0]) { reader ->
          R2(reader)
        }

        val luke = reader.readObject<Luke>(RESPONSE_FIELDS[1]) { reader ->
          Luke(reader)
        }

        return Data(
          r2 = r2,
          luke = luke
        )
      }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "919cec7210259fa24fc6026fe680b96f357c14ebf3c8a734979dcfb819685d6a"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery {
          |  r2: hero {
          |    __typename
          |    ...HumanDetails
          |    ...DroidDetails
          |  }
          |  luke: hero {
          |    __typename
          |    ...HumanDetails
          |    ...DroidDetails
          |  }
          |}
          |fragment HumanDetails on Human {
          |  __typename
          |  name
          |  height
          |}
          |fragment DroidDetails on Droid {
          |  __typename
          |  name
          |  primaryFunction
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: OperationName = OperationName { "TestQuery" }
  }
}
