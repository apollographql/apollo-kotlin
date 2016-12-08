package com.example.directives;

import java.lang.String;
import javax.annotation.Nullable;

public interface HeroNameDirective {
  @Nullable Hero hero();

  interface Hero {
    @Nullable String name();
  }
}
