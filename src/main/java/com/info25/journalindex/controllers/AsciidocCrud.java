package com.info25.journalindex.controllers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.info25.journalindex.models.File;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.FsUtils;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/asciidoc/")
public class AsciidocCrud {
    final String ASCIIDOC_HEADER = """
<link href="https://cdn.jsdelivr.net/gh/yyliu12/ckeditor5-css/asciidoctor.css" rel="stylesheet">

            """;

    @Autowired
    FsUtils fsUtils;

    @Autowired
    FileRepository fileRepository;

    @PostMapping("create")
    public String create(@RequestParam("filename") String fileName,
                         @RequestParam("date") String date) {
        File f = new File();
        LocalDate parsedDate = DateUtils.parseFromString(date);
        f.setDate(parsedDate);
        f.setPath(fileName + ".html");
        f.setAsciidoc(true);
        
        java.io.File file = new java.io.File(fsUtils.getFilePathByFile(f));
        if (file.exists()) {
            return "exists";
        }

        fileRepository.save(f);

        try {
            file.getParentFile().mkdirs();
            file.createNewFile();

            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(ASCIIDOC_HEADER);
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }

        try {
            java.io.File asciidocFile = new java.io.File(fsUtils.getAsciidocPath(f));
            asciidocFile.getParentFile().mkdirs();
            asciidocFile.createNewFile();

            BufferedWriter bw = new BufferedWriter(new FileWriter(asciidocFile));
            bw.write("");
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }

        
        
        return "ok";
    }

    @GetMapping("/editor/{id}/")
    public ModelAndView getEditor(@PathVariable("id") int id) throws IOException {
        ModelAndView mav = new ModelAndView("asciidoc/editor");
        
        File f = fileRepository.getById(id);

        String asciidoc = Files.readString(Path.of(fsUtils.getAsciidocPath(f)));

        mav.addObject("content", asciidoc);

        return mav;
    }

    @GetMapping("/editor/{id}/**")
    public ModelAndView getResource(@PathVariable("id") int id, HttpServletRequest req) {
        String request = req.getRequestURL().toString();

        String path = request.substring(request.indexOf("/editor/" + id) + ("/editor/" + id).length());
        File file = fileRepository.getById(id);

        return new ModelAndView("redirect:/api/files/getFile/" + DateUtils.formatToString(file.getDate()) + path);
    }

    @PostMapping("/editor/{id}/save")
    public String save(@PathVariable("id") int id, @RequestParam("asciidoc") String asciidoc, @RequestParam("html") String html) {
        File file = fileRepository.getById(id);

        Document doc = Jsoup.parse(html);
        file.setContent(doc.text());

        fileRepository.save(file);

        try {
            Files.writeString(Path.of(fsUtils.getAsciidocPath(file)), asciidoc);
            Files.writeString(Path.of(fsUtils.getFilePathByFile(file)), ASCIIDOC_HEADER + html);
        } catch (IOException e) {
            e.printStackTrace();
            return "error";
        }
        return "ok";
    }
}
