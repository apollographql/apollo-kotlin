package com.example;

import java.lang.String;
import javax.annotation.Nullable;

public interface TwoHeroes {
  @Nullable R2 r2();

  @Nullable Luke luke();

  interface R2 {
    String name();
  }

  interface Luke {
    String name();
  }
}
