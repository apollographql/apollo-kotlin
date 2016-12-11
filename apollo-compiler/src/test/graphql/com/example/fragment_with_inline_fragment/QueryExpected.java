package com.example.fragment_with_inline_fragment;

import java.lang.String;
import java.util.List;
import javax.annotation.Nullable;

public interface Query {
  @Nullable Hero hero();

  interface Hero {
    String name();

    List<Episode> appearsIn();

    Fragments fragments();

    interface Fragments {
      HeroDetails heroDetails();
    }
  }
}
