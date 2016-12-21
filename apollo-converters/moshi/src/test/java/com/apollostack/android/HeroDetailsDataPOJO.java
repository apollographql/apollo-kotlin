package com.apollostack.android;

import java.util.List;

import javax.annotation.Nullable;

class HeroDetailsDataPOJO implements HeroDetails.Data {
  private final AllPeoplePOJO allPeople;

  HeroDetailsDataPOJO(AllPeoplePOJO allPeople) {
    this.allPeople = allPeople;
  }

  static class AllPeoplePOJO implements AllPeople {
    private final List<AllPeoplePOJO.PeoplePOJO> people;

    AllPeoplePOJO(List<AllPeoplePOJO.PeoplePOJO> people) {
      this.people = people;
    }

    static class PeoplePOJO implements People {
      private final String name;

      PeoplePOJO(String name) {
        this.name = name;
      }

      @Nullable @Override public String name() {
        return name;
      }
    }

    @Nullable @Override public List<AllPeoplePOJO.PeoplePOJO> people() {
      return people;
    }
  }

  @Nullable @Override public AllPeoplePOJO allPeople() {
    return allPeople;
  }
}
