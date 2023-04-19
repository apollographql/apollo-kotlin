package data.builders;

import com.apollographql.apollo3.api.Adapter;
import com.apollographql.apollo3.api.ScalarAdapters;
import com.apollographql.apollo3.api.json.JsonReader;
import com.apollographql.apollo3.api.json.JsonWriter;
import org.jetbrains.annotations.NotNull;

public class MyLong {
  public Long value;

  public MyLong(Long value) {
    this.value = value;
  }

  public static class MyLongAdapter implements Adapter<MyLong> {
    @Override public MyLong fromJson(@NotNull JsonReader reader, @NotNull ScalarAdapters customScalarAdapters) {
      try {
        return new MyLong(reader.nextLong());
      } catch (Exception e) {
        throw new RuntimeException();
      }
    }

    @Override public void toJson(@NotNull JsonWriter writer, @NotNull ScalarAdapters customScalarAdapters, MyLong value) {
      try {
        writer.value(value.value);
      } catch (Exception e) {
        throw new RuntimeException();
      }
    }

    public static final MyLongAdapter INSTANCE = new MyLongAdapter();
  }
}
