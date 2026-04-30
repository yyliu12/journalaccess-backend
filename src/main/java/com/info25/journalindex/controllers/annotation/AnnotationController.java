package com.info25.journalindex.controllers.annotation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import com.info25.journalindex.models.File;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.util.ContentType;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.FsUtils;
import com.itextpdf.forms.xfdf.XfdfObject;
import com.itextpdf.forms.xfdf.XfdfObjectFactory;
import com.itextpdf.kernel.geom.AffineTransform;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.research.xfdfmerge.XfdfMerge;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/api/annotation")
public class AnnotationController {
    // write False here if you don't have a nutrient license
    // a copy of the nutrient web SDK should be located in the
    // resources/static/nutrient
    // folder.
    // if False, then PDF annotations will be disabled
    final boolean NUTRIENT_LICENSE = true;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    FsUtils fsUtils;

    // object return type: sorry! But it needs to return a filesystemresource or
    // ModelAndView
    @GetMapping("/getEditor/byId/{id}")
    public Object getEditor(@PathVariable("id") String strId) throws UnsupportedEncodingException {
        int id = Integer.parseInt(strId);
        File file = fileRepository.getById(id);
        String fileExt = ContentType.getFileExt(file.getPath());

        switch (fileExt) {
            case "html":
                return getHtmlEditorAndViewer(file);
            case "pdf":
                if (!NUTRIENT_LICENSE) {
                    return "No nutrient web SDK license, PDF annotations are disabled";
                }
                return getPdfEditor(file);
            case "jpeg":
            case "jpg":
            case "png":
                return getImageEditorAndViewer(file);
            default:
                break;
        }

        return null;
    }

    // Returns the right viewer for the annotation type
    @GetMapping("/getViewer/byId/{id}")
    public Object getViewer(@PathVariable("id") String strId, HttpServletRequest request)
            throws UnsupportedEncodingException {
        int id = Integer.parseInt(strId);
        File file = fileRepository.getById(id);
        String fileExt = ContentType.getFileExt(file.getPath());

        switch (fileExt) {
            case "html":
                return "redirect:/api/annotation/getViewer/byId/" + id + "/";
            case "pdf":
                return getPdfViewer(file);
            case "jpeg":
            case "jpg":
            case "png":
                return getImageEditorAndViewer(file);
            default:
                break;
        }

        return null;
    }

    @GetMapping("/getViewer/byId/{id}/")
    public Object getHTMLViewer(@PathVariable("id") String id) {
        File file = fileRepository.getById(Integer.valueOf(id));

        return getHtmlEditorAndViewer(file);
    }


    @GetMapping("/getViewer/byId/{id}/**")
    public String getResource(@PathVariable("id") int id, HttpServletRequest req) {
        Object uriObject = req.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String urlParts = "";
        String split = String.valueOf(id);
        if (null != uriObject) {
            String uri = uriObject.toString();
            urlParts = uri.substring(uri.indexOf("/byId/" + split + "/") + ("/byId" + split + "/").length() + 1);
        }

        File f = fileRepository.getById(id);

        // use path class to do path splitting, but behavior varies across OS-es
        Path p = Path.of(f.getPath().replace("\\", java.io.File.separator)); 
        
        String additional = p.getParent() == null ? "" : p.getParent().toString() + "/";

        if (req.getHeader("Sec-Fetch-Mode").equals("navigate")) {
            // let's serve the annotation viewer this time

            File viewingFile = fileRepository.getByDateAndPath(f.getDate(), URLDecoder.decode(additional + urlParts, StandardCharsets.UTF_8).replace("/", "\\"));
            return "redirect:/api/annotation/getViewer/byId/" + viewingFile.getId();
        } else {
            // serve raw file because this is being embedded
            return "redirect:/api/files/getFile/" + DateUtils.formatToString(f.getDate()) + "/" + additional + urlParts;
        }
    }

    // Returns a pdf file after opening it with iText and adding annotations
    private ResponseEntity<byte[]> getPdfViewer(File f) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocument pdfDoc = null;
        try {
            pdfDoc = new PdfDocument(
                    new PdfReader(fsUtils.getFilePathByFile(f)),
                    new PdfWriter(out));
        } catch (IOException e) {
            e.printStackTrace();
        }

        XfdfObjectFactory factory = new XfdfObjectFactory();
        XfdfObject xfdfObject = null;
        try {
            xfdfObject = factory.createXfdfObject(new ByteArrayInputStream(
                    f.getAnnotation().getBytes()));
        } catch (Exception e) {
            // e.printStackTrace();
        }

        // no annotations for doc if null, just close the pdfDoc
        if (xfdfObject != null) {
            XfdfMerge xfdfMerge = new XfdfMerge(pdfDoc, new AffineTransform(), 0);
            xfdfMerge.mergeXfdfIntoPdf(xfdfObject);
        }

        pdfDoc.close();

        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"out.pdf\"")
                .header("Content-Type", "application/pdf")
                .body(out.toByteArray());

    }

    // Returns the notrient pdf editor with the correct pdf file
    private ModelAndView getPdfEditor(File f) {
        ModelAndView mav = new ModelAndView("annotation_viewers/pdf.html");
        mav.addObject("id", f.getId());

        return mav;
    }

    // Returns the HTML file with annotator.js
    private ModelAndView getHtmlEditorAndViewer(File f) {
        ModelAndView mav = new ModelAndView("annotation_viewers/html.html");
        mav.addObject("date", DateUtils.formatToString(f.getDate()));
        mav.addObject("id", f.getId());
        String content = null;
        try {
            content = fsUtils.decodeBytesWithCharset(
                    Files.readAllBytes(
                            Path.of(fsUtils.getFilePathByFile(f))));
        } catch (IOException e) {
            e.printStackTrace();
        }

        mav.addObject("html", content);
        return mav;
    }

    // Returns an html file that has annotorious seadragon set up with the correct
    // image
    public ModelAndView getImageEditorAndViewer(File f) {
        ModelAndView mav = new ModelAndView("annotation_viewers/image.html");
        mav.addObject("id", f.getId());
        return mav;
    }
}
