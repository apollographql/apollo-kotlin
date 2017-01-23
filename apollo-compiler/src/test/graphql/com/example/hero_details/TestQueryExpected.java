package com.example.hero_details;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    friendsConnection {\n"
      + "      totalCount\n"
      + "      edges {\n"
      + "        node {\n"
      + "          __typename\n"
      + "          name\n"
      + "        }\n"
      + "      }\n"
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

      @Nonnull FriendsConnection friendsConnection();

      interface FriendsConnection {
        @Nullable Integer totalCount();

        @Nullable List<? extends Edge> edges();

        interface Edge {
          @Nullable Node node();

          interface Node {
            @Nonnull String name();
          }
        }
      }
    }
  }
}
