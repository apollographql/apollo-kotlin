package com.example.fragment_in_fragment.fragment;

import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public interface StarshipFragment {
  String FRAGMENT_DEFINITION = "fragment starshipFragment on Starship {\n"
      + "  id\n"
      + "  name\n"
      + "  pilotConnection {\n"
      + "    edges {\n"
      + "      node {\n"
      + "        ...pilotFragment\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  String TYPE_CONDITION = "Starship";

  @Nonnull String id();

  @Nullable String name();

  @Nullable PilotConnection pilotConnection();

  interface PilotConnection {
    @Nullable List<? extends Edge> edges();

    interface Edge {
      @Nullable Node node();

      interface Node {
        Fragments fragments();

        interface Fragments {
          PilotFragment pilotFragment();

          interface Factory {
            Creator creator();

            PilotFragment.Factory pilotFragmentFactory();
          }

          interface Creator {
            Fragments create(PilotFragment pilotFragment);
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
      PilotConnection create(@Nullable List<? extends Edge> edges);
    }
  }

  interface Factory {
    Creator creator();

    PilotConnection.Factory pilotConnectionFactory();
  }

  interface Creator {
    StarshipFragment create(@Nonnull String id, @Nullable String name,
        @Nullable PilotConnection pilotConnection);
  }
}
