package com.info25.journalindex.util;

import org.springframework.http.MediaType;

import com.info25.journalindex.models.File;

import java.util.HashMap;
import java.util.Map;

public class ContentType {
    public static Map<String, MediaType> contentTypeMap = new HashMap<>();

    static {
        contentTypeMap.put("html", MediaType.TEXT_HTML);
        contentTypeMap.put("pdf", MediaType.APPLICATION_PDF);
        contentTypeMap.put("jpg", MediaType.IMAGE_JPEG);
        contentTypeMap.put("png", MediaType.IMAGE_PNG);
        contentTypeMap.put("jpeg", MediaType.IMAGE_JPEG);
    }

    public static MediaType getContentType(String extension) {
        return contentTypeMap.get(extension.toLowerCase());
    }

    public static String getFileExt(File f) {
        return getFileExt(f.getPath());
    }

    public static String getFileExt(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    public static MediaType getContentTypeFromFileName(String fileName) {
        String ext = getFileExt(fileName);
        return getContentType(ext);
    }
}
