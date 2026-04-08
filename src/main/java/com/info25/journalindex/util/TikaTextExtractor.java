package com.info25.journalindex.util;

import java.io.ByteArrayInputStream;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

@Service
public class TikaTextExtractor {
    AutoDetectParser parser = new AutoDetectParser();
    ParseContext context = new ParseContext();

    public TikaTextExtractor() {
        TesseractOCRConfig config = new TesseractOCRConfig();
        config.setSkipOcr(true);
        context.set(TesseractOCRConfig.class, config);
    }

    public String extractText(byte[] fileContent) {
        try {
            BodyContentHandler handler = new BodyContentHandler(-1); // -1 for unlimited text
            parser.parse(new ByteArrayInputStream(fileContent), handler, new Metadata(), context);
            return handler.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return ""; // Return empty string on failure
        }
    }
}
