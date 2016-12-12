package com.example.fragment_friends_connection;

import com.apollostack.api.GraphQLQuery;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public final class FragmentFriendsConnection implements GraphQLQuery {
  public static final String OPERATION_DEFINITION = "query FragmentFriendsConnection {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    ...HeroDetails\n"
      + "  }\n"
      + "}";

  public static final List<String> FRAGMENT_DEFINITIONS = Collections.unmodifiableList(Arrays.asList(
    "fragment HeroDetails on Character {\n"
        + "  __typename\n"
        + "  name\n"
        + "  friendsConnection {\n"
        + "    totalCount\n"
        + "    edges {\n"
        + "      node {\n"
        + "        __typename\n"
        + "        name\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "}"
  ));

  @Override
  public String operationDefinition() {
    return OPERATION_DEFINITION;
  }

  @Override
  public List<String> fragmentDefinitions() {
    return FRAGMENT_DEFINITIONS;
  }

  public interface Data {
    @Nullable Hero hero();

    interface Hero {
      Fragments fragments();

      interface Fragments {
        HeroDetails heroDetails();
      }
    }
  }
}
