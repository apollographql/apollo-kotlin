package com.example.simple_fragment;

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
