package scalar;

import com.apollographql.apollo3.api.ScalarAdapter;
import com.apollographql.apollo3.api.json.JsonReader;
import com.apollographql.apollo3.api.json.JsonWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class GeoPointAdapter implements ScalarAdapter<GeoPoint> {
  @Override public GeoPoint fromJson(@NotNull JsonReader reader) throws IOException {
    Double latitude = null;
    Double longitude = null;
    reader.beginObject();
    while (reader.hasNext()) {
      switch (reader.nextName()) {
        case "latitude":
          latitude = reader.nextDouble();
          break;
        case "longitude":
          longitude = reader.nextDouble();
          break;
      }
    }
    reader.endObject();
    if (latitude != null && longitude != null) {
      return new GeoPoint(latitude, longitude);
    }
    throw new RuntimeException("Invalid GeoPoint");
  }

  @Override public void toJson(@NotNull JsonWriter writer, GeoPoint value) throws IOException {
    writer.beginObject();
    writer.name("latitude").value(value.latitude);
    writer.name("longitude").value(value.longitude);
    writer.endObject();
  }

}
