package com.example.inline_fragments_with_friends;

import java.lang.Float;
import java.lang.String;
import java.util.List;
import javax.annotation.Nullable;

public interface Query {
  @Nullable Hero hero();

  interface Hero {
    String name();

    @Nullable List<Friend> friends();

    @Nullable
    AsHuman asHuman();

    @Nullable
    AsDroid asDroid();

    interface Friend {
      String name();
    }

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
