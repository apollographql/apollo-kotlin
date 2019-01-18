package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.GraphqlUpload;
import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.InputType;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.internal.json.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
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

    private static void recursiveGetUploadData(Object value, String variableName, ArrayList<GraphqlUpload> allUploads, HashMap<String, String[]> filesMap) {
        if (value instanceof InputType) {
            try {
                Field[] fields = value.getClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object subValue = field.get(value);
                    String key = field.getName();
                    recursiveGetUploadData(subValue, variableName + "." + key, allUploads, filesMap);
                }
            } catch (IllegalAccessException e) {
                // never happen
            }
        } else if (value instanceof Input) {
            Object unwrappedValue = ((Input) value).value;
            recursiveGetUploadData(unwrappedValue, variableName, allUploads, filesMap);
        } else if (value instanceof GraphqlUpload) {
            GraphqlUpload upload = (GraphqlUpload) value;
            allUploads.add(upload);
            filesMap.put("" + filesMap.size(), new String[]{variableName});
        } else if (value instanceof GraphqlUpload[]) {
            int varFileIndex = 0;
            GraphqlUpload[] uploads = (GraphqlUpload[]) value;
            for (GraphqlUpload upload : uploads) {
                allUploads.add(upload);
                filesMap.put("" + filesMap.size(), new String[]{variableName + "." + varFileIndex});
                varFileIndex++;
            }
        } else if (value instanceof Collection) {
            Object[] listData = ((Collection) value).toArray();
            for (int i = 0; i < listData.length; i++) {
                Object subValue = listData[i];
                recursiveGetUploadData(subValue, variableName + "." + i, allUploads, filesMap);
            }
        }
    }

    public static UploadData getUploadData(Operation operation) {

        ArrayList<GraphqlUpload> allUploads = new ArrayList<>();
        HashMap<String, String[]> filesMap = new HashMap<>();
        for (String variableName : operation.variables().valueMap().keySet()) {
            Object value = operation.variables().valueMap().get(variableName);
            recursiveGetUploadData(value, "variables." + variableName, allUploads, filesMap);
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
        for (String key : filesMap.keySet()) {
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
        for (GraphqlUpload upload : uploads) {
            multipartBodyBuilder.addFormDataPart("" + index, upload.file.getName(),
                    RequestBody.create(upload.getMimetype(), upload.file));
            index++;
        }
        return multipartBodyBuilder.build();
    }

}
