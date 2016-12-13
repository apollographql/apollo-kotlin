package com.example.two_heroes;

import com.apollostack.api.Query;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TestQuery implements Query {
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

  @Override
  public String operationDefinition() {
    return OPERATION_DEFINITION;
  }

  @Override
  public List<String> fragmentDefinitions() {
    return Collections.emptyList();
  }

  @Override
  public Map<String, Object> variableDefinitions() {
    return Collections.emptyMap();
  }

  public interface Data extends Query.Data {
    @Nullable R2 r2();

    @Nullable Luke luke();

    interface R2 {
      @Nonnull String name();
    }

    interface Luke {
      @Nonnull String name();
    }
  }
}
