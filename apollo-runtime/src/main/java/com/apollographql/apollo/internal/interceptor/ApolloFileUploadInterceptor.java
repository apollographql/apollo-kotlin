package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.GraphqlUpload;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.internal.json.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.Buffer;

import static com.apollographql.apollo.internal.interceptor.ApolloServerInterceptor.MEDIA_TYPE;


class UploadData {
    public ArrayList<GraphqlUpload> allUploads = null;
    public HashMap<String, String[]> filesMap = null;
}


public class ApolloFileUploadInterceptor {

    public static UploadData getUploadData(Operation operation) {

        ArrayList<GraphqlUpload> allUploads = new ArrayList<>();
        HashMap<String, String[]> filesMap = new HashMap<>();
        int fileIndex = 0;
        for (String variableName : operation.variables().valueMap().keySet()) {
            Object value = operation.variables().valueMap().get(variableName);
            if (value instanceof GraphqlUpload) {
                GraphqlUpload upload = (GraphqlUpload)value;
                allUploads.add(upload);
                filesMap.put("" + fileIndex, new String[] { "variables." + variableName });
                fileIndex++;
            }
            else if (value instanceof  GraphqlUpload[]) {
                int varFileIndex = 0;
                GraphqlUpload[] uploads = (GraphqlUpload[])value;
                for (GraphqlUpload upload: uploads) {
                    allUploads.add(upload);
                    filesMap.put("" + fileIndex, new String[] { "variables." + variableName + "." + varFileIndex });
                    varFileIndex++;
                    fileIndex++;
                }
            }
        }

        final ArrayList<GraphqlUpload> allUploadsRef = allUploads;
        final HashMap<String, String[]> filesMapRef = filesMap;

        return new UploadData() {{
            allUploads = allUploadsRef;
            filesMap = filesMapRef;
        }};
    }

    public static RequestBody httpMultipartRequestBody(RequestBody mainBody, Operation operation) throws IOException {

        UploadData uploadData = getUploadData(operation);
        if (uploadData.allUploads.isEmpty()) {
            return mainBody;
        }


        HashMap<String, String[]> filesMap = uploadData.filesMap;
        GraphqlUpload[] uploads = uploadData.allUploads.toArray(new GraphqlUpload[0]);


        Buffer buffer = new Buffer();
        JsonWriter jsonWriter = JsonWriter.of(buffer);
        jsonWriter.setSerializeNulls(true);
        jsonWriter.beginObject();
        for (String key: filesMap.keySet()) {
            jsonWriter.name(key).beginArray();
            jsonWriter.value((filesMap.get(key))[0]);
            jsonWriter.endArray();
        }
        jsonWriter.endObject();
        jsonWriter.close();
        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder("--graphql-multipart-upload-boundary-85763456--")
                .setType(MultipartBody.FORM)
                .addFormDataPart("operations", null, mainBody)
                .addFormDataPart("map", null, RequestBody.create(MEDIA_TYPE, buffer.readByteString()));
        int index = 0;
        for (GraphqlUpload upload: uploads) {
            multipartBodyBuilder.addFormDataPart("" + index, upload.file.getName(),
                    RequestBody.create(MediaType.parse(upload.mimetype), upload.file));
        }
        return multipartBodyBuilder.build();
    }

}
