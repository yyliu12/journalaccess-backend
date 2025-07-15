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
        f.setAnnotations(cleanXFDF(xfdfData));
        fileRepository.save(f);
        return "OK";
    }

    @PostMapping("/{id}/get")
    public String getAnnotations(@PathVariable("id") int id) {
        File f = fileRepository.getById(id);
        String annotations = f.getAnnotations();

        return annotations;
    }

    private String cleanXFDF(String doc) {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        Document xmlDoc = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
            xmlDoc = docBuilder.parse(new ByteArrayInputStream(doc.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }

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
