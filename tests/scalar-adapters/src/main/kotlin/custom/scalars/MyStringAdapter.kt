package custom.scalars

import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter

class MyString(val value: String)

val MyStringAdapter = object : Adapter<MyString> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): MyString {
    return MyString(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: MyString) {
    writer.value(value.value)
  }
}
