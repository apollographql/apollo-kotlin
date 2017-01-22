package com.example.fragments_with_type_condition.fragment;

import java.lang.String;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface DroidDetails {
  String FRAGMENT_DEFINITION = "fragment DroidDetails on Droid {\n"
      + "  name\n"
      + "  primaryFunction\n"
      + "}";

  String TYPE_CONDITION = "Droid";

  @Nonnull String name();

  @Nullable String primaryFunction();
}
