package com.example;

import java.lang.String;
import javax.annotation.Nullable;

public interface HeroName {
  @Nullable Hero hero();

  interface Hero {
    String name();
  }
}
