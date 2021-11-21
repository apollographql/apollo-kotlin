package test;

import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.google.common.truth.Truth;
import javatest.GetRandomQuery;
import org.junit.Test;

public class OperationTest {
  @Test
  public void canUseDefaultFunctions() {
    GetRandomQuery query = new GetRandomQuery();

    GetRandomQuery.Data data = new GetRandomQuery.Data(42);
    String json = query.composeJsonData(data, CustomScalarAdapters.Empty, "");
    // We don't want to test indentation here so we replace newlines
    Truth.assertThat(json.replace("\n", ""))
        .isEqualTo("{\"random\":42}");
  }

}
