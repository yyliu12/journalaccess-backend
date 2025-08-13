package com.info25.journalindex.controllers.annotation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/getViewer/byId/{id}")
    public Object getViewer(@PathVariable("id") String strId, HttpServletRequest request)
            throws UnsupportedEncodingException {
        int id = Integer.parseInt(strId);
        File file = fileRepository.getById(id);
        String fileExt = ContentType.getFileExt(file.getPath());

        switch (fileExt) {
            case "html":
                return getHtmlEditorAndViewer(file);
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
            e.printStackTrace();
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


    private ModelAndView getPdfEditor(File f) {
        ModelAndView mav = new ModelAndView("annotation_viewers/pdf.html");
        mav.addObject("id", f.getId());

        return mav;
    }

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

    public ModelAndView getImageEditorAndViewer(File f) {
        ModelAndView mav = new ModelAndView("annotation_viewers/image.html");
        mav.addObject("id", f.getId());
        return mav;
    }
}
