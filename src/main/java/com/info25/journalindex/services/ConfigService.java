package com.info25.journalindex.services;

import java.io.File;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * responsible for reading out config.xml
 */
@Component
public class ConfigService {
    @Autowired
    ApplicationContext applicationContext;

    HashMap<String, String> configValues = new HashMap<>();

    /**
     * Attempts to read config.xml, exits if error
     */
    public ConfigService() {
        try {
            // read config.xml in resources
            File configXml = ResourceUtils.getFile("classpath:config.xml");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(configXml);
            
            Node config = document.getDocumentElement();
            Node next = config.getFirstChild();
            while (next.getNextSibling() != null) {
                next = next.getNextSibling();

                if (next.getNodeType() != Node.ELEMENT_NODE)
                    continue;
                
                configValues.put(next.getNodeName(), next.getTextContent());
            }
        } catch (Exception e) {
            System.out.println("FATAL: Failed to read the config. Error follows:");
            System.out.println(e);
            SpringApplication.exit(applicationContext, () -> 1);
        }
    }

    /**
     * Returns a string for a specific config option as defined in config.xml
     * @param opt what option to return
     * @return the value of the option as defined in config.xml
     */
    public String getConfigOption(String opt) {
        return configValues.get(opt);
    }
}
