package com.info25.journalindex.services;

import com.info25.journalindex.models.File;
import com.info25.journalindex.models.OOFile;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.repositories.OOFileRepository;
import com.info25.journalindex.util.FsUtils;
import com.info25.journalindex.util.TikaTextExtractor;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.http.entity.ContentType;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ParsingReader;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tomcat.util.http.fileupload.MultipartStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.tika.sax.BodyContentHandler;

/**
 * Service for communicating with the Collabora mini service
 */
@Service
public class OOServiceClient {
    String OO_SERVER_URL;
    String OO_SERVER_SECRET;
    Path RESOURCES_FOLDER;

    CloseableHttpClient client = HttpClientBuilder.create().build();
    CloseableHttpAsyncClient asyncClient = HttpAsyncClientBuilder.create().build();
    ExecutorService es = Executors.newCachedThreadPool();

    @Autowired
    ConfigService configService;

    @Autowired
    FsUtils fsUtils;

    @Autowired
    private FileRepository fileRepository;
    
    @Autowired
    private OOFileRepository oOFileRepository;

    @Autowired
    TikaTextExtractor tikaTextExtractor;


    public OOServiceClient(ConfigService configService) {
        OO_SERVER_URL = configService.getConfigOption("ooServerUrl");
        OO_SERVER_SECRET = configService.getConfigOption("ooServerSecret");
        RESOURCES_FOLDER = Path.of(configService.getConfigOption("resourcesFolder")).resolve("oo-resources");

        asyncClient.start();
    }

    public int initiateSession(OOFile ooFile) {

        HttpPost httpPost = new HttpPost(OO_SERVER_URL + "/initiate-session" + "?access_token=" + OO_SERVER_SECRET);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.addBinaryBody("file", new java.io.File(fsUtils.getOOFilePath(ooFile)));

        httpPost.setEntity(builder.build());

        try {
            CloseableHttpResponse closeableHttpResponse = client.execute(httpPost);
            String resp = EntityUtils.toString(closeableHttpResponse.getEntity(), "UTF-8");
            int id = Integer.parseInt(resp);

            EntityUtils.consume(closeableHttpResponse.getEntity());

            return id;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<String> save(int sessionId, int fileId) {

        HttpGet httpPost = new HttpGet(OO_SERVER_URL + "/hold-for-save/" + sessionId + "?access_token=" + OO_SERVER_SECRET);

        CompletableFuture<String> cf = new CompletableFuture<>();

        File f = fileRepository.getById(fileId);
        OOFile ooFile = oOFileRepository.findById(f.getOOFileId());

        asyncClient.execute(
                SimpleRequestProducer.create(SimpleHttpRequest.copy(httpPost)),
                SimpleResponseConsumer.create(),
                new FutureCallback<SimpleHttpResponse>() {
                    @Override
                    public void completed(SimpleHttpResponse result) {

                        System.out.println("request finished??");

                        byte[] data = result.getBodyBytes();

                        // System.out.println(new String(data, StandardCharsets.UTF_8));


                        String contentType = "";
                        try {
                            contentType = result.getHeader("Content-Type").getValue();
                        } catch (ProtocolException e) {
                            e.printStackTrace();
                        }

                        ContentType ct = ContentType.parse(contentType);

                        MultipartStream ms = new MultipartStream(new ByteArrayInputStream(data),
                                ct.getParameter("boundary").getBytes(),
                                1024,
                                null);

                        boolean nextPart = false;
                        try {
                            nextPart = ms.skipPreamble();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        System.out.println("now reading parts");

                        while (nextPart) {
                            try {
                                String partHeaders = ms.readHeaders();

                                ContentDisposition cd = ContentDisposition.parse(partHeaders);

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                                if (cd.getName().strip().equals("raw")) {
                                    ms.readBodyData(baos);

                                    baos.writeTo(new FileOutputStream(fsUtils.getOOFilePath(ooFile)));


                                    String extractedText = tikaTextExtractor.extractText(baos.toByteArray());

                                    f.setContent(extractedText);
                                    
                                    fileRepository.save(f);                                    

                                } else if (cd.getName().strip().equals("pdf")) {
                                    ms.readBodyData(new FileOutputStream(
                                            fsUtils.getFilePathByFile(f)
                                    ));
                                }

                                nextPart = ms.readBoundary();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                        
                        System.out.println("Done!");
                        cf.complete("OK");
                    }

                    @Override
                    public void failed(Exception ex) {
                        System.out.println("request failed??");
                        ex.printStackTrace();
                    }

                    @Override
                    public void cancelled() {

                    }
                });



        return cf;
    }


}
