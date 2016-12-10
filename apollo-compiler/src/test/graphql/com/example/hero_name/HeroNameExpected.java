package com.example.hero_name;

import java.lang.String;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface HeroName {
  @Nullable Hero hero();

  interface Hero {
    @Nonnull String name();
  }
}
