package com.example.unique_type_name;

import java.lang.Float;
import java.lang.String;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Query {
  @Nullable Hero hero();

  interface Hero {
    @Nonnull String name();

    @Nullable List<Friend> friends();

    @Nullable AsHuman asHuman();

    interface Friend {
      @Nonnull String name();
    }

    interface AsHuman {
      @Nonnull String name();

      @Nullable List<Friend$> friends();

      @Nullable Float height();

      interface Friend$ {
        @Nonnull String name();

        @Nonnull List<Episode> appearsIn();

        @Nullable List<Friend$$> friends();

        interface Friend$$ {
          Fragments fragments();

          interface Fragments {
            HeroDetails heroDetails();
          }
        }
      }
    }
  }
}
