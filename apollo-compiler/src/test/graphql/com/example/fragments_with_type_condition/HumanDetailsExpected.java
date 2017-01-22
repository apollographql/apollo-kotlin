package com.example.fragments_with_type_condition.fragment;

import java.lang.Double;
import java.lang.String;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface HumanDetails {
  String FRAGMENT_DEFINITION = "fragment HumanDetails on Human {\n"
      + "  name\n"
      + "  height\n"
      + "}";

  String TYPE_CONDITION = "Human";

  @Nonnull String name();

  @Nullable Double height();
}
