package com.info25.journalindex.controllers.annotation;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.info25.journalindex.models.File;
import com.info25.journalindex.repositories.FileRepository;

/**
 * This file handles annotations for PDF files.
 * 
 * This is different from the html and image annotators because the annotation data
 * is saved all at once. There are no individual annotation modifications.
 * 
 * We use the nutrient pdf viewer.
 */
@RestController
@RequestMapping("/api/annotation/pdf")
public class PDFAnnotator {
    @Autowired
    FileRepository fileRepository;

    /**
     * Saves the annotations for a PDF file.
     * @param id The id of the file
     * @param xfdfData The XFDF data to save
     * @return the string "OK"
     */
    @PostMapping("/{id}/save")
    public String saveAnnotation(@PathVariable("id") int id, @RequestBody String xfdfData) {
        File f = fileRepository.getById(id);
        f.setAnnotations(cleanXFDF(xfdfData));
        f.setAnnotationContent(getRawTextOfAnnotations(xfdfData));

        fileRepository.save(f);
        return "OK";
    }

    /**
     * Gets the annotations for a PDF file.
     * @param id The id of the file
     * @return XML XFDF data.
     */
    @PostMapping("/{id}/get")
    public String getAnnotations(@PathVariable("id") int id) {
        File f = fileRepository.getById(id);
        String annotations = f.getAnnotations();

        return annotations;
    }

    /**
     * Gets the combined raw text of every annotation in the XFDF data
     * @param doc The XFDF document as a string
     * @return The combined raw text of all annotations
     */
    private String getRawTextOfAnnotations(String doc) {
        // read xml file
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        Document xmlDoc = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
            xmlDoc = docBuilder.parse(new ByteArrayInputStream(doc.getBytes()));
        } catch (Exception e) { }
        StringBuilder sb = new StringBuilder();
        // annotations are stored as <contents> tags.
        NodeList contents = xmlDoc.getElementsByTagName("contents");
        for (int i = 0; i < contents.getLength(); i++) {
            Node content = contents.item(i);
            sb.append(content.getTextContent());
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Removes popup annotations that serve no purpose and accumulate.
     * @param doc The XFDF document as a string
     * @return The cleaned XFDF document as a string
     */
    private String cleanXFDF(String doc) {
        // read xml file
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        Document xmlDoc = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
            xmlDoc = docBuilder.parse(new ByteArrayInputStream(doc.getBytes()));
        } catch (Exception e) { }

        Node annots = xmlDoc.getElementsByTagName("annots").item(0);
        if (annots == null) {
            return doc; // no annotations to clean
        }

        // going backwards to avoid problems arising from altering a list
        // while iterating through it :)
        for (int i = annots.getChildNodes().getLength() - 1; i >= 0; i--) {
            Node annot = annots.getChildNodes().item(i);
            if (annot.getNodeName().equals("popup")) {
                annots.removeChild(annot);
            }
        }
        String output = null;
        try {
            // I can't believe Java doesn't have an easy way
            // to turn XML into text lol
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(xmlDoc), new StreamResult(writer));
            output = writer.getBuffer().toString().replaceAll("\n|\r", "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return output;

    }
}
