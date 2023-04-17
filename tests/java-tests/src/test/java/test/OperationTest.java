package test;

import com.apollographql.apollo3.api.ApolloAdapter;
import com.apollographql.apollo3.api.ScalarAdapters;
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter;
import com.google.common.truth.Truth;
import javatest.GetRandomQuery;
import okio.Buffer;
import org.junit.Test;

import java.io.IOException;

public class OperationTest {
  @Test
  public void canUseDefaultFunctions() throws IOException {
    GetRandomQuery query = GetRandomQuery.builder().build();

    GetRandomQuery.Data data = GetRandomQuery.Data.builder().random(42).build();

    Buffer buffer = new Buffer();
    query.adapter().toJson(new BufferedSinkJsonWriter(buffer), data, new ApolloAdapter.DataSerializeContext(ScalarAdapters.Empty));
    String json = buffer.readUtf8();

    // We don't want to test indentation here so we replace whitespace
    Truth.assertThat(
        json.replace("\n", "")
            .replace(" ", "")
    ).isEqualTo("{\"random\":42}");
  }
}
