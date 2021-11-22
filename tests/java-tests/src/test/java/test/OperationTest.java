package test;

import com.apollographql.apollo3.api.Operations;
import com.google.common.truth.Truth;
import javatest.GetRandomQuery;
import org.junit.Test;

public class OperationTest {
  @Test
  public void canUseDefaultFunctions() {
    GetRandomQuery query = new GetRandomQuery();

    GetRandomQuery.Data data = new GetRandomQuery.Data(42);

    String json = Operations.composeJsonData(query, data);
    // We don't want to test indentation here so we replace whitespace
    Truth.assertThat(
        json.replace("\n", "")
            .replace(" ", "")
    ).isEqualTo("{\"random\":42}");
  }
}