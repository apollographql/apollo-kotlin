package com.example.simple_inline_fragment;

import java.lang.Float;
import java.lang.String;
import javax.annotation.Nullable;

public interface Query {
  @Nullable Hero hero();

  interface Hero {
    String name();

    @Nullable AsHuman asHuman();

    @Nullable AsDroid asDroid();

    interface AsHuman {
      String name();

      @Nullable Float height();
    }

    interface AsDroid {
      String name();

      @Nullable String primaryFunction();
    }
  }
}
