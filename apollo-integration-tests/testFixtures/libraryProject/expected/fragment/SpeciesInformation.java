package fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public class SpeciesInformation {
  public static final String FRAGMENT_DEFINITION = "fragment SpeciesInformation on Species {\n"
      + "  __typename\n"
      + "  classification\n"
      + "}";

  public static final List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "Species"));

  private final @Nullable String classification;

  public SpeciesInformation(@Nullable String classification) {
    this.classification = classification;
  }

  public @Nullable String classification() {
    return this.classification;
  }

  @Override
  public String toString() {
    return "SpeciesInformation{"
      + "classification=" + classification
      + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof SpeciesInformation) {
      SpeciesInformation that = (SpeciesInformation) o;
      return ((this.classification == null) ? (that.classification == null) : this.classification.equals(that.classification));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= (classification == null) ? 0 : classification.hashCode();
    return h;
  }

  public static final class Mapper implements ResponseFieldMapper<SpeciesInformation> {
    final Field[] fields = {
      Field.forString("classification", "classification", null, true)
    };

    @Override
    public SpeciesInformation map(ResponseReader reader) throws IOException {
      final String classification = reader.read(fields[0]);
      return new SpeciesInformation(classification);
    }
  }
}
