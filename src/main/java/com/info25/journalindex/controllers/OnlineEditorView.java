package com.info25.journalindex.controllers;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.info25.journalindex.models.File;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.FsUtils;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/api/onlineEditor/v2")
public class OnlineEditorView {
    @Autowired
    FileRepository fileRepository;

    @Autowired
    FsUtils fsUtils;

    @RequestMapping("/frame/{id}/")
    public String frame(@PathVariable("id") int id, Model m) throws IOException {

        File file = fileRepository.getById(id);

        java.io.File localFile = new java.io.File(fsUtils.getFilePathByFile(file));
        String content = java.nio.file.Files.readString(localFile.toPath());

        Document doc = Jsoup.parse(content);
        
        Element contentDiv = doc.selectFirst("div.ck-content");


        m.addAttribute("text", contentDiv.html());
        m.addAttribute("date", DateUtils.formatToString(file.getDate()));
        m.addAttribute("id", id);

        return "ckeditor/frame";
    }

    @RequestMapping("/frame/{id}/**")
    public String frameWithPath(@PathVariable("id") int id, HttpServletRequest req) throws IOException {
        String request = req.getRequestURL().toString();

        String path = request.substring(request.indexOf("/frame/" + id) + ("/frame/" + id).length());
        File file = fileRepository.getById(id);

        return "redirect:/api/files/getFile/" + DateUtils.formatToString(file.getDate()) + path;
    }
}
