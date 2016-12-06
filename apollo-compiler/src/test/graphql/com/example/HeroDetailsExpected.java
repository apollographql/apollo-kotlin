package com.example;

import java.lang.Integer;
import java.lang.String;
import java.util.List;

public interface HeroDetails {
  Character hero();

  interface Character {
    String name();

    CharacterFriendsConnection friendsConnection();
  }

  interface CharacterFriendsConnection {
    Integer totalCount();

    List<CharacterFriendsConnectionFriendsEdge> edges();
  }

  interface CharacterFriendsConnectionFriendsEdge {
    CharacterFriendsConnectionFriendsEdgeCharacter node();
  }

  interface CharacterFriendsConnectionFriendsEdgeCharacter {
    String name();
  }
}
