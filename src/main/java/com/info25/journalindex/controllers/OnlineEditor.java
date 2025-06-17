package com.info25.journalindex.controllers;

import java.io.IOException;
import java.time.LocalDate;

import javax.annotation.processing.Filer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.info25.journalindex.models.File;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.FsUtils;

@RestController
public class OnlineEditor {
    @Autowired
    FileRepository fileRepository;

    final String HEADER_CSS = """
            <link href="https://fonts.googleapis.com/css2?family=Roboto:ital,wght@0,100..900;1,100..900&display=swap" rel="stylesheet">
            <style> * { font-family: 'Roboto' }</style>
                                """;

    @PostMapping("/api/onlineEditor/init")
    public String initOnlineEditor(@RequestParam("date") String date, @RequestParam("path") String path) {
        LocalDate parsedDate = DateUtils.parseFromString(date);
        java.io.File file = new java.io.File(FsUtils.getFileByDateAndPath(parsedDate, path));
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File fileModel = new File();
        fileModel.setPath(path);
        fileModel.setDate(parsedDate);
        fileRepository.save(fileModel);

        return String.valueOf(fileModel.getId());
    }

    @PostMapping("/api/onlineEditor/save")
    public String saveOnlineEditor(@RequestParam("id") int id, @RequestParam("content") String content) {
        File file = fileRepository.getById(id);

        java.io.File localFile = new java.io.File(FsUtils.getFilePathByFile(file));
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

    @PostMapping("/api/onlineEditor/get")
    public String getOnlineEditor(@RequestParam("id") int id) {
        File file = fileRepository.getById(id);

        java.io.File localFile = new java.io.File(FsUtils.getFilePathByFile(file));
        try {
            return java.nio.file.Files.readString(localFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    @PostMapping("/api/onlineEditor/{date}/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
            @PathVariable("date") String date) {
        LocalDate parsedDate = DateUtils.parseFromString(date);
        // TODO: Check if file we are overwriting files
        File fileModel = new File();
        fileModel.setDate(parsedDate);
        fileModel.setPath(file.getOriginalFilename());
        fileRepository.save(fileModel);

        java.io.File localFile = new java.io.File(FsUtils.getFilePathByFile(fileModel));
        try {
            file.transferTo(localFile);
            return "{\"location\": \"" + file.getOriginalFilename() + "\"}";
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }
}
