package com.example.two_heroes;

import java.lang.String;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface TwoHeroes {
  @Nullable R2 r2();

  @Nullable Luke luke();

  interface R2 {
    @Nonnull String name();
  }

  interface Luke {
    @Nonnull String name();
  }
}
