packate com.example;

import com.apollographql.apollo3.api.*
import com.apollographql.apollo3.api.json.*

public class Adapters {
  public static final Adapter<Long> = new Adapter<Long>() {
    @Override
    public Long fromJson (JsonReader reader) throws IOException {
      return reader.nextLong();
    }

    @Override
    public void toJson (JsonWriter writer, Long value) throws IOException {
      writer.value(value);
    }
  };
}
