package com.example.simple_arguments;

import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.Query;
import com.example.simple_arguments.type.Episode;
import java.lang.Override;
import java.lang.String;
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

  public static final class Variables extends Operation.Variables {
    private final @Nullable Episode episode;

    private final boolean includeName;

    Variables(@Nullable Episode episode, boolean includeName) {
      this.episode = episode;
      this.includeName = includeName;
    }

    public @Nullable Episode episode() {
      return episode;
    }

    public boolean includeName() {
      return includeName;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private @Nullable Episode episode;

      private boolean includeName;

      Builder() {
      }

      public Builder episode(@Nullable Episode episode) {
        this.episode = episode;
        return this;
      }

      public Builder includeName(boolean includeName) {
        this.includeName = includeName;
        return this;
      }

      public Variables build() {
        return new Variables(episode, includeName);
      }
    }
  }

  public interface Data extends Operation.Data {
    @Nullable Hero hero();

    interface Hero {
      @Nullable String name();
    }
  }
}
