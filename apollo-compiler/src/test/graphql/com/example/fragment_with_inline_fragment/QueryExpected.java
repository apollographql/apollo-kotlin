package com.example.fragment_with_inline_fragment;

import java.lang.String;
import javax.annotation.Nullable;

public interface Query {
  @Nullable Hero hero();

  interface Hero {
    String name();

    Fragments fragments();

    interface Fragments {
      HeroDetails heroDetails();
    }
  }
}
