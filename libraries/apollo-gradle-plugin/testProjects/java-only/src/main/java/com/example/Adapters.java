packate com.example;

import com.apollographql.apollo3.api.*
import com.apollographql.apollo3.api.json.*

public class Adapters {
  public static final Adapter<Long> = new Adapter<Long>() {
    @Override
    public Long fromJson (JsonReader reader, ScalarAdapters scalarAdapters) throws IOException {
      return reader.nextLong();
    }

    @Override
    public void toJson (JsonWriter writer, ScalarAdapters scalarAdapters, Long value) throws IOException {
      writer.value(value);
    }
  };
}
