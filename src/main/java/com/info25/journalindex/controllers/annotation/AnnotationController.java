package com.info25.journalindex.controllers.annotation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.util.ContentType;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.FsUtils;
import com.info25.journalindex.models.File;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AnnotationController {
    @Autowired
    FileRepository fileRepository;


    @GetMapping("/api/annotation/getViewer/byId/{id}")
    public ModelAndView getViewer(@PathVariable("id") String strId) throws UnsupportedEncodingException{
        int id = Integer.parseInt(strId);

        File file = fileRepository.getById(id);

        String fileExt = ContentType.getFileExt(file.getPath());


        return getHtmlViewer(file);

        /*switch (fileExt) {
            case "html":
                mav = new ModelAndView("htmlViewer");
                break;
            case "pdf":
                break;
            case "jpeg":
            case "jpg":
            case "png":
                break;
            default:
                break;
        }*/

    }


    private ModelAndView getHtmlViewer(File f) {

        
        ModelAndView mav = new ModelAndView("annotation_viewers/html.html");
        mav.addObject("date", DateUtils.formatToString(f.getDate()));
        mav.addObject("id", f.getId());
        String content = null;
        try {
            content = FsUtils.decodeBytesWithCharset(
                Files.readAllBytes(
                    Path.of(FsUtils.getFilePathByFile(f))
                )
            );
        } catch (IOException e) {
            e.printStackTrace();
        }


        

        mav.addObject("html", content);
        return mav;
    }
}
