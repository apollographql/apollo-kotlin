package test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import model.builders.GetIntQuery;


public class ModelBuilderTest {

  @Test
  public void simpleTest() {
    GetIntQuery.Data data = GetIntQuery.Data.builder()
        .nullableInt(null)
        .nonNullableInt(42)
        .build();

    assertEquals(null, data.nullableInt);
    assertEquals(Integer.valueOf(42), data.nonNullableInt);
  }
}
