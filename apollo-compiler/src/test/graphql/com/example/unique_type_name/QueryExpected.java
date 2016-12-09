package com.example.unique_type_name;

import java.lang.Float;
import java.lang.String;
import java.util.List;
import javax.annotation.Nullable;

public interface Query {
  @Nullable Hero hero();

  interface Hero {
    String name();

    @Nullable List<Friend> friends();

    @Nullable AsHuman asHuman();

    interface Friend {
      String name();
    }

    interface AsHuman {
      String name();

      @Nullable List<Friend$> friends();

      @Nullable Float height();

      interface Friend$ {
        String name();

        List<Episode> appearsIn();

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
