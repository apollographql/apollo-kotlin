package com.example.pojo_fragment_with_inline_fragment;

import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.Query;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    ...HeroDetails\n"
      + "    appearsIn\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + HeroDetails.FRAGMENT_DEFINITION;

  private final Operation.Variables variables;

  public TestQuery() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  public static class Data implements Operation.Data {
    private final @Nullable Hero hero;

    public Data(@Nullable Hero hero) {
      this.hero = hero;
    }

    public @Nullable Hero hero() {
      return this.hero;
    }

    public static class Hero {
      private final @Nonnull String name;

      private final @Nonnull List<? extends Episode> appearsIn;

      private final Fragments fragments;

      public Hero(@Nonnull String name, @Nonnull List<? extends Episode> appearsIn,
          Fragments fragments) {
        this.name = name;
        this.appearsIn = appearsIn;
        this.fragments = fragments;
      }

      public @Nonnull String name() {
        return this.name;
      }

      public @Nonnull List<? extends Episode> appearsIn() {
        return this.appearsIn;
      }

      public Fragments fragments() {
        return this.fragments;
      }

      public static class Fragments {
        private final HeroDetails heroDetails;

        public Fragments(HeroDetails heroDetails) {
          this.heroDetails = heroDetails;
        }

        public HeroDetails heroDetails() {
          return this.heroDetails;
        }
      }
    }
  }
}
