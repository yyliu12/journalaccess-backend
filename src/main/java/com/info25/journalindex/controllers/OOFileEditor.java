package com.info25.journalindex.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.info25.journalindex.models.File;
import com.info25.journalindex.models.OOFile;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.repositories.OOFileRepository;
import com.info25.journalindex.services.ConfigService;
import com.info25.journalindex.services.OOServiceClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Controller
@RequestMapping("/api/oofile/")
public class OOFileEditor {
    @Autowired
    OOServiceClient ooServiceClient;

    @Autowired
    OOFileRepository ooFileRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    ConfigService configService;

    String OO_SECRET;

    public OOFileEditor(ConfigService configService) {
        OO_SECRET = configService.getConfigOption("ooServerSecret");
    }

    @GetMapping("/frame")
    public String frame(@RequestParam("id") int id, Model model) {
        File f = fileRepository.getById(id);
        OOFile ooFile = ooFileRepository.findById(f.getOOFileId());

        int sessionId = ooServiceClient.initiateSession(ooFile);

        model.addAttribute("sessionId", sessionId);
        model.addAttribute("fileId", id);

        return "oofile/frame";
    }

    @GetMapping("/editor")
    public String editor(@RequestParam("sessionId") int id, Model model) {
        model.addAttribute("sessionId", id);
        model.addAttribute("ooSecret", OO_SECRET);

        return "oofile/editor";
    }

    @GetMapping("/save")
    @ResponseBody
    public CompletableFuture<String> save(@RequestParam("sessionId") int sessionId,
                                           @RequestParam("fileId") int fileId) {
        return ooServiceClient.save(sessionId, fileId);
    }

    // For future
    private String calculateAccessToken(int sessionId) {
        ObjectMapper om = new ObjectMapper();
        ObjectNode data = om.createObjectNode();

        data.put("validity", java.time.Instant.now().getEpochSecond());
        data.put("sessionId", sessionId);

        ObjectNode token = om.createObjectNode();
        token.put("data", data.toString());

        SecretKeySpec secretKeySpec = new SecretKeySpec(OO_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        Mac mac = null;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        byte[] hmacBytes = mac.doFinal(data.toString().getBytes(StandardCharsets.UTF_8));


        token.put("signature", Base64.getEncoder().encodeToString(hmacBytes));
        return token.toString();
    }

}
