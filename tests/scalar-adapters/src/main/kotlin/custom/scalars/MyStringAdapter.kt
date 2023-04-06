package custom.scalars

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

class MyString(val value: String)

val MyStringAdapter = object : Adapter<MyString> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): MyString {
    return MyString(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: MyString) {
    writer.value(value.value)
  }
}
