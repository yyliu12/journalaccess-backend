package com.info25.journalindex.controllers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.info25.journalindex.models.File;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.FsUtils;

/**
 * Functions for the online editor
 */
@RestController
public class OnlineEditor {
    @Autowired
    FileRepository fileRepository;

    @Autowired
    FsUtils fsUtils;

    final String HEADER_CSS = """
            <link href="https://fonts.googleapis.com/css2?family=Roboto:ital,wght@0,100..900;1,100..900&display=swap" rel="stylesheet">
            <style> * { font-family: 'Roboto' }</style>
    """;

    final String V2_HEADER = """
<link href="https://cdn.jsdelivr.net/gh/yyliu12/ckeditor5-css/ckeditor5-content.css" rel="stylesheet">
<link href="https://cdn.jsdelivr.net/gh/yyliu12/ckeditor5-css/ckeditor5-premium-features-content.css" rel="stylesheet">
<div class="ck-content">
            """;
    
    final String V2_FOOTER = """
</div>""";

    /**
     * Creates a blank html file in the filesystem and db
     * @param date what date to create the file under
     * @param path the name of the file
     * @return the id of the new file created in the db
     */
    @PostMapping("/api/onlineEditor/init")
    public String initOnlineEditor(@RequestParam("date") String date, @RequestParam("path") String path) {
        LocalDate parsedDate = DateUtils.parseFromString(date);
        java.io.File file = new java.io.File(fsUtils.getFileByDateAndPath(parsedDate, path));

        if (file.exists()) {
            return "exists";
        }

        try {
            file.getParentFile().mkdirs();
            file.createNewFile();

            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(V2_HEADER);
            bw.write(V2_FOOTER);

            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File fileModel = new File();
        fileModel.setPath(path);
        fileModel.setDate(parsedDate);
        fileModel.setCKEditorFile(true);

        fileRepository.save(fileModel);
        

        return String.valueOf(fileModel.getId());
    }

    /**
     * Writes new content to an html file
     * @param id the id of the html file
     * @param content what content to write
     * @return OK
     */
    @PostMapping("/api/onlineEditor/save")
    public String saveOnlineEditor(@RequestParam("id") int id, @RequestParam("content") String content) {
        File file = fileRepository.getById(id);

        java.io.File localFile = new java.io.File(fsUtils.getFilePathByFile(file));
        try {
            java.nio.file.Files.writeString(localFile.toPath(), HEADER_CSS + content);

        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR";
        }

        Document doc = Jsoup.parse(content);
        file.setContent(doc.body().wholeText());
        fileRepository.save(file);

        return "OK";

    }

    @PostMapping("/api/onlineEditor/v2/save")
    public String saveOnlineEditorv2(@RequestParam("id") int id, @RequestParam("content") String content) {
        File file = fileRepository.getById(id);

        java.io.File localFile = new java.io.File(fsUtils.getFilePathByFile(file));
        try {
            java.nio.file.Files.writeString(localFile.toPath(), V2_HEADER + content + V2_FOOTER);

        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR";
        }

        Document doc = Jsoup.parse(content);
        file.setContent(doc.body().wholeText());
        fileRepository.save(file);

        return "OK";

    }

    /**
     * gets text from the html file
     * @param id the id of the html file
     * @return the html file's contents
     */
    @PostMapping("/api/onlineEditor/get")
    public String getOnlineEditor(@RequestParam("id") int id) {
        File file = fileRepository.getById(id);

        java.io.File localFile = new java.io.File(fsUtils.getFilePathByFile(file));
        try {
            return java.nio.file.Files.readString(localFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    @PostMapping("/api/onlineEditor/v2/{date}/upload")
    public String uploadFileCKEditor(@RequestParam("upload") MultipartFile file,
            @PathVariable("date") String date) {
        LocalDate parsedDate = DateUtils.parseFromString(date);
        // TODO: Check if file we are overwriting files
        File fileModel = new File();
        fileModel.setDate(parsedDate);
        fileModel.setPath(file.getOriginalFilename());


        java.io.File localFile = new java.io.File(fsUtils.getFilePathByFile(fileModel));
        // Overwrite check
        if (localFile.exists()) {
            return "{\"error\": {\"message\": \"File already exists\"}}";
        }

        fileRepository.save(fileModel);


        try {
            file.transferTo(localFile);
            return "{\"url\": \"" + file.getOriginalFilename() + "\"}";
        } catch (IOException e) {
            e.printStackTrace();
            return "{\"error\": {\"message\": \"Upload failed\"}}";
        }
    }

    /**
     * Uploads a file to the journal -- used when uploading from tinyMCE
     * @param file the raw file data
     * @param date what date to upload the file to
     * @return a JSON object for tinyMCE to know the url of the file
     */
    @PostMapping("/api/onlineEditor/{date}/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
            @PathVariable("date") String date) {
        LocalDate parsedDate = DateUtils.parseFromString(date);
        // TODO: Check if file we are overwriting files
        File fileModel = new File();
        fileModel.setDate(parsedDate);
        fileModel.setPath(file.getOriginalFilename());


        java.io.File localFile = new java.io.File(fsUtils.getFilePathByFile(fileModel));

        // Overwrite check
        if (localFile.exists()) {
            return "ERROR";
        }
        fileRepository.save(fileModel);

        try {
            file.transferTo(localFile);
            return "{\"location\": \"" + file.getOriginalFilename() + "\"}";
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }
}
