package com.info25.journalindex.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redirects users to the react app
 */
@Controller
public class Index {
    @GetMapping("")
    public String getMethodName() {
        return "redirect:/app";
    }
    
}
