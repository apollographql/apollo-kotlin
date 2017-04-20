package com.apollographql.apollo.api.graphql.internal;

/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import com.apollographql.apollo.api.internal.Function;
import com.apollographql.apollo.api.internal.Functions;
import com.apollographql.apollo.api.internal.Optional;

import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;


public final class OptionalTest {
  @Test
  public void testAbsent() {
    Optional<String> optionalName = Optional.absent();
    assertFalse(optionalName.isPresent());
  }

  public void testOf() {
    assertEquals("training", Optional.of("training").get());
  }

  public void testOfNull() {
    try {
      Optional.of(null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void testFromNullable() {
    Optional<String> optionalName = Optional.fromNullable("bob");
    assertEquals("bob", optionalName.get());
  }

  @Test
  public void testFromNullableNull() {
    // not promised by spec, but easier to test
    assertSame(Optional.absent(), Optional.fromNullable(null));
  }

  @Test
  public void testIsPresentNo() {
    assertFalse(Optional.absent().isPresent());
  }

  @Test
  public void testIsPresentYes() {
    assertTrue(Optional.of("training").isPresent());
  }

  @Test
  public void testGetAbsent() {
    Optional<String> optional = Optional.absent();
    try {
      optional.get();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @Test
  public void testGetPresent() {
    assertEquals("training", Optional.of("training").get());
  }

  @Test
  public void testOrTPresent() {
    assertEquals("a", Optional.of("a").or("default"));
  }

  @Test
  public void testOrTAbsent() {
    assertEquals("default", Optional.absent().or("default"));
  }

  @Test
  public void testOrOptionalPresent() {
    assertEquals(Optional.of("a"), Optional.of("a").or(Optional.of("fallback")));
  }

  @Test
  public void testOrOptionalAbsent() {
    assertEquals(Optional.of("fallback"), Optional.absent().or(Optional.of("fallback")));
  }

  @Test
  public void testOrNullPresent() {
    assertEquals("a", Optional.of("a").orNull());
  }

  @Test
  public void testOrNullAbsent() {
    assertNull(Optional.absent().orNull());
  }

  @Test
  public void testAsSetPresent() {
    Set<String> expected = Collections.singleton("a");
    assertEquals(expected, Optional.of("a").asSet());
  }

  @Test
  public void testAsSetAbsent() {
    assertTrue("Returned set should be empty", Optional.absent().asSet().isEmpty());
  }

  @Test
  public void testAsSetPresentIsImmutable() {
    Set<String> presentAsSet = Optional.of("a").asSet();
    try {
      presentAsSet.add("b");
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  @Test
  public void testAsSetAbsentIsImmutable() {
    Set<Object> absentAsSet = Optional.absent().asSet();
    try {
      absentAsSet.add("foo");
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  @Test
  public void testTransformAbsent() {
    assertEquals(Optional.absent(), Optional.absent().transform(Functions.identity()));
    assertEquals(Optional.absent(), Optional.absent().transform(Functions.toStringFunction()));
  }

  @Test
  public void testTransformPresentIdentity() {
    assertEquals(Optional.of("a"), Optional.of("a").transform(Functions.identity()));
  }

  @Test
  public void testTransformPresentToString() {
    assertEquals(Optional.of("42"), Optional.of(42).transform(Functions.toStringFunction()));
  }

  @Test
  public void testTransformPresentFunctionReturnsNull() {
    try {
      Optional<String> unused =
          Optional.of("a")
              .transform(
                  new Function<String, String>() {
                    @Override
                    public String apply(String input) {
                      return null;
                    }
                  });
      fail("Should throw if Function returns null.");
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void testTransformAbssentFunctionReturnsNull() {
    assertEquals(Optional.absent(),
        Optional.absent().transform(
            new Function<Object, Object>() {
              @Override public Object apply(Object input) {
                return null;
              }
            }));
  }

  @Test
  public void testEqualsAndHashCodeAbsent() {
    assertEquals(Optional.<String>absent(), Optional.<Integer>absent());
    assertEquals(Optional.absent().hashCode(), Optional.absent().hashCode());
    assertThat(Optional.absent().hashCode())
        .isNotEqualTo(Optional.of(0).hashCode());
  }

  @Test
  public void testEqualsAndHashCodePresent() {
    assertEquals(Optional.of("training"), Optional.of("training"));
    assertFalse(Optional.of("a").equals(Optional.of("b")));
    assertFalse(Optional.of("a").equals(Optional.absent()));
    assertEquals(Optional.of("training").hashCode(), Optional.of("training").hashCode());
  }

  @Test
  public void testToStringAbsent() {
    assertEquals("Optional.absent()", Optional.absent().toString());
  }

  @Test
  public void testToStringPresent() {
    assertEquals("Optional.of(training)", Optional.of("training").toString());
  }


  private static Optional<Integer> getSomeOptionalInt() {
    return Optional.of(1);
  }

  private static FluentIterable<? extends Number> getSomeNumbers() {
    return FluentIterable.from(ImmutableList.<Number>of());
  }

  /*
   * The following tests demonstrate the shortcomings of or() and test that the casting workaround
   * mentioned in the method Javadoc does in fact compile.
   */

  @SuppressWarnings("unused") // compilation test
  @Test
  public void testSampleCodeError1() {
    Optional<Integer> optionalInt = getSomeOptionalInt();
    // Number value = optionalInt.or(0.5); // error
  }


  @SuppressWarnings("unused") // compilation test
  @Test
  public void testSampleCodeFine1() {
    Optional<Number> optionalInt = Optional.of((Number) 1);
    Number value = optionalInt.or(0.5); // fine
  }

  @SuppressWarnings("unused") // compilation test
  @Test
  public void testSampleCodeFine2() {
    FluentIterable<? extends Number> numbers = getSomeNumbers();

    // Sadly, the following is what users will have to do in some circumstances.

  }

  @Test
  public void testMapAbsent() {
    assertEquals(Optional.absent(), Optional.absent().map(Functions.identity()));
    assertEquals(Optional.absent(), Optional.absent().map(Functions.toStringFunction()));
  }

  @Test
  public void testMapPresentIdentity() {
    assertEquals(Optional.of("a"), Optional.of("a").map(Functions.identity()));
  }

  @Test
  public void testMapPresentToString() {
    assertEquals(Optional.of("42"), Optional.of(42).map(Functions.toStringFunction()));
  }

  @Test
  public void testMapPresentFunctionReturnsNull() {
    try {
      Optional<String> unused =
          Optional.of("a")
              .map(
                  new Function<String, String>() {
                    @Override
                    public String apply(String input) {
                      return null;
                    }
                  });
      fail("Should throw if Function returns null.");
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void testMapAbssentFunctionReturnsNull() {
    assertEquals(Optional.absent(),
        Optional.absent().map(
            new Function<Object, Object>() {
              @Override public Object apply(Object input) {
                return null;
              }
            }));
  }

  @Test
  public void testFlatMapAbsent() {
    assertEquals(Optional.absent(), Optional.absent().flatMap(new Function<Object, Optional<String>>() {
      @Nonnull @Override public Optional<String> apply(@Nonnull Object o) {
        return Optional.of(o.toString());
      }
    }));
  }

  @Test
  public void testFlatMapMapPresentIdentity() {
    assertEquals(Optional.of("a"), Optional.of("a").flatMap(new Function<String, Optional<String>>() {
          @Nonnull @Override public Optional<String> apply(@Nonnull String s) {
            return Optional.of(s);
          }
        })
    );
  }

  @Test
  public void testFlatMapPresentToString() {
    assertEquals(Optional.of("42"), Optional.of(42).flatMap(new Function<Integer, Optional<String>>() {
      @Nonnull @Override public Optional<String> apply(@Nonnull Integer integer) {
        return Optional.of(integer.toString());
      }
    }));
  }

  @Test
  public void testFlatMapPresentFunctionReturnsNull() {
    try {
      Optional<String> unused = Optional.of("a").flatMap(new Function<String, Optional<String>>() {
        @Nonnull @Override public Optional<String> apply(@Nonnull String s) {
          return null;
        }
      });
      fail("Should throw if Function returns null.");
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void testFlatMapAbssentFunctionReturnsNull() {
    assertEquals(Optional.absent(), Optional.absent().flatMap(new Function<Object, Optional<Object>>() {
      @Nonnull @Override public Optional<Object> apply(@Nonnull Object o) {
        return null;
      }
    }));
  }
}
