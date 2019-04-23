package com.chepito.cloudvision;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;

/**
 * Created by chepito on 05-19-18.
 */

public class core {

    public static Vision getVisionInstance() {

        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();



        Vision.Builder visionBuilder = new Vision.Builder(
                httpTransport,
                jsonFactory,
                null);

        visionBuilder.setVisionRequestInitializer(
                new VisionRequestInitializer("AIzaSyDhpbLnIzP0UcDJfQwN0lPECIr8_nFslAQ"));

        return  visionBuilder.build();
    }
}
