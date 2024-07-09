package test;

import com.apollographql.apollo.api.Optional;
import jvmoverloads.GetPersonQuery;
import org.junit.Test;

public class JvmOverloadsTest {
  @Test
  public void jvmOverloads() {
    new GetPersonQuery();
    new GetPersonQuery(new Optional.Present<>("1"));
  }
}
