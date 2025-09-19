package test

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseResponse
import okio.Buffer
import org.intellij.lang.annotations.Language


private fun String.jsonReader(): JsonReader = Buffer().writeUtf8(this).jsonReader()

fun <D : Query.Data> Query<D>.parseResponse(json: String): ApolloResponse<D> = parseResponse(json.jsonReader(), null, CustomScalarAdapters.Empty, null)

@Language("json")
val userNameError = """
    {
      "errors": [
        {
          "path": ["user", "name"], 
          "message": "cannot resolve name"
        }
      ],
      "data": {
        "user": {
          "name": null
        }
      }
    }
  """.trimIndent()

@Language("json")
val userSuccess = """
    {
      "data": {
        "user": {
          "name": "Pancakes"
        }
      }
    }
  """.trimIndent()

@Language("json")
val productPriceError = """
    {
      "errors": [
        {
          "path": ["product", "price"], 
          "message": "cannot resolve price"
        }
      ],
      "data": {
        "product": {
          "price": null
        }
      }
    }
  """.trimIndent()

@Language("json")
val productPriceNull = """
    {
      "data": {
        "product": {
          "price": null
        }
      }
    }
  """.trimIndent()

