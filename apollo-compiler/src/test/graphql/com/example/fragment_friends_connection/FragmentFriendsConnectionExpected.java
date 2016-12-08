package com.example.fragment_friends_connection;

import javax.annotation.Nullable;

public interface FragmentFriendsConnection {
  @Nullable Hero hero();

  interface Hero {
    Fragments fragments();

    interface Fragments {
      HeroDetails heroDetails();
    }
  }
}
