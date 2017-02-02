package com.example.two_heroes;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  r2: hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "  }\n"
      + "  luke: hero(episode: EMPIRE) {\n"
      + "    __typename\n"
      + "    name\n"
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
    @Nullable R2 r2();

    @Nullable Luke luke();

    interface R2 {
      @Nonnull String name();

      interface Factory {
        Creator creator();
      }

      interface Creator {
        R2 create(@Nonnull String name);
      }
    }

    interface Luke {
      @Nonnull String name();

      interface Factory {
        Creator creator();
      }

      interface Creator {
        Luke create(@Nonnull String name);
      }
    }

    interface Factory {
      Creator creator();

      R2.Factory r2Factory();

      Luke.Factory lukeFactory();
    }

    interface Creator {
      Data create(@Nullable R2 r2, @Nullable Luke luke);
    }
  }
}
