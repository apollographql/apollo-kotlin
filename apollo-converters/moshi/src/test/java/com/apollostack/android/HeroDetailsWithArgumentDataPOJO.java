package com.apollostack.android;

import javax.annotation.Nullable;

class HeroDetailsWithArgumentDataPOJO implements HeroDetailsWithArgument.Data {
  private final HeroPOJO hero;

  HeroDetailsWithArgumentDataPOJO(HeroPOJO hero) {
    this.hero = hero;
  }

  @Nullable @Override public HeroPOJO hero() {
    return hero;
  }

  static class HeroPOJO implements HeroDetailsWithArgument.Data.Hero {
    private final String name;

    HeroPOJO(String name) {
      this.name = name;
    }

    @Nullable @Override public String name() {
      return name;
    }
  }
}
