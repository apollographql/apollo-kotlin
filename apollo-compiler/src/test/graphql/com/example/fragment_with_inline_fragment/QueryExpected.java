package com.example.fragment_with_inline_fragment;

import java.lang.String;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Query {
  @Nullable Hero hero();

  interface Hero {
    @Nonnull String name();

    @Nonnull List<Episode> appearsIn();

    Fragments fragments();

    interface Fragments {
      HeroDetails heroDetails();
    }
  }
}
