packate com.example;

import com.apollographql.apollo.api.*
import com.apollographql.apollo.api.json.*

public class Adapters {
  public static final Adapter<Long> = new Adapter<Long>() {
    @Override
    public Long fromJson (JsonReader reader, CustomScalarAdapters customScalarAdapters) throws IOException {
      return reader.nextLong();
    }

    @Override
    public void toJson (JsonWriter writer, CustomScalarAdapters customScalarAdapters, Long value) throws IOException {
      writer.value(value);
    }
  };
}
