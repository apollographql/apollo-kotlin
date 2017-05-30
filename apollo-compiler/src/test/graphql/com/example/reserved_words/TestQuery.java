package com.example.reserved_words;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.internal.Optional;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "";

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
    private final Optional<String> abstract_;

    private final Optional<String> assert_;

    private final Optional<String> boolean_;

    private final Optional<String> break_;

    private final Optional<String> byte_;

    private final Optional<String> case_;

    private final Optional<String> catch_;

    private final Optional<String> char_;

    private final Optional<String> class_;

    private final Optional<String> const_;

    private final Optional<String> continue_;

    private final Optional<String> default_;

    private final Optional<String> do_;

    private final Optional<String> double_;

    private final Optional<String> else_;

    private final Optional<String> enum_;

    private final Optional<String> extends_;

    private final Optional<String> final_;

    private final Optional<String> finally_;

    private final Optional<String> float_;

    private final Optional<String> for_;

    private final Optional<String> goto_;

    private final Optional<String> if_;

    private final Optional<String> implements_;

    private final Optional<String> import_;

    private final Optional<String> instanceof_;

    private final Optional<String> int_;

    private final Optional<String> interface_;

    private final Optional<String> long_;

    private final Optional<String> native_;

    private final Optional<String> new_;

    private final Optional<String> package_;

    private final Optional<String> private_;

    private final Optional<String> protected_;

    private final Optional<String> public_;

    private final Optional<String> return_;

    private final Optional<String> short_;

    private final Optional<String> static_;

    private final Optional<String> strictfp_;

    private final Optional<String> super_;

    private final Optional<String> switch_;

    private final Optional<String> synchronized_;

    private final Optional<String> this_;

    private final Optional<String> throw_;

    private final Optional<String> throws_;

    private final Optional<String> transient_;

    private final Optional<String> try_;

    private final Optional<String> void_;

    private final Optional<String> volatile_;

    private final Optional<String> while_;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

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
        @Nullable String volatile_, @Nullable String while_) {
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
          + "while_=" + while_
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
         && this.while_.equals(that.while_);
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
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Field[] fields = {
        Field.forString("abstract", "abstract", null, true),
        Field.forString("assert", "assert", null, true),
        Field.forString("boolean", "boolean", null, true),
        Field.forString("break", "break", null, true),
        Field.forString("byte", "byte", null, true),
        Field.forString("case", "case", null, true),
        Field.forString("catch", "catch", null, true),
        Field.forString("char", "char", null, true),
        Field.forString("class", "class", null, true),
        Field.forString("const", "const", null, true),
        Field.forString("continue", "continue", null, true),
        Field.forString("default", "default", null, true),
        Field.forString("do", "do", null, true),
        Field.forString("double", "double", null, true),
        Field.forString("else", "else", null, true),
        Field.forString("enum", "enum", null, true),
        Field.forString("extends", "extends", null, true),
        Field.forString("final", "final", null, true),
        Field.forString("finally", "finally", null, true),
        Field.forString("float", "float", null, true),
        Field.forString("for", "for", null, true),
        Field.forString("goto", "goto", null, true),
        Field.forString("if", "if", null, true),
        Field.forString("implements", "implements", null, true),
        Field.forString("import", "import", null, true),
        Field.forString("instanceof", "instanceof", null, true),
        Field.forString("int", "int", null, true),
        Field.forString("interface", "interface", null, true),
        Field.forString("long", "long", null, true),
        Field.forString("native", "native", null, true),
        Field.forString("new", "new", null, true),
        Field.forString("package", "package", null, true),
        Field.forString("private", "private", null, true),
        Field.forString("protected", "protected", null, true),
        Field.forString("public", "public", null, true),
        Field.forString("return", "return", null, true),
        Field.forString("short", "short", null, true),
        Field.forString("static", "static", null, true),
        Field.forString("strictfp", "strictfp", null, true),
        Field.forString("super", "super", null, true),
        Field.forString("switch", "switch", null, true),
        Field.forString("synchronized", "synchronized", null, true),
        Field.forString("this", "this", null, true),
        Field.forString("throw", "throw", null, true),
        Field.forString("throws", "throws", null, true),
        Field.forString("transient", "transient", null, true),
        Field.forString("try", "try", null, true),
        Field.forString("void", "void", null, true),
        Field.forString("volatile", "volatile", null, true),
        Field.forString("while", "while", null, true)
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final String abstract_ = reader.read(fields[0]);
        final String assert_ = reader.read(fields[1]);
        final String boolean_ = reader.read(fields[2]);
        final String break_ = reader.read(fields[3]);
        final String byte_ = reader.read(fields[4]);
        final String case_ = reader.read(fields[5]);
        final String catch_ = reader.read(fields[6]);
        final String char_ = reader.read(fields[7]);
        final String class_ = reader.read(fields[8]);
        final String const_ = reader.read(fields[9]);
        final String continue_ = reader.read(fields[10]);
        final String default_ = reader.read(fields[11]);
        final String do_ = reader.read(fields[12]);
        final String double_ = reader.read(fields[13]);
        final String else_ = reader.read(fields[14]);
        final String enum_ = reader.read(fields[15]);
        final String extends_ = reader.read(fields[16]);
        final String final_ = reader.read(fields[17]);
        final String finally_ = reader.read(fields[18]);
        final String float_ = reader.read(fields[19]);
        final String for_ = reader.read(fields[20]);
        final String goto_ = reader.read(fields[21]);
        final String if_ = reader.read(fields[22]);
        final String implements_ = reader.read(fields[23]);
        final String import_ = reader.read(fields[24]);
        final String instanceof_ = reader.read(fields[25]);
        final String int_ = reader.read(fields[26]);
        final String interface_ = reader.read(fields[27]);
        final String long_ = reader.read(fields[28]);
        final String native_ = reader.read(fields[29]);
        final String new_ = reader.read(fields[30]);
        final String package_ = reader.read(fields[31]);
        final String private_ = reader.read(fields[32]);
        final String protected_ = reader.read(fields[33]);
        final String public_ = reader.read(fields[34]);
        final String return_ = reader.read(fields[35]);
        final String short_ = reader.read(fields[36]);
        final String static_ = reader.read(fields[37]);
        final String strictfp_ = reader.read(fields[38]);
        final String super_ = reader.read(fields[39]);
        final String switch_ = reader.read(fields[40]);
        final String synchronized_ = reader.read(fields[41]);
        final String this_ = reader.read(fields[42]);
        final String throw_ = reader.read(fields[43]);
        final String throws_ = reader.read(fields[44]);
        final String transient_ = reader.read(fields[45]);
        final String try_ = reader.read(fields[46]);
        final String void_ = reader.read(fields[47]);
        final String volatile_ = reader.read(fields[48]);
        final String while_ = reader.read(fields[49]);
        return new Data(abstract_, assert_, boolean_, break_, byte_, case_, catch_, char_, class_, const_, continue_, default_, do_, double_, else_, enum_, extends_, final_, finally_, float_, for_, goto_, if_, implements_, import_, instanceof_, int_, interface_, long_, native_, new_, package_, private_, protected_, public_, return_, short_, static_, strictfp_, super_, switch_, synchronized_, this_, throw_, throws_, transient_, try_, void_, volatile_, while_);
      }
    }
  }
}
