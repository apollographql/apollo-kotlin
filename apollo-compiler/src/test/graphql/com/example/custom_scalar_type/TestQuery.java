package com.example.custom_scalar_type;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.Utils;
import com.example.custom_scalar_type.type.CustomType;
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

  private static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "TestQuery";
    }
  };

  private final Operation.Variables variables;

  public TestQuery() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String operationId() {
    return "97c3220729cb6b43bfbb66f24be53a88482515ea92d3ba9783fce882bc58fc53";
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

  @Override
  public OperationName name() {
    return OPERATION_NAME;
  }

  public static final class Builder {
    Builder() {
    }

    public TestQuery build() {
      return new TestQuery();
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("hero", "hero", null, true)
    };

    final Optional<Hero> hero;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable Hero hero) {
      this.hero = Optional.fromNullable(hero);
    }

    public Optional<Hero> hero() {
      return this.hero;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], hero.isPresent() ? hero.get().marshaller() : null);
        }
      };
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
        return this.hero.equals(that.hero);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= hero.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Hero.Mapper heroFieldMapper = new Hero.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final Hero hero = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<Hero>() {
          @Override
          public Hero read(ResponseReader reader) {
            return heroFieldMapper.map(reader);
          }
        });
        return new Data(hero);
      }
    }
  }

  public static class Hero {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false),
      ResponseField.forCustomType("birthDate", "birthDate", null, false, CustomType.DATE),
      ResponseField.forCustomList("appearanceDates", "appearanceDates", null, false),
      ResponseField.forCustomType("fieldWithUnsupportedType", "fieldWithUnsupportedType", null, false, CustomType.UNSUPPORTEDTYPE),
      ResponseField.forCustomType("profileLink", "profileLink", null, false, CustomType.URL),
      ResponseField.forCustomList("links", "links", null, false)
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final @Nonnull Date birthDate;

    final @Nonnull List<Date> appearanceDates;

    final @Nonnull Object fieldWithUnsupportedType;

    final @Nonnull String profileLink;

    final @Nonnull List<String> links;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Hero(@Nonnull String __typename, @Nonnull String name, @Nonnull Date birthDate,
        @Nonnull List<Date> appearanceDates, @Nonnull Object fieldWithUnsupportedType,
        @Nonnull String profileLink, @Nonnull List<String> links) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.name = Utils.checkNotNull(name, "name == null");
      this.birthDate = Utils.checkNotNull(birthDate, "birthDate == null");
      this.appearanceDates = Utils.checkNotNull(appearanceDates, "appearanceDates == null");
      this.fieldWithUnsupportedType = Utils.checkNotNull(fieldWithUnsupportedType, "fieldWithUnsupportedType == null");
      this.profileLink = Utils.checkNotNull(profileLink, "profileLink == null");
      this.links = Utils.checkNotNull(links, "links == null");
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

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeCustom((ResponseField.CustomTypeField) $responseFields[2], birthDate);
          writer.writeList($responseFields[3], new ResponseWriter.ListWriter() {
            @Override
            public void write(ResponseWriter.ListItemWriter listItemWriter) {
              for (Date $item : appearanceDates) {
                listItemWriter.writeCustom(CustomType.DATE, $item);
              }
            }
          });
          writer.writeCustom((ResponseField.CustomTypeField) $responseFields[4], fieldWithUnsupportedType);
          writer.writeCustom((ResponseField.CustomTypeField) $responseFields[5], profileLink);
          writer.writeList($responseFields[6], new ResponseWriter.ListWriter() {
            @Override
            public void write(ResponseWriter.ListItemWriter listItemWriter) {
              for (String $item : links) {
                listItemWriter.writeCustom(CustomType.URL, $item);
              }
            }
          });
        }
      };
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
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.birthDate.equals(that.birthDate)
         && this.appearanceDates.equals(that.appearanceDates)
         && this.fieldWithUnsupportedType.equals(that.fieldWithUnsupportedType)
         && this.profileLink.equals(that.profileLink)
         && this.links.equals(that.links);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= name.hashCode();
        h *= 1000003;
        h ^= birthDate.hashCode();
        h *= 1000003;
        h ^= appearanceDates.hashCode();
        h *= 1000003;
        h ^= fieldWithUnsupportedType.hashCode();
        h *= 1000003;
        h ^= profileLink.hashCode();
        h *= 1000003;
        h ^= links.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Hero> {
      @Override
      public Hero map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final Date birthDate = reader.readCustomType((ResponseField.CustomTypeField) $responseFields[2]);
        final List<Date> appearanceDates = reader.readList($responseFields[3], new ResponseReader.ListReader<Date>() {
          @Override
          public Date read(ResponseReader.ListItemReader reader) {
            return reader.readCustomType(CustomType.DATE);
          }
        });
        final Object fieldWithUnsupportedType = reader.readCustomType((ResponseField.CustomTypeField) $responseFields[4]);
        final String profileLink = reader.readCustomType((ResponseField.CustomTypeField) $responseFields[5]);
        final List<String> links = reader.readList($responseFields[6], new ResponseReader.ListReader<String>() {
          @Override
          public String read(ResponseReader.ListItemReader reader) {
            return reader.readCustomType(CustomType.URL);
          }
        });
        return new Hero(__typename, name, birthDate, appearanceDates, fieldWithUnsupportedType, profileLink, links);
      }
    }
  }
}
