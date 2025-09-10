package com.info25.journalindex.services;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.info25.journalindex.models.File;
import com.info25.journalindex.util.ContentType;
import com.info25.journalindex.util.FileTypes;
import com.info25.journalindex.util.FsUtils;

@Service
public class TextExtractionService {
    @Autowired
    OCRServerClient ocrServerClient;

    @Autowired
    FsUtils fsUtils;
    /**
     * Gets text from a file. Supports all types of files allowed in Journal access.
     * 
     * Picks the right function for extracting text automatically.
     * @param f what file in the db to extract text from
     * @param includePdfTextLayer whether to include the existing pdf text layer or not. ignored if the file is not a pdf
     * @return
     */
    public String getText(File f, boolean includePdfTextLayer) {
        switch (ContentType.getFileType(f)) {
            case FileTypes.IMAGE:
                return ocrServerClient.getTextOfImage(f);
            case FileTypes.PDF:
                return ocrServerClient.getTextOfPDF(f, includePdfTextLayer);
            case FileTypes.WEBPAGE:
                try {
                    // Jsoup html text parsing
                    
                    byte[] fileData = Files.readAllBytes(Paths.get(fsUtils.getFilePathByFile(f)));
                    return Jsoup.parse(fsUtils.decodeBytesWithCharset(fileData)).body().wholeText();
                } catch (Exception e) {
                    System.out.println(e);
                }

                return "";
            default:
                return "";
        }
    }
}
