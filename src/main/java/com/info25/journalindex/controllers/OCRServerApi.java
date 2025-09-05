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

/**
 * API used to extract text from files
 */
@RestController
@RequestMapping("/api/ocrserver")
public class OCRServerApi {
    @Autowired
    OCRServerClient ocrServerClient;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    TextExtractionService textExtractionService;

    /**
     * Gets text of PDF
     * @param fileId the id of the file
     * @param includePdfTextLayer whether to include the existing text layer on the pdf or not
     * @return the text of the pdf
     */
    @PostMapping("/getTextOfPdf")
    public String getTextOfPdf(
        @RequestParam("id") int fileId, 
        @RequestParam("includePdfTextLayer") boolean includePdfTextLayer
    ) {
        File f = fileRepository.getById(fileId);

        return ocrServerClient.getTextOfPDF(f, includePdfTextLayer);
    }

    /**
     * Gets text of image
     * @param fileId the id of the file
     * @return the text of the image
     */
    @PostMapping("/getTextOfImage")
    public String getTextOfImage(@RequestParam("id") int fileId) {
        File f = fileRepository.getById(fileId);

        return ocrServerClient.getTextOfImage(f);
    }

    /**
     * Gets text of the file; takes appropriate action based on file type
     * utilizes the TextExtractionService
     * 
     * @param fileId the id of the file
     * @param includePdfTextLayer only valid for PDFs; whether to include the existing text layer on the pdf or not
     * @return the text of the file
     */
    @PostMapping("/getText")
    public String getText(
        @RequestParam("id") int fileId,
        @RequestParam("includePdfTextLayer") boolean includePdfTextLayer
    ) {
        File f = fileRepository.getById(fileId);
        
        return textExtractionService.getText(f, includePdfTextLayer);
    }
}
