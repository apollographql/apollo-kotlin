package com.example;

import java.lang.String;

public interface TwoHeroes {
  Character r2();

  Character luke();

  interface Character {
    String name();
  }
}
