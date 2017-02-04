package com.example.fragment_in_fragment;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.example.fragment_in_fragment.fragment.PilotFragment;
import com.example.fragment_in_fragment.fragment.StarshipFragment;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query AllStarships {\n"
      + "  allStarships(first: 7) {\n"
      + "    edges {\n"
      + "      node {\n"
      + "        ...starshipFragment\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + StarshipFragment.FRAGMENT_DEFINITION + "\n"
   + PilotFragment.FRAGMENT_DEFINITION;

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
    @Nullable AllStarship allStarships();

    interface AllStarship {
      @Nullable List<? extends Edge> edges();

      interface Edge {
        @Nullable Node node();

        interface Node {
          Fragments fragments();

          interface Fragments {
            StarshipFragment starshipFragment();

            interface Factory {
              Creator creator();
            }

            interface Creator {
              Fragments create(StarshipFragment starshipFragment);
            }
          }

          interface Factory {
            Creator creator();

            Fragments.Factory fragmentsFactory();
          }

          interface Creator {
            Node create(Fragments fragments);
          }
        }

        interface Factory {
          Creator creator();

          Node.Factory nodeFactory();
        }

        interface Creator {
          Edge create(@Nullable Node node);
        }
      }

      interface Factory {
        Creator creator();

        Edge.Factory edgeFactory();
      }

      interface Creator {
        AllStarship create(@Nullable List<? extends Edge> edges);
      }
    }

    interface Factory {
      Creator creator();

      AllStarship.Factory allStarshipFactory();
    }

    interface Creator {
      Data create(@Nullable AllStarship allStarships);
    }
  }
}
