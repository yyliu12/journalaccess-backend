package com.info25.journalindex.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.info25.journalindex.models.File;
import com.info25.journalindex.models.OOFile;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.repositories.OOFileRepository;
import com.info25.journalindex.services.ConfigService;
import com.info25.journalindex.services.OOServiceClient;
import com.info25.journalindex.util.OnlyOfficeUtil;

import org.bouncycastle.jcajce.BCFKSLoadStoreParameter.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
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

    @Autowired
    OnlyOfficeUtil onlyOfficeUtil;

    String OO_SECRET;
    String OO_SERVER_URL;
    String COLLABORA_URL;
    String ONLYOFFICE_URL;
    String ONLYOFFICE_SECRET;

    public OOFileEditor(ConfigService configService) {
        OO_SECRET = configService.getConfigOption("ooServerSecret");
        COLLABORA_URL = configService.getConfigOption("collaboraUrl");
        ONLYOFFICE_URL = configService.getConfigOption("onlyOfficeUrl");
        OO_SERVER_URL = configService.getConfigOption("ooServerUrl");
        ONLYOFFICE_SECRET = configService.getConfigOption("onlyOfficeSecret");
    }

    @GetMapping("/frame")
    public String frame(@RequestParam("id") int id, @RequestParam(value = "sessionId", required = false, defaultValue = "-1") long sessionId, Model model) {
        File f = fileRepository.getById(id);
        OOFile ooFile = ooFileRepository.findById(f.getOOFileId());

        if (sessionId == -1) {
            sessionId = ooServiceClient.initiateSession(ooFile);
            return "redirect:/api/oofile/frame?id=" + id + "&sessionId=" + sessionId;
        }

        model.addAttribute("collaboraUrl", COLLABORA_URL);
        model.addAttribute("onlyOfficeUrl", ONLYOFFICE_URL);
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("fileId", id);

        if (ooFile.isCollabora()) 
            return "oofile/frame";
        
        model.addAttribute("onlyOfficeData", onlyOfficeUtil.calculateTokenPackage(sessionId, id, ooFile));


        return "oo2file/frame";
    }

    @GetMapping("/editor")
    public String editor(@RequestParam("sessionId") long id, Model model) {
        model.addAttribute("sessionId", id);
        model.addAttribute("ooSecret", OO_SECRET);
        model.addAttribute("collaboraUrl", COLLABORA_URL);

        return "oofile/editor";
    }

    @GetMapping("/save")
    @ResponseBody
    public CompletableFuture<String> save(@RequestParam("sessionId") long sessionId,
                                          @RequestParam("fileId") int fileId) {
        return ooServiceClient.saveCollabora(sessionId, fileId);
    }

    @GetMapping("/saveOnlyOffice")
    @ResponseBody
    public CompletableFuture<String> saveOnlyOffice(@RequestParam("sessionId") long sessionId, 
                                                    @RequestParam("fileId") int fileId) {
    
        return ooServiceClient.saveOnlyOffice(sessionId, fileId);
    }



}
