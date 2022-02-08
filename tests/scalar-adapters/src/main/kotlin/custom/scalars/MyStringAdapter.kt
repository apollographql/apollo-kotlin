package custom.scalars

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

val MyStringAdapter = object : Adapter<String> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): String {
    return reader.nextString()!!
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: String) {
    writer.value(value)
  }
}
