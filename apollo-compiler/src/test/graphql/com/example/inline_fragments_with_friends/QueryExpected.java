package com.example.inline_fragments_with_friends;

import java.lang.Float;
import java.lang.String;
import java.util.List;
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

      @Nullable List<Friend> friends();

      @Nullable Float height();

      interface Friend {
        @Nonnull String name();

        @Nonnull List<Episode> appearsIn();
      }
    }

    interface AsDroid {
      @Nonnull String name();

      @Nullable List<Friend> friends();

      @Nullable String primaryFunction();

      interface Friend {
        @Nonnull String name();

        long id();
      }
    }
  }
}
