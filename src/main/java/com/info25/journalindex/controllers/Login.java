package com.info25.journalindex.controllers;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.ArrayList;

/**
 * Controllers for login page. Also responsible for the random login page image.
 *
 * To customize login bg: create "loginbg" folder in resources folder, then add
 * jpg images, only.
 *
 * Created: 7/22/2025
 */
@Controller
public class Login {
    ArrayList<String> loginBgs = new ArrayList<String>();
    boolean useDefaultBg = false;

    /**
     * initing the class involves checking what images are in the loginbg folder
     * in the resources folder of the app
     */
    public Login() {
        try {
            File loginBgFolder = ResourceUtils.getFile("classpath:loginbg");
            if (loginBgFolder.exists() && loginBgFolder.isDirectory()) {
                File[] files = loginBgFolder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(".jpg")) {
                            // this is a login bg, add to list
                            loginBgs.add(file.getAbsolutePath());
                        }
                    }
                }
                // no login bgs -- use default
                if (loginBgs.isEmpty()) {
                    useDefaultBg = true;
                }
            } else {
                // login bg folder doesn't exist -- use default
                useDefaultBg = true;
            }
        } catch (Exception e) {
            // error -- use default
            useDefaultBg = true;
        }
    }

    /**
     * login page endpoint
     * @return login template
     */
    @GetMapping("login")
    public String login() {
        return "login";
    }

    /**
     * Returns random login background image.
     * @return jpg image
     */
    @GetMapping("loginBg.jpg")
    @ResponseBody // this is a controller class but this function doesn't return template
    public ResponseEntity<FileSystemResource> loginBg() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setCacheControl("no-cache, no-store, must-revalidate"); // so it's always random!
        FileSystemResource fsr = null;
        // when class was created we figured out whether there were login bg images
        if (useDefaultBg) {
            try {
                // default bg image included on every installation
                fsr = new FileSystemResource(ResourceUtils.getFile("classpath:defaultbg.jpg"));
            } catch (Exception e) {
                return null; // Default background not found
            }
        } else {
            // choose random bg
            int randomIndex = (int) (Math.random() * loginBgs.size());
            fsr = new FileSystemResource(loginBgs.get(randomIndex));
        }

        return new ResponseEntity<>(fsr, headers, HttpStatus.OK);
    }
}
