package com.example.fragment_in_fragment.fragment;

import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public interface PilotFragment {
  String FRAGMENT_DEFINITION = "fragment pilotFragment on Person {\n"
      + "  name\n"
      + "  homeworld {\n"
      + "    name\n"
      + "  }\n"
      + "}";

  String TYPE_CONDITION = "Person";

  @Nullable String name();

  @Nullable Homeworld homeworld();

  interface Homeworld {
    @Nullable String name();

    interface Factory {
      Creator creator();
    }

    interface Creator {
      Homeworld create(@Nullable String name);
    }
  }

  interface Factory {
    Creator creator();

    Homeworld.Factory homeworldFactory();
  }

  interface Creator {
    PilotFragment create(@Nullable String name, @Nullable Homeworld homeworld);
  }
}
