package custom.scalars

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

class MyIntAdapter : Adapter<Int> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Int {
    return reader.nextInt()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Int) {
    writer.value(value)
  }
}
