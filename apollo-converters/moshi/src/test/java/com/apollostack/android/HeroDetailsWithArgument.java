package com.apollostack.android;

import com.apollostack.api.GraphQLOperation;
import com.apollostack.api.GraphQLQuery;

import javax.annotation.Nullable;

public final class HeroDetailsWithArgument implements GraphQLQuery<HeroDetailsWithArgument.Variables> {
  public static final String OPERATION_DEFINITION = "query HeroDetailsWithArgument($episode: Episode) {\n"
      + "  hero(episode: $episode) {\n"
      + "    __typename\n"
      + "    name\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final Variables variables;

  public HeroDetailsWithArgument(Variables variables) {
    this.variables = variables;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Variables variables() {
    return variables;
  }

  public static final class Variables extends GraphQLOperation.Variables {
    private final @Nullable Episode episode;

    Variables(@Nullable Episode episode) {
      this.episode = episode;
    }

    public @Nullable Episode episode() {
      return episode;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private @Nullable Episode episode;

      Builder() {
      }

      public Builder episode(@Nullable Episode episode) {
        this.episode = episode;
        return this;
      }

      public Variables build() {
        return new Variables(episode);
      }
    }
  }

  public interface Data extends GraphQLOperation.Data {
    @Nullable Hero hero();

    interface Hero {
      @Nullable String name();
    }
  }
}