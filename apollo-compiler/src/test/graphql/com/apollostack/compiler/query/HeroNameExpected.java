package com.apollostack.compiler.query;

import java.lang.String;

public interface HeroName {
  Character hero();

  interface Character {
    String name();
  }
}
