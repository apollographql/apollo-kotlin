package com.apollographql.android.internal;

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

import junit.framework.TestCase;

import java.util.Collections;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

public final class OptionalTest extends TestCase {
  public void testAbsent() {
    Optional<String> optionalName = Optional.absent();
    assertFalse(optionalName.isPresent());
  }

  public void testOf() {
    assertEquals("training", Optional.of("training").get());
  }

  public void testOf_null() {
    try {
      Optional.of(null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testFromNullable() {
    Optional<String> optionalName = Optional.fromNullable("bob");
    assertEquals("bob", optionalName.get());
  }

  public void testFromNullable_null() {
    // not promised by spec, but easier to test
    assertSame(Optional.absent(), Optional.fromNullable(null));
  }

  public void testIsPresent_no() {
    assertFalse(Optional.absent().isPresent());
  }

  public void testIsPresent_yes() {
    assertTrue(Optional.of("training").isPresent());
  }

  public void testGet_absent() {
    Optional<String> optional = Optional.absent();
    try {
      optional.get();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testGet_present() {
    assertEquals("training", Optional.of("training").get());
  }

  public void testOr_T_present() {
    assertEquals("a", Optional.of("a").or("default"));
  }

  public void testOr_T_absent() {
    assertEquals("default", Optional.absent().or("default"));
  }

  public void testOr_Optional_present() {
    assertEquals(Optional.of("a"), Optional.of("a").or(Optional.of("fallback")));
  }

  public void testOr_Optional_absent() {
    assertEquals(Optional.of("fallback"), Optional.absent().or(Optional.of("fallback")));
  }

  public void testOrNull_present() {
    assertEquals("a", Optional.of("a").orNull());
  }

  public void testOrNull_absent() {
    assertNull(Optional.absent().orNull());
  }

  public void testAsSet_present() {
    Set<String> expected = Collections.singleton("a");
    assertEquals(expected, Optional.of("a").asSet());
  }

  public void testAsSet_absent() {
    assertTrue("Returned set should be empty", Optional.absent().asSet().isEmpty());
  }

  public void testAsSet_presentIsImmutable() {
    Set<String> presentAsSet = Optional.of("a").asSet();
    try {
      presentAsSet.add("b");
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testAsSet_absentIsImmutable() {
    Set<Object> absentAsSet = Optional.absent().asSet();
    try {
      absentAsSet.add("foo");
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }





  public void testEqualsAndHashCode_absent() {
    assertEquals(Optional.<String>absent(), Optional.<Integer>absent());
    assertEquals(Optional.absent().hashCode(), Optional.absent().hashCode());
    assertThat(Optional.absent().hashCode())
        .isNotEqualTo(Optional.of(0).hashCode());
  }

  public void testEqualsAndHashCode_present() {
    assertEquals(Optional.of("training"), Optional.of("training"));
    assertFalse(Optional.of("a").equals(Optional.of("b")));
    assertFalse(Optional.of("a").equals(Optional.absent()));
    assertEquals(Optional.of("training").hashCode(), Optional.of("training").hashCode());
  }

  public void testToString_absent() {
    assertEquals("Optional.absent()", Optional.absent().toString());
  }

  public void testToString_present() {
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
  public void testSampleCodeError1() {
    Optional<Integer> optionalInt = getSomeOptionalInt();
    // Number value = optionalInt.or(0.5); // error
  }



  @SuppressWarnings("unused") // compilation test
  public void testSampleCodeFine1() {
    Optional<Number> optionalInt = Optional.of((Number) 1);
    Number value = optionalInt.or(0.5); // fine
  }

  @SuppressWarnings("unused") // compilation test
  public void testSampleCodeFine2() {
    FluentIterable<? extends Number> numbers = getSomeNumbers();

    // Sadly, the following is what users will have to do in some circumstances.

  }
}
