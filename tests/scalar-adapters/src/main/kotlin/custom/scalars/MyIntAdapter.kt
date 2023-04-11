package custom.scalars

import com.apollographql.apollo3.api.ScalarAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

class MyInt(val value: Int)

class MyIntAdapter : ScalarAdapter<MyInt> {
  override fun fromJson(reader: JsonReader): MyInt {
    return MyInt(reader.nextInt())
  }

  override fun toJson(writer: JsonWriter, value: MyInt) {
    writer.value(value.value)
  }
}
