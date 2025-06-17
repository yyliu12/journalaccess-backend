package com.info25.journalindex.controllers;

import java.io.File;
import java.io.FileNotFoundException;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ReactServe {
    @GetMapping("/app/**")
    public ResponseEntity<FileSystemResource> serveReactApp(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        File file = null;
        try {
            file = ResourceUtils.getFile("classpath:index.html");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        FileSystemResource fsr = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(fsr);

    }
    
}
