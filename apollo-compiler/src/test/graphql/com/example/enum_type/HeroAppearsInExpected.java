package com.example.enum_type;

import com.apollostack.api.GraphQLQuery;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class HeroAppearsIn implements GraphQLQuery {
  public static final String OPERATION_DEFINITION = "query HeroAppearsIn {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    appearsIn\n"
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

      @Nonnull List<Episode> appearsIn();
    }
  }
}
