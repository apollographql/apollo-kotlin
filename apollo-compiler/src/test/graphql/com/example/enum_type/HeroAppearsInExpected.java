package com.example.enum_type;

import java.lang.String;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface HeroAppearsIn {
  @Nullable Hero hero();

  interface Hero {
    @Nonnull String name();

    @Nonnull List<Episode> appearsIn();
  }
}
