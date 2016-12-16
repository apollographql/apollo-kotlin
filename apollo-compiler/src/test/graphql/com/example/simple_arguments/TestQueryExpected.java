package com.example.simple_arguments;

import com.apollostack.api.Query;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public final class TestQuery implements Query<TestQuery.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery($episode: Episode, $includeName: Boolean!) {\n"
      + "  hero(episode: $episode) {\n"
      + "    __typename\n"
      + "    name @include(if: $includeName)\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final TestQuery.Variables variables;

  public TestQuery(TestQuery.Variables variables) {
    this.variables = variables;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public TestQuery.Variables variables() {
    return variables;
  }

  public static final class Variables extends Query.Variables {
    Variables(Map<String, Object> data) {
      super(data);
    }

    public @Nullable Episode episode() {
      return (Episode) data.get("episode");
    }

    public boolean includeName() {
      return (boolean) data.get("includeName");
    }

    public static final class Builder {
      final Map<String, Object> data = new HashMap<String, Object>();

      public Builder episode(@Nullable Episode episode) {
        data.put("episode", episode);
        return this;
      }

      public Builder includeName(boolean includeName) {
        data.put("includeName", includeName);
        return this;
      }

      public Variables build() {
        return new Variables(data);
      }
    }
  }

  public interface Data extends Query.Data {
    @Nullable Hero hero();

    interface Hero {
      @Nullable String name();
    }
  }
}
