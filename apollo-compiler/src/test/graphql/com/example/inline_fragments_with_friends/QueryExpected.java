package com.example.inline_fragments_with_friends;

import java.lang.Float;
import java.lang.String;
import java.util.List;
import javax.annotation.Nullable;

public interface Query {
  @Nullable Hero hero();

  interface Hero {
    String name();

    @Nullable AsHuman asHuman();

    @Nullable AsDroid asDroid();

    interface AsHuman {
      String name();

      @Nullable List<Friend> friends();

      @Nullable Float height();

      interface Friend {
        String name();

        List<Episode> appearsIn();
      }
    }

    interface AsDroid {
      String name();

      @Nullable List<Friend> friends();

      @Nullable String primaryFunction();

      interface Friend {
        String name();

        long id();
      }
    }
  }
}
