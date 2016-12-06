package com.example;

import java.lang.String;

public interface HeroName {
  Character hero();

  interface Character {
    String name();
  }
}
