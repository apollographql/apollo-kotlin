package test;

import com.apollographql.apollo3.api.Adapters;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.NullableAdapter;
import com.apollographql.apollo3.api.StringAdapter;
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader;
import okio.Okio;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static com.google.common.truth.Truth.assertThat;

public class AdapterTest {
  @Test
  public void nonNullString() {
    String json = "\"test\"";
    BufferedSourceJsonReader jsonReader = new BufferedSourceJsonReader(Okio.buffer(Okio.source(new ByteArrayInputStream(json.getBytes()))));
    String result = StringAdapter.INSTANCE.fromJson(jsonReader, CustomScalarAdapters.Empty);

    assertThat(result).isEqualTo("test");
  }

  @Test
  public void nullString() {
    String json = "null";
    BufferedSourceJsonReader jsonReader = new BufferedSourceJsonReader(Okio.buffer(Okio.source(new ByteArrayInputStream(json.getBytes()))));
    String result = new NullableAdapter<>(StringAdapter.INSTANCE).fromJson(jsonReader, CustomScalarAdapters.Empty);
    assertThat(result).isEqualTo(null);
  }

  @Test
  public void nullString2() {
    String json = "null";
    BufferedSourceJsonReader jsonReader = new BufferedSourceJsonReader(Okio.buffer(Okio.source(new ByteArrayInputStream(json.getBytes()))));
    String result = Adapters.NullableStringAdapter.fromJson(jsonReader, CustomScalarAdapters.Empty);
    assertThat(result).isEqualTo(null);
  }

  /**
   * Kotlin APIs do not declare their exceptions but it's still possible to catch them in java
   */
  @Test
  public void malformedJson() {
    String json = "{ ";
    BufferedSourceJsonReader jsonReader = new BufferedSourceJsonReader(Okio.buffer(Okio.source(new ByteArrayInputStream(json.getBytes()))));
    try {
      Adapters.NullableStringAdapter.fromJson(jsonReader, CustomScalarAdapters.Empty);
      throw new IllegalStateException("a failure was expected");
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("Expected a string but");
    }
  }
}


