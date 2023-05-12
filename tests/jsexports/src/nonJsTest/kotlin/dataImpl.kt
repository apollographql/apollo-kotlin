import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.obj
import jsexport.GetAnimalQuery
import jsexport.adapter.GetAnimalQuery_ResponseAdapter
import okio.Buffer
import okio.use

fun animalImpl(response: String): GetAnimalQuery.Data {
  return Buffer().writeUtf8(response).jsonReader().use {
    GetAnimalQuery_ResponseAdapter.Data.obj(false).fromJson(it, CustomScalarAdapters.Empty)
  }
}