package com.example.two_heroes_unique;

import java.lang.String;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface TwoHeroesUnique {
  @Nullable R2 r2();

  @Nullable Luke luke();

  interface R2 {
    @Nonnull String name();
  }

  interface Luke {
    long id();

    @Nonnull String name();
  }
}
