package com.example.simple_inline_fragment;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import java.lang.Double;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query Query {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    ... on Human {\n"
      + "      height\n"
      + "    }\n"
      + "    ... on Droid {\n"
      + "      primaryFunction\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

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

  public interface Data extends Operation.Data {
    @Nullable Hero hero();

    interface Hero {
      @Nonnull String name();

      @Nullable AsHuman asHuman();

      @Nullable AsDroid asDroid();

      interface AsHuman {
        @Nonnull String name();

        @Nullable Double height();

        interface Factory {
          Creator creator();
        }

        interface Creator {
          AsHuman create(@Nonnull String name, @Nullable Double height);
        }
      }

      interface AsDroid {
        @Nonnull String name();

        @Nullable String primaryFunction();

        interface Factory {
          Creator creator();
        }

        interface Creator {
          AsDroid create(@Nonnull String name, @Nullable String primaryFunction);
        }
      }

      interface Factory {
        Creator creator();

        AsHuman.Factory asHumanFactory();

        AsDroid.Factory asDroidFactory();
      }

      interface Creator {
        Hero create(@Nonnull String name, @Nullable AsHuman asHuman, @Nullable AsDroid asDroid);
      }
    }

    interface Factory {
      Creator creator();

      Hero.Factory heroFactory();
    }

    interface Creator {
      Data create(@Nullable Hero hero);
    }
  }
}
