package com.info25.journalindex.controllers;

import java.io.File;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ReactServe {
    final File REACT_APP_PATH = new File("C:\\Users\\yuyan\\Desktop\\jaui\\build");

    @GetMapping("/app/**")
    public ResponseEntity<FileSystemResource> serveReactApp(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        File file = new File(REACT_APP_PATH, path);
        if (!file.exists() || !file.isFile()) {
            file = new File(REACT_APP_PATH, "index.html");
        }

        FileSystemResource fsr = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(fsr);

    }
    
}
