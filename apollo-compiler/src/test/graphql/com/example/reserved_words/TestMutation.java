package com.example.reserved_words;

import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.Utils;
import com.example.reserved_words.type.TestInputType;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Generated;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Generated("Apollo GraphQL")
public final class TestMutation implements Mutation<TestMutation.Data, Optional<TestMutation.Data>, TestMutation.Variables> {
  public static final String OPERATION_ID = null;

  public static final String QUERY_DOCUMENT = null;

  public static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "TestMutation";
    }
  };

  private final TestMutation.Variables variables;

  public TestMutation(@NotNull TestInputType input) {
    Utils.checkNotNull(input, "input == null");
    variables = new TestMutation.Variables(input);
  }

  @Override
  public String operationId() {
    return OPERATION_ID;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Optional<TestMutation.Data> wrapData(TestMutation.Data data) {
    return Optional.fromNullable(data);
  }

  @Override
  public TestMutation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<TestMutation.Data> responseFieldMapper() {
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
    private @NotNull TestInputType input;

    Builder() {
    }

    public Builder input(@NotNull TestInputType input) {
      this.input = input;
      return this;
    }

    public TestMutation build() {
      Utils.checkNotNull(input, "input == null");
      return new TestMutation(input);
    }
  }

  public static final class Variables extends com.apollographql.apollo.api.Operation.Variables {
    private final @NotNull TestInputType input;

    private final transient Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(@NotNull TestInputType input) {
      this.input = input;
      this.valueMap.put("input", input);
    }

    public @NotNull TestInputType input() {
      return input;
    }

    @Override
    public Map<String, Object> valueMap() {
      return Collections.unmodifiableMap(valueMap);
    }

    @Override
    public InputFieldMarshaller marshaller() {
      return new InputFieldMarshaller() {
        @Override
        public void marshal(InputFieldWriter writer) throws IOException {
          writer.writeObject("input", input.marshaller());
        }
      };
    }
  }

  public static class Data implements com.apollographql.apollo.api.Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("abstract", "abstract", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("assert", "assert", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("boolean", "boolean", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("break", "break", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("byte", "byte", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("case", "case", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("catch", "catch", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("char", "char", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("class", "class", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("const", "const", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("continue", "continue", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("default", "default", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("do", "do", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("double", "double", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("else", "else", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("enum", "enum", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("extends", "extends", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("final", "final", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("finally", "finally", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("float", "float", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("for", "for", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("goto", "goto", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("if", "if", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("implements", "implements", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("import", "import", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("instanceof", "instanceof", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("int", "int", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("interface", "interface", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("long", "long", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("native", "native", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("new", "new", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("package", "package", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("private", "private", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("protected", "protected", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("public", "public", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("return", "return", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("short", "short", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("static", "static", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("strictfp", "strictfp", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("super", "super", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("switch", "switch", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("synchronized", "synchronized", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("this", "this", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("throw", "throw", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("throws", "throws", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("transient", "transient", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("try", "try", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("void", "void", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("volatile", "volatile", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("while", "while", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("operation", "operation", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final Optional<String> abstract_;

    final Optional<String> assert_;

    final Optional<String> boolean_;

    final Optional<String> break_;

    final Optional<String> byte_;

    final Optional<String> case_;

    final Optional<String> catch_;

    final Optional<String> char_;

    final Optional<String> class_;

    final Optional<String> const_;

    final Optional<String> continue_;

    final Optional<String> default_;

    final Optional<String> do_;

    final Optional<String> double_;

    final Optional<String> else_;

    final Optional<String> enum_;

    final Optional<String> extends_;

    final Optional<String> final_;

    final Optional<String> finally_;

    final Optional<String> float_;

    final Optional<String> for_;

    final Optional<String> goto_;

    final Optional<String> if_;

    final Optional<String> implements_;

    final Optional<String> import_;

    final Optional<String> instanceof_;

    final Optional<String> int_;

    final Optional<String> interface_;

    final Optional<String> long_;

    final Optional<String> native_;

    final Optional<String> new_;

    final Optional<String> package_;

    final Optional<String> private_;

    final Optional<String> protected_;

    final Optional<String> public_;

    final Optional<String> return_;

    final Optional<String> short_;

    final Optional<String> static_;

    final Optional<String> strictfp_;

    final Optional<String> super_;

    final Optional<String> switch_;

    final Optional<String> synchronized_;

    final Optional<String> this_;

    final Optional<String> throw_;

    final Optional<String> throws_;

    final Optional<String> transient_;

    final Optional<String> try_;

    final Optional<String> void_;

    final Optional<String> volatile_;

    final Optional<String> while_;

    final Optional<Operation> operation;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Data(@Nullable String abstract_, @Nullable String assert_, @Nullable String boolean_,
        @Nullable String break_, @Nullable String byte_, @Nullable String case_,
        @Nullable String catch_, @Nullable String char_, @Nullable String class_,
        @Nullable String const_, @Nullable String continue_, @Nullable String default_,
        @Nullable String do_, @Nullable String double_, @Nullable String else_,
        @Nullable String enum_, @Nullable String extends_, @Nullable String final_,
        @Nullable String finally_, @Nullable String float_, @Nullable String for_,
        @Nullable String goto_, @Nullable String if_, @Nullable String implements_,
        @Nullable String import_, @Nullable String instanceof_, @Nullable String int_,
        @Nullable String interface_, @Nullable String long_, @Nullable String native_,
        @Nullable String new_, @Nullable String package_, @Nullable String private_,
        @Nullable String protected_, @Nullable String public_, @Nullable String return_,
        @Nullable String short_, @Nullable String static_, @Nullable String strictfp_,
        @Nullable String super_, @Nullable String switch_, @Nullable String synchronized_,
        @Nullable String this_, @Nullable String throw_, @Nullable String throws_,
        @Nullable String transient_, @Nullable String try_, @Nullable String void_,
        @Nullable String volatile_, @Nullable String while_, @Nullable Operation operation) {
      this.abstract_ = Optional.fromNullable(abstract_);
      this.assert_ = Optional.fromNullable(assert_);
      this.boolean_ = Optional.fromNullable(boolean_);
      this.break_ = Optional.fromNullable(break_);
      this.byte_ = Optional.fromNullable(byte_);
      this.case_ = Optional.fromNullable(case_);
      this.catch_ = Optional.fromNullable(catch_);
      this.char_ = Optional.fromNullable(char_);
      this.class_ = Optional.fromNullable(class_);
      this.const_ = Optional.fromNullable(const_);
      this.continue_ = Optional.fromNullable(continue_);
      this.default_ = Optional.fromNullable(default_);
      this.do_ = Optional.fromNullable(do_);
      this.double_ = Optional.fromNullable(double_);
      this.else_ = Optional.fromNullable(else_);
      this.enum_ = Optional.fromNullable(enum_);
      this.extends_ = Optional.fromNullable(extends_);
      this.final_ = Optional.fromNullable(final_);
      this.finally_ = Optional.fromNullable(finally_);
      this.float_ = Optional.fromNullable(float_);
      this.for_ = Optional.fromNullable(for_);
      this.goto_ = Optional.fromNullable(goto_);
      this.if_ = Optional.fromNullable(if_);
      this.implements_ = Optional.fromNullable(implements_);
      this.import_ = Optional.fromNullable(import_);
      this.instanceof_ = Optional.fromNullable(instanceof_);
      this.int_ = Optional.fromNullable(int_);
      this.interface_ = Optional.fromNullable(interface_);
      this.long_ = Optional.fromNullable(long_);
      this.native_ = Optional.fromNullable(native_);
      this.new_ = Optional.fromNullable(new_);
      this.package_ = Optional.fromNullable(package_);
      this.private_ = Optional.fromNullable(private_);
      this.protected_ = Optional.fromNullable(protected_);
      this.public_ = Optional.fromNullable(public_);
      this.return_ = Optional.fromNullable(return_);
      this.short_ = Optional.fromNullable(short_);
      this.static_ = Optional.fromNullable(static_);
      this.strictfp_ = Optional.fromNullable(strictfp_);
      this.super_ = Optional.fromNullable(super_);
      this.switch_ = Optional.fromNullable(switch_);
      this.synchronized_ = Optional.fromNullable(synchronized_);
      this.this_ = Optional.fromNullable(this_);
      this.throw_ = Optional.fromNullable(throw_);
      this.throws_ = Optional.fromNullable(throws_);
      this.transient_ = Optional.fromNullable(transient_);
      this.try_ = Optional.fromNullable(try_);
      this.void_ = Optional.fromNullable(void_);
      this.volatile_ = Optional.fromNullable(volatile_);
      this.while_ = Optional.fromNullable(while_);
      this.operation = Optional.fromNullable(operation);
    }

    public Optional<String> abstract_() {
      return this.abstract_;
    }

    public Optional<String> assert_() {
      return this.assert_;
    }

    public Optional<String> boolean_() {
      return this.boolean_;
    }

    public Optional<String> break_() {
      return this.break_;
    }

    public Optional<String> byte_() {
      return this.byte_;
    }

    public Optional<String> case_() {
      return this.case_;
    }

    public Optional<String> catch_() {
      return this.catch_;
    }

    public Optional<String> char_() {
      return this.char_;
    }

    public Optional<String> class_() {
      return this.class_;
    }

    public Optional<String> const_() {
      return this.const_;
    }

    public Optional<String> continue_() {
      return this.continue_;
    }

    public Optional<String> default_() {
      return this.default_;
    }

    public Optional<String> do_() {
      return this.do_;
    }

    public Optional<String> double_() {
      return this.double_;
    }

    public Optional<String> else_() {
      return this.else_;
    }

    public Optional<String> enum_() {
      return this.enum_;
    }

    public Optional<String> extends_() {
      return this.extends_;
    }

    public Optional<String> final_() {
      return this.final_;
    }

    public Optional<String> finally_() {
      return this.finally_;
    }

    public Optional<String> float_() {
      return this.float_;
    }

    public Optional<String> for_() {
      return this.for_;
    }

    public Optional<String> goto_() {
      return this.goto_;
    }

    public Optional<String> if_() {
      return this.if_;
    }

    public Optional<String> implements_() {
      return this.implements_;
    }

    public Optional<String> import_() {
      return this.import_;
    }

    public Optional<String> instanceof_() {
      return this.instanceof_;
    }

    public Optional<String> int_() {
      return this.int_;
    }

    public Optional<String> interface_() {
      return this.interface_;
    }

    public Optional<String> long_() {
      return this.long_;
    }

    public Optional<String> native_() {
      return this.native_;
    }

    public Optional<String> new_() {
      return this.new_;
    }

    public Optional<String> package_() {
      return this.package_;
    }

    public Optional<String> private_() {
      return this.private_;
    }

    public Optional<String> protected_() {
      return this.protected_;
    }

    public Optional<String> public_() {
      return this.public_;
    }

    public Optional<String> return_() {
      return this.return_;
    }

    public Optional<String> short_() {
      return this.short_;
    }

    public Optional<String> static_() {
      return this.static_;
    }

    public Optional<String> strictfp_() {
      return this.strictfp_;
    }

    public Optional<String> super_() {
      return this.super_;
    }

    public Optional<String> switch_() {
      return this.switch_;
    }

    public Optional<String> synchronized_() {
      return this.synchronized_;
    }

    public Optional<String> this_() {
      return this.this_;
    }

    public Optional<String> throw_() {
      return this.throw_;
    }

    public Optional<String> throws_() {
      return this.throws_;
    }

    public Optional<String> transient_() {
      return this.transient_;
    }

    public Optional<String> try_() {
      return this.try_;
    }

    public Optional<String> void_() {
      return this.void_;
    }

    public Optional<String> volatile_() {
      return this.volatile_;
    }

    public Optional<String> while_() {
      return this.while_;
    }

    public Optional<Operation> operation() {
      return this.operation;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], abstract_.isPresent() ? abstract_.get() : null);
          writer.writeString($responseFields[1], assert_.isPresent() ? assert_.get() : null);
          writer.writeString($responseFields[2], boolean_.isPresent() ? boolean_.get() : null);
          writer.writeString($responseFields[3], break_.isPresent() ? break_.get() : null);
          writer.writeString($responseFields[4], byte_.isPresent() ? byte_.get() : null);
          writer.writeString($responseFields[5], case_.isPresent() ? case_.get() : null);
          writer.writeString($responseFields[6], catch_.isPresent() ? catch_.get() : null);
          writer.writeString($responseFields[7], char_.isPresent() ? char_.get() : null);
          writer.writeString($responseFields[8], class_.isPresent() ? class_.get() : null);
          writer.writeString($responseFields[9], const_.isPresent() ? const_.get() : null);
          writer.writeString($responseFields[10], continue_.isPresent() ? continue_.get() : null);
          writer.writeString($responseFields[11], default_.isPresent() ? default_.get() : null);
          writer.writeString($responseFields[12], do_.isPresent() ? do_.get() : null);
          writer.writeString($responseFields[13], double_.isPresent() ? double_.get() : null);
          writer.writeString($responseFields[14], else_.isPresent() ? else_.get() : null);
          writer.writeString($responseFields[15], enum_.isPresent() ? enum_.get() : null);
          writer.writeString($responseFields[16], extends_.isPresent() ? extends_.get() : null);
          writer.writeString($responseFields[17], final_.isPresent() ? final_.get() : null);
          writer.writeString($responseFields[18], finally_.isPresent() ? finally_.get() : null);
          writer.writeString($responseFields[19], float_.isPresent() ? float_.get() : null);
          writer.writeString($responseFields[20], for_.isPresent() ? for_.get() : null);
          writer.writeString($responseFields[21], goto_.isPresent() ? goto_.get() : null);
          writer.writeString($responseFields[22], if_.isPresent() ? if_.get() : null);
          writer.writeString($responseFields[23], implements_.isPresent() ? implements_.get() : null);
          writer.writeString($responseFields[24], import_.isPresent() ? import_.get() : null);
          writer.writeString($responseFields[25], instanceof_.isPresent() ? instanceof_.get() : null);
          writer.writeString($responseFields[26], int_.isPresent() ? int_.get() : null);
          writer.writeString($responseFields[27], interface_.isPresent() ? interface_.get() : null);
          writer.writeString($responseFields[28], long_.isPresent() ? long_.get() : null);
          writer.writeString($responseFields[29], native_.isPresent() ? native_.get() : null);
          writer.writeString($responseFields[30], new_.isPresent() ? new_.get() : null);
          writer.writeString($responseFields[31], package_.isPresent() ? package_.get() : null);
          writer.writeString($responseFields[32], private_.isPresent() ? private_.get() : null);
          writer.writeString($responseFields[33], protected_.isPresent() ? protected_.get() : null);
          writer.writeString($responseFields[34], public_.isPresent() ? public_.get() : null);
          writer.writeString($responseFields[35], return_.isPresent() ? return_.get() : null);
          writer.writeString($responseFields[36], short_.isPresent() ? short_.get() : null);
          writer.writeString($responseFields[37], static_.isPresent() ? static_.get() : null);
          writer.writeString($responseFields[38], strictfp_.isPresent() ? strictfp_.get() : null);
          writer.writeString($responseFields[39], super_.isPresent() ? super_.get() : null);
          writer.writeString($responseFields[40], switch_.isPresent() ? switch_.get() : null);
          writer.writeString($responseFields[41], synchronized_.isPresent() ? synchronized_.get() : null);
          writer.writeString($responseFields[42], this_.isPresent() ? this_.get() : null);
          writer.writeString($responseFields[43], throw_.isPresent() ? throw_.get() : null);
          writer.writeString($responseFields[44], throws_.isPresent() ? throws_.get() : null);
          writer.writeString($responseFields[45], transient_.isPresent() ? transient_.get() : null);
          writer.writeString($responseFields[46], try_.isPresent() ? try_.get() : null);
          writer.writeString($responseFields[47], void_.isPresent() ? void_.get() : null);
          writer.writeString($responseFields[48], volatile_.isPresent() ? volatile_.get() : null);
          writer.writeString($responseFields[49], while_.isPresent() ? while_.get() : null);
          writer.writeObject($responseFields[50], operation.isPresent() ? operation.get().marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "abstract_=" + abstract_ + ", "
          + "assert_=" + assert_ + ", "
          + "boolean_=" + boolean_ + ", "
          + "break_=" + break_ + ", "
          + "byte_=" + byte_ + ", "
          + "case_=" + case_ + ", "
          + "catch_=" + catch_ + ", "
          + "char_=" + char_ + ", "
          + "class_=" + class_ + ", "
          + "const_=" + const_ + ", "
          + "continue_=" + continue_ + ", "
          + "default_=" + default_ + ", "
          + "do_=" + do_ + ", "
          + "double_=" + double_ + ", "
          + "else_=" + else_ + ", "
          + "enum_=" + enum_ + ", "
          + "extends_=" + extends_ + ", "
          + "final_=" + final_ + ", "
          + "finally_=" + finally_ + ", "
          + "float_=" + float_ + ", "
          + "for_=" + for_ + ", "
          + "goto_=" + goto_ + ", "
          + "if_=" + if_ + ", "
          + "implements_=" + implements_ + ", "
          + "import_=" + import_ + ", "
          + "instanceof_=" + instanceof_ + ", "
          + "int_=" + int_ + ", "
          + "interface_=" + interface_ + ", "
          + "long_=" + long_ + ", "
          + "native_=" + native_ + ", "
          + "new_=" + new_ + ", "
          + "package_=" + package_ + ", "
          + "private_=" + private_ + ", "
          + "protected_=" + protected_ + ", "
          + "public_=" + public_ + ", "
          + "return_=" + return_ + ", "
          + "short_=" + short_ + ", "
          + "static_=" + static_ + ", "
          + "strictfp_=" + strictfp_ + ", "
          + "super_=" + super_ + ", "
          + "switch_=" + switch_ + ", "
          + "synchronized_=" + synchronized_ + ", "
          + "this_=" + this_ + ", "
          + "throw_=" + throw_ + ", "
          + "throws_=" + throws_ + ", "
          + "transient_=" + transient_ + ", "
          + "try_=" + try_ + ", "
          + "void_=" + void_ + ", "
          + "volatile_=" + volatile_ + ", "
          + "while_=" + while_ + ", "
          + "operation=" + operation
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
        return this.abstract_.equals(that.abstract_)
         && this.assert_.equals(that.assert_)
         && this.boolean_.equals(that.boolean_)
         && this.break_.equals(that.break_)
         && this.byte_.equals(that.byte_)
         && this.case_.equals(that.case_)
         && this.catch_.equals(that.catch_)
         && this.char_.equals(that.char_)
         && this.class_.equals(that.class_)
         && this.const_.equals(that.const_)
         && this.continue_.equals(that.continue_)
         && this.default_.equals(that.default_)
         && this.do_.equals(that.do_)
         && this.double_.equals(that.double_)
         && this.else_.equals(that.else_)
         && this.enum_.equals(that.enum_)
         && this.extends_.equals(that.extends_)
         && this.final_.equals(that.final_)
         && this.finally_.equals(that.finally_)
         && this.float_.equals(that.float_)
         && this.for_.equals(that.for_)
         && this.goto_.equals(that.goto_)
         && this.if_.equals(that.if_)
         && this.implements_.equals(that.implements_)
         && this.import_.equals(that.import_)
         && this.instanceof_.equals(that.instanceof_)
         && this.int_.equals(that.int_)
         && this.interface_.equals(that.interface_)
         && this.long_.equals(that.long_)
         && this.native_.equals(that.native_)
         && this.new_.equals(that.new_)
         && this.package_.equals(that.package_)
         && this.private_.equals(that.private_)
         && this.protected_.equals(that.protected_)
         && this.public_.equals(that.public_)
         && this.return_.equals(that.return_)
         && this.short_.equals(that.short_)
         && this.static_.equals(that.static_)
         && this.strictfp_.equals(that.strictfp_)
         && this.super_.equals(that.super_)
         && this.switch_.equals(that.switch_)
         && this.synchronized_.equals(that.synchronized_)
         && this.this_.equals(that.this_)
         && this.throw_.equals(that.throw_)
         && this.throws_.equals(that.throws_)
         && this.transient_.equals(that.transient_)
         && this.try_.equals(that.try_)
         && this.void_.equals(that.void_)
         && this.volatile_.equals(that.volatile_)
         && this.while_.equals(that.while_)
         && this.operation.equals(that.operation);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= abstract_.hashCode();
        h *= 1000003;
        h ^= assert_.hashCode();
        h *= 1000003;
        h ^= boolean_.hashCode();
        h *= 1000003;
        h ^= break_.hashCode();
        h *= 1000003;
        h ^= byte_.hashCode();
        h *= 1000003;
        h ^= case_.hashCode();
        h *= 1000003;
        h ^= catch_.hashCode();
        h *= 1000003;
        h ^= char_.hashCode();
        h *= 1000003;
        h ^= class_.hashCode();
        h *= 1000003;
        h ^= const_.hashCode();
        h *= 1000003;
        h ^= continue_.hashCode();
        h *= 1000003;
        h ^= default_.hashCode();
        h *= 1000003;
        h ^= do_.hashCode();
        h *= 1000003;
        h ^= double_.hashCode();
        h *= 1000003;
        h ^= else_.hashCode();
        h *= 1000003;
        h ^= enum_.hashCode();
        h *= 1000003;
        h ^= extends_.hashCode();
        h *= 1000003;
        h ^= final_.hashCode();
        h *= 1000003;
        h ^= finally_.hashCode();
        h *= 1000003;
        h ^= float_.hashCode();
        h *= 1000003;
        h ^= for_.hashCode();
        h *= 1000003;
        h ^= goto_.hashCode();
        h *= 1000003;
        h ^= if_.hashCode();
        h *= 1000003;
        h ^= implements_.hashCode();
        h *= 1000003;
        h ^= import_.hashCode();
        h *= 1000003;
        h ^= instanceof_.hashCode();
        h *= 1000003;
        h ^= int_.hashCode();
        h *= 1000003;
        h ^= interface_.hashCode();
        h *= 1000003;
        h ^= long_.hashCode();
        h *= 1000003;
        h ^= native_.hashCode();
        h *= 1000003;
        h ^= new_.hashCode();
        h *= 1000003;
        h ^= package_.hashCode();
        h *= 1000003;
        h ^= private_.hashCode();
        h *= 1000003;
        h ^= protected_.hashCode();
        h *= 1000003;
        h ^= public_.hashCode();
        h *= 1000003;
        h ^= return_.hashCode();
        h *= 1000003;
        h ^= short_.hashCode();
        h *= 1000003;
        h ^= static_.hashCode();
        h *= 1000003;
        h ^= strictfp_.hashCode();
        h *= 1000003;
        h ^= super_.hashCode();
        h *= 1000003;
        h ^= switch_.hashCode();
        h *= 1000003;
        h ^= synchronized_.hashCode();
        h *= 1000003;
        h ^= this_.hashCode();
        h *= 1000003;
        h ^= throw_.hashCode();
        h *= 1000003;
        h ^= throws_.hashCode();
        h *= 1000003;
        h ^= transient_.hashCode();
        h *= 1000003;
        h ^= try_.hashCode();
        h *= 1000003;
        h ^= void_.hashCode();
        h *= 1000003;
        h ^= volatile_.hashCode();
        h *= 1000003;
        h ^= while_.hashCode();
        h *= 1000003;
        h ^= operation.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Operation.Mapper operationFieldMapper = new Operation.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final String abstract_ = reader.readString($responseFields[0]);
        final String assert_ = reader.readString($responseFields[1]);
        final String boolean_ = reader.readString($responseFields[2]);
        final String break_ = reader.readString($responseFields[3]);
        final String byte_ = reader.readString($responseFields[4]);
        final String case_ = reader.readString($responseFields[5]);
        final String catch_ = reader.readString($responseFields[6]);
        final String char_ = reader.readString($responseFields[7]);
        final String class_ = reader.readString($responseFields[8]);
        final String const_ = reader.readString($responseFields[9]);
        final String continue_ = reader.readString($responseFields[10]);
        final String default_ = reader.readString($responseFields[11]);
        final String do_ = reader.readString($responseFields[12]);
        final String double_ = reader.readString($responseFields[13]);
        final String else_ = reader.readString($responseFields[14]);
        final String enum_ = reader.readString($responseFields[15]);
        final String extends_ = reader.readString($responseFields[16]);
        final String final_ = reader.readString($responseFields[17]);
        final String finally_ = reader.readString($responseFields[18]);
        final String float_ = reader.readString($responseFields[19]);
        final String for_ = reader.readString($responseFields[20]);
        final String goto_ = reader.readString($responseFields[21]);
        final String if_ = reader.readString($responseFields[22]);
        final String implements_ = reader.readString($responseFields[23]);
        final String import_ = reader.readString($responseFields[24]);
        final String instanceof_ = reader.readString($responseFields[25]);
        final String int_ = reader.readString($responseFields[26]);
        final String interface_ = reader.readString($responseFields[27]);
        final String long_ = reader.readString($responseFields[28]);
        final String native_ = reader.readString($responseFields[29]);
        final String new_ = reader.readString($responseFields[30]);
        final String package_ = reader.readString($responseFields[31]);
        final String private_ = reader.readString($responseFields[32]);
        final String protected_ = reader.readString($responseFields[33]);
        final String public_ = reader.readString($responseFields[34]);
        final String return_ = reader.readString($responseFields[35]);
        final String short_ = reader.readString($responseFields[36]);
        final String static_ = reader.readString($responseFields[37]);
        final String strictfp_ = reader.readString($responseFields[38]);
        final String super_ = reader.readString($responseFields[39]);
        final String switch_ = reader.readString($responseFields[40]);
        final String synchronized_ = reader.readString($responseFields[41]);
        final String this_ = reader.readString($responseFields[42]);
        final String throw_ = reader.readString($responseFields[43]);
        final String throws_ = reader.readString($responseFields[44]);
        final String transient_ = reader.readString($responseFields[45]);
        final String try_ = reader.readString($responseFields[46]);
        final String void_ = reader.readString($responseFields[47]);
        final String volatile_ = reader.readString($responseFields[48]);
        final String while_ = reader.readString($responseFields[49]);
        final Operation operation = reader.readObject($responseFields[50], new ResponseReader.ObjectReader<Operation>() {
          @Override
          public Operation read(ResponseReader reader) {
            return operationFieldMapper.map(reader);
          }
        });
        return new Data(abstract_, assert_, boolean_, break_, byte_, case_, catch_, char_, class_, const_, continue_, default_, do_, double_, else_, enum_, extends_, final_, finally_, float_, for_, goto_, if_, implements_, import_, instanceof_, int_, interface_, long_, native_, new_, package_, private_, protected_, public_, return_, short_, static_, strictfp_, super_, switch_, synchronized_, this_, throw_, throws_, transient_, try_, void_, volatile_, while_, operation);
      }
    }
  }

  public static class Operation {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @NotNull String name;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Operation(@NotNull String __typename, @NotNull String name) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.name = Utils.checkNotNull(name, "name == null");
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    public @NotNull String name() {
      return this.name;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Operation{"
          + "__typename=" + __typename + ", "
          + "name=" + name
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Operation) {
        Operation that = (Operation) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name);
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
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Operation> {
      @Override
      public Operation map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        return new Operation(__typename, name);
      }
    }
  }
}
