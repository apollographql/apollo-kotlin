package com.apollographql.apollo.response;

import com.apollographql.apollo.api.CustomTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import com.apollographql.apollo.api.internal.ResponseFieldMapper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

public class OperationResponseParserTest {

    abstract static class TestOperation implements Operation<Operation.Data, String, Operation.Variables> {
    }

    @Test
    public void validateNullDataFieldInPayload() {
        OperationResponseParser<Operation.Data, String> parser = new OperationResponseParser<>(
                mock(TestOperation.class),
                mock(ResponseFieldMapper.class),
                new ScalarTypeAdapters(new HashMap<ScalarType, CustomTypeAdapter<?>>())
        );

        Map<String, Object> payload = new HashMap<>();
        parser.parse(payload);

        payload.put("data", null);
        parser.parse(payload);
    }
}
