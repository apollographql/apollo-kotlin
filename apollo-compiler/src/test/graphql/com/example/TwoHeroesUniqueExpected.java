package com.example;

import java.lang.String;

public interface TwoHeroesUnique {
  R2Character r2();

  LukeCharacter luke();

  interface LukeCharacter {
    long id();

    String name();
  }

  interface R2Character {
    String name();
  }
}
