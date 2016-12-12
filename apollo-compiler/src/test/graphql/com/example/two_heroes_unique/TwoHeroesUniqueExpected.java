package com.example.two_heroes_unique;

import com.apollostack.api.GraphQLQuery;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TwoHeroesUnique implements GraphQLQuery<TwoHeroesUnique.Data> {
  public static final String OPERATION_DEFINITION = "query TwoHeroesUnique {\n"
      + "  r2: hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "  }\n"
      + "  luke: hero(episode: EMPIRE) {\n"
      + "    __typename\n"
      + "    id\n"
      + "    name\n"
      + "  }\n"
      + "}";

  @Override
  public String getOperationDefinition() {
    return OPERATION_DEFINITION;
  }

  @Override
  public List<String> getFragmentDefinitions() {
    return Collections.emptyList();
  }

  public interface Data {
    @Nullable R2 r2();

    @Nullable Luke luke();

    interface R2 {
      @Nonnull String name();
    }

    interface Luke {
      long id();

      @Nonnull String name();
    }
  }
}
