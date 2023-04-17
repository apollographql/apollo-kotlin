package test;

import com.apollographql.apollo3.api.Adapters;
import com.apollographql.apollo3.api.ApolloAdapter.DataDeserializeContext;
import com.apollographql.apollo3.api.NullableAdapter;
import com.apollographql.apollo3.api.ScalarAdapters;
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader;
import okio.Okio;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;

import static com.google.common.truth.Truth.assertThat;

public class AdapterTest {
  @Test
  public void nonNullString() throws IOException {
    String json = "\"test\"";
    BufferedSourceJsonReader jsonReader = new BufferedSourceJsonReader(Okio.buffer(Okio.source(new ByteArrayInputStream(json.getBytes()))));
    String result = Adapters.StringApolloAdapter.fromJson(jsonReader, new DataDeserializeContext(ScalarAdapters.Empty, new HashSet<>(), null));

    assertThat(result).isEqualTo("test");
  }

  @Test
  public void nullString() {
    String json = "null";
    BufferedSourceJsonReader jsonReader = new BufferedSourceJsonReader(Okio.buffer(Okio.source(new ByteArrayInputStream(json.getBytes()))));
    String result = new NullableAdapter<>(Adapters.StringApolloAdapter).fromJson(jsonReader, new DataDeserializeContext(ScalarAdapters.Empty, new HashSet<>(), null));
    assertThat(result).isEqualTo(null);
  }

  @Test
  public void nullString2() {
    String json = "null";
    BufferedSourceJsonReader jsonReader = new BufferedSourceJsonReader(Okio.buffer(Okio.source(new ByteArrayInputStream(json.getBytes()))));
    String result = Adapters.NullableStringApolloAdapter.fromJson(jsonReader, new DataDeserializeContext(ScalarAdapters.Empty, new HashSet<>(), null));
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
      Adapters.NullableStringApolloAdapter.fromJson(jsonReader, new DataDeserializeContext(ScalarAdapters.Empty, new HashSet<>(), null));
      throw new IllegalStateException("a failure was expected");
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("Expected a string but");
    }
  }
}


