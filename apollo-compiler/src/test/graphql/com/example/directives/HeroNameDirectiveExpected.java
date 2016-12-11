package com.example.directives;

import com.apollostack.api.GraphQLQuery;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public final class HeroNameDirective implements GraphQLQuery<HeroNameDirective.Data> {
  private final String OPERATION_DEFINITION = "query HeroNameDirective {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name @include(if: false)\n"
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
    @Nullable Hero hero();

    interface Hero {
      @Nullable String name();
    }
  }
}
