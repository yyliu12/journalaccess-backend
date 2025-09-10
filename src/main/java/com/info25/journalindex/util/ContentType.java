package com.info25.journalindex.util;

import org.springframework.http.MediaType;

import com.info25.journalindex.models.File;

import java.util.HashMap;
import java.util.Map;

/**
 * ContentType utils
 */
public class ContentType {
    public static Map<String, MediaType> contentTypeMap = new HashMap<>();

    // HTML mediatype to extension associations
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

    /**
     * Gets file extension of a File object
     * @param f
     * @return
     */
    public static String getFileExt(File f) {
        return getFileExt(f.getPath());
    }

    /**
     * Gets file extension based on a file name. Always makes file name lowercase
     * @param fileName
     * @return
     */
    public static String getFileExt(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Gets a MediaType object based on the filename
     */
    public static MediaType getContentTypeFromFileName(String fileName) {
        String ext = getFileExt(fileName);
        return getContentType(ext);
    }

    // Returns the internal Journal Access FileType based on a file name
    public static FileTypes getFileType(File f) {
        switch (ContentType.getFileExt(f.getPath())) {
            case "pdf":
                return FileTypes.PDF;
            case "jpg":
            case "jpeg":
            case "png":
                return FileTypes.IMAGE;
            case "html":
                return FileTypes.WEBPAGE;
        }
        return null; // no file type
    }
}
