import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import kotlin.js.JsExport

@JsExport
class Point(val x: Int, val y: Int)

object PointAdapter: Adapter<Point> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Point {
    @Suppress("UNCHECKED_CAST") val map = AnyAdapter.fromJson(reader, customScalarAdapters) as Map<String, Int>

    return Point(map.get("x")!!, map.get("y")!!)
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Point) {
    TODO("Not yet implemented")
  }

}