package com.example.fragments_with_type_condition;

import javax.annotation.Nullable;

public interface Query {
  @Nullable R2 r2();

  @Nullable Luke luke();

  interface R2 {
    Fragments fragments();

    interface Fragments {
      HumanDetails humanDetails();

      DroidDetails droidDetails();
    }
  }

  interface Luke {
    Fragments fragments();

    interface Fragments {
      HumanDetails humanDetails();

      DroidDetails droidDetails();
    }
  }
}
