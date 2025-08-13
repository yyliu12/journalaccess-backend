package com.info25.journalindex.services;

import com.info25.journalindex.models.File;
import com.info25.journalindex.util.FsUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OCRServerClient {
    @Autowired
    FsUtils fsUtils;

    String OCRSERVER_URL;
    String OCRSERVER_SECRET;

    CloseableHttpClient client = HttpClientBuilder.create().build();

    public OCRServerClient(ConfigService configService) {
        OCRSERVER_URL = configService.getConfigOption("ocrServerUrl");
        OCRSERVER_SECRET = configService.getConfigOption("ocrServerSecret");
    }

    public String getTextOfPDF(File f, boolean includePdfTextLayer) {
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            builder.setMode(HttpMultipartMode.LEGACY);
            builder.addPart("secret", new StringBody(OCRSERVER_SECRET, ContentType.MULTIPART_FORM_DATA));
            builder.addPart("file", new FileBody(new java.io.File(fsUtils.getFilePathByFile(f)), ContentType.DEFAULT_BINARY));
            builder.addPart("includePdfTextLayer", new StringBody(Boolean.toString(includePdfTextLayer), ContentType.MULTIPART_FORM_DATA));

            HttpEntity httpEntity = builder.build();
            HttpPost request = new HttpPost(OCRSERVER_URL + "/ocrPDF");
            request.setEntity(httpEntity);

            CloseableHttpResponse httpResponse = client.execute(request);
            String response = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");

            return response;
        } catch (Exception e) {
            return "An error occurred";
        }
    }

    public String getTextOfImage(File f) {
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            builder.setMode(HttpMultipartMode.LEGACY);
            builder.addPart("secret", new StringBody(OCRSERVER_SECRET, ContentType.MULTIPART_FORM_DATA));
            builder.addPart("file", new FileBody(new java.io.File(fsUtils.getFilePathByFile(f)), ContentType.DEFAULT_BINARY));

            HttpEntity httpEntity = builder.build();
            HttpPost request = new HttpPost(OCRSERVER_URL + "/ocrImage");
            request.setEntity(httpEntity);

            CloseableHttpResponse httpResponse = client.execute(request);
            String response = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");

            return response;
        } catch (Exception e) {
            return "An error occurred";
        }
    }

}
