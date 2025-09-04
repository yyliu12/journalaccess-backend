package com.info25.journalindex.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.info25.journalindex.models.File;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.services.OCRServerClient;
import com.info25.journalindex.services.TextExtractionService;
import com.info25.journalindex.util.ContentType;
import com.info25.journalindex.util.FileTypes;

@RestController
@RequestMapping("/api/ocrserver")
public class OCRServerApi {
    @Autowired
    OCRServerClient ocrServerClient;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    TextExtractionService textExtractionService;

    @PostMapping("/getTextOfPdf")
    public String getTextOfPdf(
        @RequestParam("id") int fileId, 
        @RequestParam("includePdfTextLayer") boolean includePdfTextLayer
    ) {
        File f = fileRepository.getById(fileId);

        return ocrServerClient.getTextOfPDF(f, includePdfTextLayer);
    }

    @PostMapping("/getTextOfImage")
    public String getTextOfImage(@RequestParam("id") int fileId) {
        File f = fileRepository.getById(fileId);

        return ocrServerClient.getTextOfImage(f);
    }

    @PostMapping("/getText")
    public String getText(
        @RequestParam("id") int fileId,
        @RequestParam("includePdfTextLayer") boolean includePdfTextLayer
    ) {
        File f = fileRepository.getById(fileId);
        
        return textExtractionService.getText(f, includePdfTextLayer);
    }
}
