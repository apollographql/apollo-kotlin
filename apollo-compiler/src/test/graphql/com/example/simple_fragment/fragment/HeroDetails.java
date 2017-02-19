package com.example.simple_fragment.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;

@Generated("Apollo GraphQL")
public class HeroDetails {
  public static final Creator CREATOR = new Creator() {
    @Override
    public @Nonnull HeroDetails create(@Nonnull String name) {
      return new HeroDetails(name);
    }
  };

  public static final Factory FACTORY = new Factory() {
    @Override
    public @Nonnull Creator creator() {
      return CREATOR;
    }
  };

  public static final String FRAGMENT_DEFINITION = "fragment HeroDetails on Character {\n"
      + "  __typename\n"
      + "  name\n"
      + "}";

  public static final String TYPE_CONDITION = "Character";

  private final @Nonnull String name;

  public HeroDetails(@Nonnull String name) {
    this.name = name;
  }

  public @Nonnull String name() {
    return this.name;
  }

  @Override
  public String toString() {
    return "HeroDetails{"
      + "name=" + name
      + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof HeroDetails) {
      HeroDetails that = (HeroDetails) o;
      return ((this.name == null) ? (that.name == null) : this.name.equals(that.name));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= (name == null) ? 0 : name.hashCode();
    return h;
  }

  public static final class Mapper implements ResponseFieldMapper<HeroDetails> {
    final Factory factory;

    final Field[] fields = {
      Field.forString("name", "name", null, false)
    };

    public Mapper(@Nonnull Factory factory) {
      this.factory = factory;
    }

    @Override
    public HeroDetails map(ResponseReader reader) throws IOException {
      final __ContentValues contentValues = new __ContentValues();
      reader.read(new ResponseReader.ValueHandler() {
        @Override
        public void handle(final int fieldIndex, final Object value) throws IOException {
          switch (fieldIndex) {
            case 0: {
              contentValues.name = (String) value;
              break;
            }
          }
        }
      }, fields);
      return factory.creator().create(contentValues.name);
    }

    static final class __ContentValues {
      String name;
    }
  }

  public interface Factory {
    @Nonnull Creator creator();
  }

  public interface Creator {
    @Nonnull HeroDetails create(@Nonnull String name);
  }
}
