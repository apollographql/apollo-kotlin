package test;

import com.apollographql.apollo.api.BaseFakeResolver;
import com.apollographql.apollo.api.CompiledField;
import com.apollographql.apollo.api.CustomScalarAdapters;
import com.apollographql.apollo.api.ExecutableDefinition;
import com.apollographql.apollo.api.FakeResolver;
import com.apollographql.apollo.api.FakeResolverContext;
import com.apollographql.apollo.api.FakeResolverKt;
import com.apollographql.apollo.api.Query;
import data.builders.GetAliasesQuery;
import data.builders.GetAnimalQuery;
import data.builders.GetCustomScalarQuery;
import data.builders.GetDirectionQuery;
import data.builders.GetEverythingQuery;
import data.builders.GetFelineQuery;
import data.builders.GetIntQuery;
import data.builders.GetPartialQuery;
import data.builders.MyLong;
import data.builders.PutIntMutation;
import data.builders.builder.MutationRootBuilder;
import data.builders.builder.QueryBuilder;
import data.builders.builder.QueryMap;
import data.builders.builder.resolver.DefaultFakeResolver;
import data.builders.type.Direction;
import data.builders.builder.DataBuilders;
import data.builders.type.MutationRoot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DataBuilderTest {
  private CustomScalarAdapters customScalarAdapters = new CustomScalarAdapters.Builder()
      .add("Long2", new MyLong.MyLongAdapter())
      .build();
  private DataBuilders dataBuilders = new DataBuilders(customScalarAdapters);

  @Test
  public void nullabilityTest() {
    GetIntQuery.Data data = QueryBuilder.buildData(
        GetIntQuery.definition,
        CustomScalarAdapters.Empty,
        dataBuilders.query()
            .nullableInt(null)
            .nonNullableInt(42)
            .build()
    );

    assertEquals(null, data.nullableInt);
    assertEquals(Integer.valueOf(42), data.nonNullableInt);
  }

  @Test
  public void aliasTest() {
    GetAliasesQuery.Data data = QueryBuilder.buildData(
        GetAliasesQuery.definition,
        CustomScalarAdapters.Empty,
        dataBuilders.query()
            .cat(dataBuilders.cat().species("Cat").build())
            .alias("aliasedNullableInt", 50)
            .alias(
                "aliasedCat",
                dataBuilders.cat().species("AliasedCat").build()
            )
            .build()
    );

    assertEquals(Integer.valueOf(50), data.aliasedNullableInt);
    assertEquals("Cat", data.cat.species);
    assertEquals("AliasedCat", data.aliasedCat.species);
  }

  @Test
  public void mutationTest() {
    PutIntMutation.Data data = MutationRootBuilder.buildData(
        PutIntMutation.definition,
        CustomScalarAdapters.Empty,
        dataBuilders.mutationRoot()
            .nullableInt(null)
            .build()
    );

    assertEquals(null, data.nullableInt);
  }

  @Test
  public void interfaceTest() {
    GetAnimalQuery.Data data = QueryBuilder.buildData(
        GetAnimalQuery.definition,
        CustomScalarAdapters.Empty,
        dataBuilders.query()
            .animal(
                dataBuilders.lion()
                    .species("LionSpecies")
                    .roar("Rooooaaarr")
                    .build()
            )
            .build()
    );

    assertEquals("Lion", data.animal.__typename);
    assertEquals("LionSpecies", data.animal.species);
    assertEquals("Rooooaaarr", data.animal.onLion.roar);
  }

  @Test
  public void otherInterfaceImplementationTest() {
    GetAnimalQuery.Data data = QueryBuilder.buildData(
        GetAnimalQuery.definition,
        CustomScalarAdapters.Empty,
        dataBuilders.query()
            .animal(
                dataBuilders.otherAnimal()
                    .__typename("Gazelle")
                    .species("GazelleSpecies")
                    .build()
            )
            .build()
    );

    assertEquals("Gazelle", data.animal.__typename);
    assertEquals("GazelleSpecies", data.animal.species);
    assertNull(data.animal.onLion);
  }

  @Test
  public void unionTest1() {
    GetFelineQuery.Data data = QueryBuilder.buildData(
        GetFelineQuery.definition,
        CustomScalarAdapters.Empty,
        dataBuilders.query()
            .feline(
                dataBuilders.lion()
                    .species("LionSpecies")
                    .roar("Rooooaaarr")
                    .build()
            )
            .build()
    );

    assertEquals("Lion", data.feline.__typename);
    assertEquals(null, data.feline.onCat);
  }

  @Test
  public void unionTest2() {
    GetFelineQuery.Data data = QueryBuilder.buildData(
        GetFelineQuery.definition,
        CustomScalarAdapters.Empty,
        dataBuilders.query()
            .feline(
                dataBuilders.cat()
                    .species("CatSpecies")
                    .mustaches(5)
                    .build()
            )
            .build()
    );

    assertEquals("Cat", data.feline.__typename);
    assertEquals(Integer.valueOf(5), data.feline.onCat.mustaches);
  }

  @Test
  public void otherUnionMemberTest() {
    GetFelineQuery.Data data = QueryBuilder.buildData(
        GetFelineQuery.definition,
        CustomScalarAdapters.Empty,
        dataBuilders.query()
            .feline(
                dataBuilders.otherFeline()
                    .__typename("Tiger")
                    .build()
            )
            .build()
    );

    assertEquals("Tiger", data.feline.__typename);
    assertEquals(null, data.feline.onCat);
  }

  @Test
  public void enumTest() {
    GetDirectionQuery.Data data = QueryBuilder.buildData(
        GetDirectionQuery.definition,
        CustomScalarAdapters.Empty,
        dataBuilders.query()
            .direction(Direction.NORTH)
            .build()
    );
    assertEquals(Direction.NORTH, data.direction);
  }

  @Test
  public void customScalarTest() {
    GetCustomScalarQuery.Data data = QueryBuilder.buildData(
        GetCustomScalarQuery.definition,
        customScalarAdapters,
        dataBuilders.query()
            .long1(new MyLong(42L))
            .long2(new MyLong(43L))
            .long3(44)
            .listOfListOfLong1(Collections.singletonList(Collections.singletonList(new MyLong(42L))))
            .build()
    );


    assertEquals(Long.valueOf(42), data.long1.value);
    assertEquals(Long.valueOf(43), data.long2.value);
    assertEquals(44, data.long3);
  }

  @Test
  public void fakeValues() {
    GetEverythingQuery.Data data = QueryBuilder.buildData(
        new DefaultFakeResolver(),
        GetEverythingQuery.definition,
        customScalarAdapters,
        dataBuilders.query().build()
    );

    assertEquals(Direction.NORTH, data.direction);
    assertEquals(Integer.valueOf(-34), data.nullableInt);
    assertEquals(Integer.valueOf(-99), data.nonNullableInt);
    assertEquals(Arrays.asList(
        Arrays.asList(73, 74, 75),
        Arrays.asList(4, 5, 6),
        Arrays.asList(35, 36, 37)
    ), data.listOfListOfInt);
    assertEquals(Integer.valueOf(53), data.cat.mustaches);
    assertEquals("Cat", data.animal.__typename);
    assertEquals("Lion", data.feline.__typename);
  }

  @Test
  public void partialFakeValues() {
    GetPartialQuery.Data data = QueryBuilder.buildData(
        new DefaultFakeResolver(),
        GetPartialQuery.definition,
        customScalarAdapters,
        dataBuilders.query()
            .listOfListOfAnimal(
                Collections.singletonList(
                    Collections.singletonList(
                        dataBuilders.lion()
                            .species("FooSpecies")
                            .build()
                    )
                )
            )
            .build()
      );


    GetPartialQuery.Data data2 = new GetPartialQuery.Data(
        Collections.singletonList(
            Collections.singletonList (
                new GetPartialQuery.ListOfListOfAnimal(
                    "Lion",
                    "FooSpecies",
                    new GetPartialQuery.OnLion("roar")
                )
            )
        )
    );
    assertEquals(data2, data);
  }

  static class MyFakeResolver implements FakeResolver {
    @NotNull @Override public Object resolveLeaf(@NotNull FakeResolverContext context) {
      String name = context.getMergedField().getType().rawType().getName();
      Object ret = null;
      switch (name) {
        case "Long1": {
          ret = "45";
          break;
        }
        case "Long2": {
          ret = "46";
          break;
        }
        case "Long3": {
          ret = 47L;
          break;
        }
        default: throw new IllegalStateException();
      }
      return ret;
    }

    @Override public int resolveListSize(@NotNull FakeResolverContext context) {
      return 1;
    }

    @Override public boolean resolveMaybeNull(@NotNull FakeResolverContext context) {
      return false;
    }

    @NotNull @Override public String resolveTypename(@NotNull FakeResolverContext context) {
      throw new IllegalStateException();
    }

    @Nullable @Override public String stableIdForObject(@NotNull Map<String, ?> obj, @NotNull CompiledField mergedField) {
      return null;
    }
  }

  @Test
  public void customScalarFakeValues() {
    GetCustomScalarQuery.Data data = QueryBuilder.buildData(
        new MyFakeResolver(),
        GetCustomScalarQuery.definition,
        customScalarAdapters,
        dataBuilders.query().build()
    );

    assertEquals(Long.valueOf(45L), data.long1.value);
    assertEquals(Long.valueOf(46L), data.long2.value);
    assertEquals(47, data.long3); // AnyDataAdapter will try to fit the smallest possible number
  }
}
