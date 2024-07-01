package custom.scalars

import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter

class MyInt(val value: Int)

class MyIntAdapter : Adapter<MyInt> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): MyInt {
    return MyInt(reader.nextInt())
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: MyInt) {
    writer.value(value.value)
  }
}
