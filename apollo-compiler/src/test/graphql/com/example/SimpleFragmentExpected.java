package com.example;

import javax.annotation.Nullable;

public interface SimpleFragment {
  @Nullable Hero hero();

  interface Hero {
    Fragments fragments();

    interface Fragments {
      HeroDetailsFragment heroDetailsFragment();
    }
  }
}
