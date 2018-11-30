package com.apollographql.apollo.api;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public final class InputTest {

    @Test
    public void testInputEqualsOnNotNullValue() {
        String value = "Hello world!";
        Input<String> stringInput = Input.fromNullable(value);
        Input<String> anotherStringInput = Input.fromNullable(value);

        assertEquals(stringInput, anotherStringInput);
    }

    @Test
    public void testInputNotEqualsOnDifferentValues() {
        String value = "Hello world!";
        String value2 = "Bye world!";
        Input<String> stringInput = Input.fromNullable(value);
        Input<String> anotherStringInput = Input.fromNullable(value2);

        assertNotEquals(stringInput, anotherStringInput);
    }

    @Test
    public void testInputEqualsOnNullValue() {
        Input<String> stringInput = Input.fromNullable(null);
        Input<String> anotherStringInput = Input.fromNullable(null);

        assertEquals(stringInput, anotherStringInput);
    }
}
