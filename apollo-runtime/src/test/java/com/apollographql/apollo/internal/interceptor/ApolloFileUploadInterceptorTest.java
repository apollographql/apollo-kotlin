package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.GraphqlUpload;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.ResponseFieldMapper;

import static com.google.common.truth.Truth.assertThat;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;

import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;

import static com.apollographql.apollo.internal.interceptor.ApolloFileUploadInterceptor.httpMultipartRequestBody;

public class ApolloFileUploadInterceptorTest {

    @Test
    public void singleUploadSimple() {
        RequestBody mainBody = RequestBody.create(null, "dummy request body");
        Operation operation = new Mutation() {
            @Override
            public String queryDocument() {
                return null;
            }

            @Override
            public Variables variables() {

                return new Variables() {
                    @Override
                    public Map<String, Object> valueMap() {
                        return new HashMap<String, Object>() {{
                            put("k1", "v1");
                            put("k2", file1);
                        }};
                    }
                };
            }

            @Override
            public ResponseFieldMapper responseFieldMapper() {
                return null;
            }

            @Override
            public Object wrapData(Data data) {
                return null;
            }

            @NotNull
            @Override
            public OperationName name() {
                return null;
            }

            @NotNull
            @Override
            public String operationId() {
                return null;
            }
        };
        try {
            RequestBody body = httpMultipartRequestBody(mainBody, operation);

            BufferedSink limited = new Buffer();
            Sink sink = Okio.buffer(limited);

            body.writeTo(limited);
            sink.flush();
            String a = limited.buffer().readUtf8();
            String[] sp = a.split(boundary);
            assertThat(sp).hasLength(5);
            assertThat(sp[0]).isEqualTo("");
            assertThat(sp[1]).isEqualTo("");
            assertThat(sp[4].trim()).isEqualTo("--");
        } catch (java.io.IOException e) {
            fail("httpMultipartRequestBody raised java.io.IOException");
        }

    }

    static GraphqlUpload createGraphqlUpload(String fileName, String content, final String mimeType) {
        try {
            Path tempDir = Files.createTempDirectory("graphql-tmp-test-dir");
            Path filePath = Paths.get(tempDir.toString(), fileName);
            final File f = filePath.toFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write(content);
            bw.close();
            return new GraphqlUpload() {{
                file = f;
                mimetype = mimeType;
            }};
        } catch (IOException e) {
            throw new RuntimeException();
        }

    }

    static GraphqlUpload file1 = createGraphqlUpload("test1.txt", "content_testOne", "text/plain");
    static GraphqlUpload file2 = createGraphqlUpload("test2.jpg", "content_testTwo", "image/jpeg");
    static GraphqlUpload file3 = createGraphqlUpload("test3.pdf", "content_testThree", "text/plain");
    static String boundary = "----graphql-multipart-upload-boundary-85763456--";
}
