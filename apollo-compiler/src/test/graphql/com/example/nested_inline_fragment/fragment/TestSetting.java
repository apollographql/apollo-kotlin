package com.example.nested_inline_fragment.fragment;

import com.apollographql.apollo.api.GraphqlFragment;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.Utils;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Generated("Apollo GraphQL")
public interface TestSetting extends GraphqlFragment {
  String FRAGMENT_DEFINITION = "fragment TestSetting on Setting {\n"
      + "  __typename\n"
      + "  value {\n"
      + "    __typename\n"
      + "    ... on StringListSettingValue {\n"
      + "      list\n"
      + "    }\n"
      + "  }\n"
      + "  ... on SelectSetting {\n"
      + "    options {\n"
      + "      __typename\n"
      + "      allowFreeText\n"
      + "      id\n"
      + "      label\n"
      + "    }\n"
      + "  }\n"
      + "}";

  List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "BooleanSetting", "SelectSetting", "StringListSetting", "TextSetting", "TypedStringListSetting"));

  @NotNull String __typename();

  Optional<? extends Value> value();

  ResponseFieldMarshaller marshaller();

  final class Mapper implements ResponseFieldMapper<TestSetting> {
    final AsSelectSetting.Mapper asSelectSettingFieldMapper = new AsSelectSetting.Mapper();

    @Override
    public TestSetting map(ResponseReader reader) {
      final AsSelectSetting asSelectSetting = reader.readConditional(ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("SelectSetting")), new ResponseReader.ConditionalTypeReader<AsSelectSetting>() {
        @Override
        public AsSelectSetting read(String conditionalType, ResponseReader reader) {
          return asSelectSettingFieldMapper.map(reader);
        }
      });
      if (asSelectSetting != null) {
        return asSelectSetting;
      }
      return null;
    }
  }

  interface Value {
    @NotNull String __typename();

    ResponseFieldMarshaller marshaller();

    final class Mapper implements ResponseFieldMapper<Value> {
      final AsStringListSettingValue.Mapper asStringListSettingValueFieldMapper = new AsStringListSettingValue.Mapper();

      @Override
      public Value map(ResponseReader reader) {
        final AsStringListSettingValue asStringListSettingValue = reader.readConditional(ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("StringListSettingValue")), new ResponseReader.ConditionalTypeReader<AsStringListSettingValue>() {
          @Override
          public AsStringListSettingValue read(String conditionalType, ResponseReader reader) {
            return asStringListSettingValueFieldMapper.map(reader);
          }
        });
        if (asStringListSettingValue != null) {
          return asStringListSettingValue;
        }
        return null;
      }
    }
  }

  class AsStringListSettingValue implements Value {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("list", "list", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final Optional<List<String>> list;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsStringListSettingValue(@NotNull String __typename, @Nullable List<String> list) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.list = Optional.fromNullable(list);
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    public Optional<List<String>> list() {
      return this.list;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeList($responseFields[1], list.isPresent() ? list.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(Object value, ResponseWriter.ListItemWriter listItemWriter) {
              listItemWriter.writeString(value);
            }
          });
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsStringListSettingValue{"
          + "__typename=" + __typename + ", "
          + "list=" + list
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof AsStringListSettingValue) {
        AsStringListSettingValue that = (AsStringListSettingValue) o;
        return this.__typename.equals(that.__typename)
         && this.list.equals(that.list);
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
        h ^= list.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<AsStringListSettingValue> {
      @Override
      public AsStringListSettingValue map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final List<String> list = reader.readList($responseFields[1], new ResponseReader.ListReader<String>() {
          @Override
          public String read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readString();
          }
        });
        return new AsStringListSettingValue(__typename, list);
      }
    }
  }

  class AsSelectSetting implements TestSetting {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("value", "value", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("options", "options", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final Optional<Value1> value;

    final Optional<List<Option>> options;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsSelectSetting(@NotNull String __typename, @Nullable Value1 value,
        @Nullable List<Option> options) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.value = Optional.fromNullable(value);
      this.options = Optional.fromNullable(options);
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    public Optional<Value1> value() {
      return this.value;
    }

    public Optional<List<Option>> options() {
      return this.options;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeObject($responseFields[1], value.isPresent() ? value.get().marshaller() : null);
          writer.writeList($responseFields[2], options.isPresent() ? options.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(Object value, ResponseWriter.ListItemWriter listItemWriter) {
              listItemWriter.writeObject(((Option) value).marshaller());
            }
          });
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsSelectSetting{"
          + "__typename=" + __typename + ", "
          + "value=" + value + ", "
          + "options=" + options
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof AsSelectSetting) {
        AsSelectSetting that = (AsSelectSetting) o;
        return this.__typename.equals(that.__typename)
         && this.value.equals(that.value)
         && this.options.equals(that.options);
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
        h ^= value.hashCode();
        h *= 1000003;
        h ^= options.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<AsSelectSetting> {
      final Value1.Mapper value1FieldMapper = new Value1.Mapper();

      final Option.Mapper optionFieldMapper = new Option.Mapper();

      @Override
      public AsSelectSetting map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Value1 value = reader.readObject($responseFields[1], new ResponseReader.ObjectReader<Value1>() {
          @Override
          public Value1 read(ResponseReader reader) {
            return value1FieldMapper.map(reader);
          }
        });
        final List<Option> options = reader.readList($responseFields[2], new ResponseReader.ListReader<Option>() {
          @Override
          public Option read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readObject(new ResponseReader.ObjectReader<Option>() {
              @Override
              public Option read(ResponseReader reader) {
                return optionFieldMapper.map(reader);
              }
            });
          }
        });
        return new AsSelectSetting(__typename, value, options);
      }
    }
  }

  interface Value1 extends Value {
    @NotNull String __typename();

    ResponseFieldMarshaller marshaller();

    final class Mapper implements ResponseFieldMapper<Value1> {
      final AsStringListSettingValue1.Mapper asStringListSettingValue1FieldMapper = new AsStringListSettingValue1.Mapper();

      @Override
      public Value1 map(ResponseReader reader) {
        final AsStringListSettingValue1 asStringListSettingValue = reader.readConditional(ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("StringListSettingValue")), new ResponseReader.ConditionalTypeReader<AsStringListSettingValue1>() {
          @Override
          public AsStringListSettingValue1 read(String conditionalType, ResponseReader reader) {
            return asStringListSettingValue1FieldMapper.map(reader);
          }
        });
        if (asStringListSettingValue != null) {
          return asStringListSettingValue;
        }
        return null;
      }
    }
  }

  class AsStringListSettingValue1 implements Value1 {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("list", "list", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final Optional<List<String>> list;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsStringListSettingValue1(@NotNull String __typename, @Nullable List<String> list) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.list = Optional.fromNullable(list);
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    public Optional<List<String>> list() {
      return this.list;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeList($responseFields[1], list.isPresent() ? list.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(Object value, ResponseWriter.ListItemWriter listItemWriter) {
              listItemWriter.writeString(value);
            }
          });
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsStringListSettingValue1{"
          + "__typename=" + __typename + ", "
          + "list=" + list
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof AsStringListSettingValue1) {
        AsStringListSettingValue1 that = (AsStringListSettingValue1) o;
        return this.__typename.equals(that.__typename)
         && this.list.equals(that.list);
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
        h ^= list.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<AsStringListSettingValue1> {
      @Override
      public AsStringListSettingValue1 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final List<String> list = reader.readList($responseFields[1], new ResponseReader.ListReader<String>() {
          @Override
          public String read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readString();
          }
        });
        return new AsStringListSettingValue1(__typename, list);
      }
    }
  }

  class Option {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forBoolean("allowFreeText", "allowFreeText", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("id", "id", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("label", "label", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final boolean allowFreeText;

    final @NotNull String id;

    final @NotNull String label;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Option(@NotNull String __typename, boolean allowFreeText, @NotNull String id,
        @NotNull String label) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.allowFreeText = allowFreeText;
      this.id = Utils.checkNotNull(id, "id == null");
      this.label = Utils.checkNotNull(label, "label == null");
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    public boolean allowFreeText() {
      return this.allowFreeText;
    }

    public @NotNull String id() {
      return this.id;
    }

    public @NotNull String label() {
      return this.label;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeBoolean($responseFields[1], allowFreeText);
          writer.writeString($responseFields[2], id);
          writer.writeString($responseFields[3], label);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Option{"
          + "__typename=" + __typename + ", "
          + "allowFreeText=" + allowFreeText + ", "
          + "id=" + id + ", "
          + "label=" + label
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Option) {
        Option that = (Option) o;
        return this.__typename.equals(that.__typename)
         && this.allowFreeText == that.allowFreeText
         && this.id.equals(that.id)
         && this.label.equals(that.label);
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
        h ^= Boolean.valueOf(allowFreeText).hashCode();
        h *= 1000003;
        h ^= id.hashCode();
        h *= 1000003;
        h ^= label.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Option> {
      @Override
      public Option map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final boolean allowFreeText = reader.readBoolean($responseFields[1]);
        final String id = reader.readString($responseFields[2]);
        final String label = reader.readString($responseFields[3]);
        return new Option(__typename, allowFreeText, id, label);
      }
    }
  }
}
