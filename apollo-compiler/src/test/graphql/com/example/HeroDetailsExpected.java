package com.example;

import java.lang.Integer;
import java.lang.String;
import java.util.List;
import javax.annotation.Nullable;

public interface HeroDetails {
  @Nullable Hero hero();

  interface Hero {
    String name();

    FriendsConnection friendsConnection();

    interface FriendsConnection {
      @Nullable Integer totalCount();

      @Nullable List<Edge> edges();

      interface Edge {
        @Nullable Node node();

        interface Node {
          String name();
        }
      }
    }
  }
}
