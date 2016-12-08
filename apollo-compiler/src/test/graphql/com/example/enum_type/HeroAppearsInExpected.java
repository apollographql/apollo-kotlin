package com.example.enum_type;

import java.lang.String;
import java.util.List;
import javax.annotation.Nullable;

public interface HeroAppearsIn {
  @Nullable Hero hero();

  interface Hero {
    String name();

    List<Episode> appearsIn();
  }
}
