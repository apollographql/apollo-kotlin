package com.example.fragment_with_inline_fragment;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.example.fragment_with_inline_fragment.fragment.HeroDetails;
import com.example.fragment_with_inline_fragment.type.Episode;
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

  public interface Data extends Operation.Data {
    @Nullable Hero hero();

    interface Hero {
      @Nonnull String name();

      @Nonnull List<? extends Episode> appearsIn();

      Fragments fragments();

      interface Fragments {
        HeroDetails heroDetails();

        interface Factory {
          Creator creator();
        }

        interface Creator {
          Fragments create(HeroDetails heroDetails);
        }
      }

      interface Factory {
        Creator creator();

        Fragments.Factory fragmentsFactory();
      }

      interface Creator {
        Hero create(@Nonnull String name, @Nonnull List<? extends Episode> appearsIn,
            Fragments fragments);
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
