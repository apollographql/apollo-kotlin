package com.example.simple_fragment.fragment;

import java.lang.String;
import javax.annotation.Nonnull;

public interface HeroDetails {
  String FRAGMENT_DEFINITION = "fragment HeroDetails on Character {\n"
      + "  __typename\n"
      + "  name\n"
      + "}";

  String TYPE_CONDITION = "Character";

  @Nonnull String name();
}
