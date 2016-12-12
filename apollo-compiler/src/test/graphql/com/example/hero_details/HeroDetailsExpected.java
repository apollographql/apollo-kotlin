package com.example.hero_details;

import com.apollostack.api.GraphQLQuery;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class HeroDetails implements GraphQLQuery {
  public static final String OPERATION_DEFINITION = "query HeroDetails {\n"
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

  @Override
  public String operationDefinition() {
    return OPERATION_DEFINITION;
  }

  @Override
  public List<String> fragmentDefinitions() {
    return Collections.emptyList();
  }

  public interface Data {
    @Nullable Hero hero();

    interface Hero {
      @Nonnull String name();

      @Nonnull FriendsConnection friendsConnection();

      interface FriendsConnection {
        @Nullable Integer totalCount();

        @Nullable List<Edge> edges();

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
