package test;

import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.Operations;
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter;
import com.google.common.truth.Truth;
import javatest.GetRandomQuery;
import okio.Buffer;
import org.junit.Test;

import java.io.IOException;

public class OperationTest {
  @Test
  public void canUseDefaultFunctions() throws IOException {
    GetRandomQuery query = new GetRandomQuery();

    GetRandomQuery.Data data = new GetRandomQuery.Data(42);

    Buffer buffer = new Buffer();
    query.adapter().toJson(new BufferedSinkJsonWriter(buffer), CustomScalarAdapters.Empty, data);
    String json = buffer.readUtf8();

    // We don't want to test indentation here so we replace whitespace
    Truth.assertThat(
        json.replace("\n", "")
            .replace(" ", "")
    ).isEqualTo("{\"random\":42}");
  }
}