package com.example.simple_inline_fragment;

import java.lang.Float;
import java.lang.String;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Query {
  @Nullable Hero hero();

  interface Hero {
    @Nonnull String name();

    @Nullable AsHuman asHuman();

    @Nullable AsDroid asDroid();

    interface AsHuman {
      @Nonnull String name();

      @Nullable Float height();
    }

    interface AsDroid {
      @Nonnull String name();

      @Nullable String primaryFunction();
    }
  }
}
