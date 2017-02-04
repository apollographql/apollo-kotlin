package com.example.unique_type_name;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.example.unique_type_name.fragment.HeroDetails;
import com.example.unique_type_name.type.Episode;
import java.lang.Double;
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
      + "    friends {\n"
      + "      __typename\n"
      + "      name\n"
      + "    }\n"
      + "    ... on Human {\n"
      + "      height\n"
      + "      friends {\n"
      + "        __typename\n"
      + "        appearsIn\n"
      + "        friends {\n"
      + "          __typename\n"
      + "          ...HeroDetails\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
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

      @Nullable List<? extends Friend> friends();

      @Nullable AsHuman asHuman();

      interface Friend {
        @Nonnull String name();

        interface Factory {
          Creator creator();
        }

        interface Creator {
          Friend create(@Nonnull String name);
        }
      }

      interface AsHuman {
        @Nonnull String name();

        @Nullable List<? extends Friend$> friends();

        @Nullable Double height();

        interface Friend$ {
          @Nonnull String name();

          @Nonnull List<? extends Episode> appearsIn();

          @Nullable List<? extends Friend$$> friends();

          interface Friend$$ {
            Fragments fragments();

            interface Fragments {
              HeroDetails heroDetails();

              interface Factory {
                Creator creator();

                HeroDetails.Factory heroDetailsFactory();
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
              Friend$$ create(Fragments fragments);
            }
          }

          interface Factory {
            Creator creator();

            Friend$$.Factory friend$$Factory();
          }

          interface Creator {
            Friend$ create(@Nonnull String name, @Nonnull List<? extends Episode> appearsIn,
                @Nullable List<? extends Friend$$> friends);
          }
        }

        interface Factory {
          Creator creator();

          Friend$.Factory friend$Factory();
        }

        interface Creator {
          AsHuman create(@Nonnull String name, @Nullable List<? extends Friend$> friends,
              @Nullable Double height);
        }
      }

      interface Factory {
        Creator creator();

        Friend.Factory friendFactory();

        AsHuman.Factory asHumanFactory();
      }

      interface Creator {
        Hero create(@Nonnull String name, @Nullable List<? extends Friend> friends,
            @Nullable AsHuman asHuman);
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
