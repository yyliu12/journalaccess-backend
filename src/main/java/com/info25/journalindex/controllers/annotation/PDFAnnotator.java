package com.info25.journalindex.controllers.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.info25.journalindex.models.File;
import com.info25.journalindex.repositories.FileRepository;

@RestController
@RequestMapping("/api/annotation/pdf")
public class PDFAnnotator {
    @Autowired
    FileRepository fileRepository;

    @PostMapping("/{id}/save")
    public String saveAnnotation(@PathVariable("id") int id, @RequestBody String xfdfData) {
        File f = fileRepository.getById(id);
        f.setAnnotations(xfdfData);
        fileRepository.save(f);
        return "OK";
    }

    @PostMapping("/{id}/get")
    public String getAnnotations(@PathVariable("id") int id) {
        File f = fileRepository.getById(id);
        String annotations = f.getAnnotations();
        
        return annotations;
    }
}
