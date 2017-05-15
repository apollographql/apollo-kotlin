package com.example.custom_scalar_type;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.internal.Optional;
import com.example.custom_scalar_type.type.CustomType;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Date;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    birthDate\n"
      + "    appearanceDates\n"
      + "    fieldWithUnsupportedType\n"
      + "    profileLink\n"
      + "    links\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final Operation.Variables variables;

  public TestQuery() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Optional<TestQuery.Data> wrapData(TestQuery.Data data) {
    return Optional.fromNullable(data);
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<TestQuery.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    Builder() {
    }

    public TestQuery build() {
      return new TestQuery();
    }
  }

  public static class Data implements Operation.Data {
    private final Optional<Hero> hero;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable Hero hero) {
      this.hero = Optional.fromNullable(hero);
    }

    public Optional<Hero> hero() {
      return this.hero;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "hero=" + hero
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.hero == null) ? (that.hero == null) : this.hero.equals(that.hero));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (hero == null) ? 0 : hero.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Hero.Mapper heroFieldMapper = new Hero.Mapper();

      final Field[] fields = {
        Field.forObject("hero", "hero", null, true, new Field.ObjectReader<Hero>() {
          @Override public Hero read(final ResponseReader reader) throws IOException {
            return heroFieldMapper.map(reader);
          }
        })
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final Hero hero = reader.read(fields[0]);
        return new Data(hero);
      }
    }
  }

  public static class Hero {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final @Nonnull Date birthDate;

    private final @Nonnull List<Date> appearanceDates;

    private final @Nonnull Object fieldWithUnsupportedType;

    private final @Nonnull String profileLink;

    private final @Nonnull List<String> links;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Hero(@Nonnull String __typename, @Nonnull String name, @Nonnull Date birthDate,
        @Nonnull List<Date> appearanceDates, @Nonnull Object fieldWithUnsupportedType,
        @Nonnull String profileLink, @Nonnull List<String> links) {
      this.__typename = __typename;
      this.name = name;
      this.birthDate = birthDate;
      this.appearanceDates = appearanceDates;
      this.fieldWithUnsupportedType = fieldWithUnsupportedType;
      this.profileLink = profileLink;
      this.links = links;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The name of the character
     */
    public @Nonnull String name() {
      return this.name;
    }

    /**
     * The date character was born.
     */
    public @Nonnull Date birthDate() {
      return this.birthDate;
    }

    /**
     * The dates of appearances
     */
    public @Nonnull List<Date> appearanceDates() {
      return this.appearanceDates;
    }

    /**
     * The date character was born.
     */
    public @Nonnull Object fieldWithUnsupportedType() {
      return this.fieldWithUnsupportedType;
    }

    /**
     * Profile link
     */
    public @Nonnull String profileLink() {
      return this.profileLink;
    }

    /**
     * Links
     */
    public @Nonnull List<String> links() {
      return this.links;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Hero{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "birthDate=" + birthDate + ", "
          + "appearanceDates=" + appearanceDates + ", "
          + "fieldWithUnsupportedType=" + fieldWithUnsupportedType + ", "
          + "profileLink=" + profileLink + ", "
          + "links=" + links
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Hero) {
        Hero that = (Hero) o;
        return ((this.__typename == null) ? (that.__typename == null) : this.__typename.equals(that.__typename))
         && ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
         && ((this.birthDate == null) ? (that.birthDate == null) : this.birthDate.equals(that.birthDate))
         && ((this.appearanceDates == null) ? (that.appearanceDates == null) : this.appearanceDates.equals(that.appearanceDates))
         && ((this.fieldWithUnsupportedType == null) ? (that.fieldWithUnsupportedType == null) : this.fieldWithUnsupportedType.equals(that.fieldWithUnsupportedType))
         && ((this.profileLink == null) ? (that.profileLink == null) : this.profileLink.equals(that.profileLink))
         && ((this.links == null) ? (that.links == null) : this.links.equals(that.links));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (__typename == null) ? 0 : __typename.hashCode();
        h *= 1000003;
        h ^= (name == null) ? 0 : name.hashCode();
        h *= 1000003;
        h ^= (birthDate == null) ? 0 : birthDate.hashCode();
        h *= 1000003;
        h ^= (appearanceDates == null) ? 0 : appearanceDates.hashCode();
        h *= 1000003;
        h ^= (fieldWithUnsupportedType == null) ? 0 : fieldWithUnsupportedType.hashCode();
        h *= 1000003;
        h ^= (profileLink == null) ? 0 : profileLink.hashCode();
        h *= 1000003;
        h ^= (links == null) ? 0 : links.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Hero> {
      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false),
        Field.forCustomType("birthDate", "birthDate", null, false, CustomType.DATE),
        Field.forList("appearanceDates", "appearanceDates", null, false, new Field.ListReader<Date>() {
          @Override public Date read(final Field.ListItemReader reader) throws IOException {
            return reader.readCustomType(CustomType.DATE);
          }
        }),
        Field.forCustomType("fieldWithUnsupportedType", "fieldWithUnsupportedType", null, false, CustomType.UNSUPPORTEDTYPE),
        Field.forCustomType("profileLink", "profileLink", null, false, CustomType.URL),
        Field.forList("links", "links", null, false, new Field.ListReader<String>() {
          @Override public String read(final Field.ListItemReader reader) throws IOException {
            return reader.readCustomType(CustomType.URL);
          }
        })
      };

      @Override
      public Hero map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String name = reader.read(fields[1]);
        final Date birthDate = reader.read(fields[2]);
        final List<Date> appearanceDates = reader.read(fields[3]);
        final Object fieldWithUnsupportedType = reader.read(fields[4]);
        final String profileLink = reader.read(fields[5]);
        final List<String> links = reader.read(fields[6]);
        return new Hero(__typename, name, birthDate, appearanceDates, fieldWithUnsupportedType, profileLink, links);
      }
    }
  }
}
