package com.example.fragments_with_type_condition;

import java.lang.String;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface DroidDetails {
  String FRAGMENT_DEFINITION = "fragment DroidDetails on Droid {\n"
      + "  name\n"
      + "  primaryFunction\n"
      + "}";

  @Nonnull String name();

  @Nullable String primaryFunction();
}
