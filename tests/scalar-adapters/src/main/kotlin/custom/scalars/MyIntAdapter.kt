package custom.scalars

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

class MyInt(val value: Int)

class MyIntAdapter : Adapter<MyInt> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): MyInt {
    return MyInt(reader.nextInt())
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: MyInt) {
    writer.value(value.value)
  }
}
