package com.info25.journalindex.util;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.ibm.icu.text.CharsetDetector;
import com.info25.journalindex.models.File;
import com.info25.journalindex.services.ConfigService;

/**
 * A class with useful filesystem functions
 */
@Component
public class FsUtils {
    Path rootPath;
    String sep = java.io.File.separator;

    public FsUtils(ConfigService configService) {
        rootPath = Paths.get(configService.getConfigOption("fsRoot"));
    }

    /**
     * Gets where a file should be stored based on its date and path
     * @param date
     * @param path
     * @return
     */
    public String getFileByDateAndPath(LocalDate date, String path) {
        Path p = rootPath.resolve(
                date.getYear() + sep + date.getMonthValue() + sep + date.getDayOfMonth())
                .resolve(path.replace("\\", sep));
        System.out.println(p.toAbsolutePath().toString());
        return p.toAbsolutePath().toString();
    }

    /**
     * Gets where a file should be stored
     * @param f
     * @return
     */
    public String getFilePathByFile(File f) {
        return getFileByDateAndPath(f.getDate(), f.getPath());
    }

    /**
     * Gets the folder of files stored in a specific date
     * @param date
     * @return
     */
    public String getFolderByDate(LocalDate date) {
        return rootPath.resolve(
                date.getYear() + sep + date.getMonthValue() + sep + date.getDayOfMonth()).toAbsolutePath().toString();
    }

    /**
     * Removes the last folder/file from a path
     * @param path
     * @return
     */
    public static String removeFirstFolderFromPath(String path) {
        Path p = Paths.get(path);
        if (p.getNameCount() > 0) {
            return p.subpath(1, p.getNameCount()).toString();
        } else {
            return path;
        }
    }

    /**
     * Reads a file to text using ICU's ICU4J library
     * @param bytes
     * @return
     */
    public String decodeBytesWithCharset(byte[] bytes) {
        CharsetDetector detector = new CharsetDetector();
        detector.setText(bytes);
        String charset = detector.detect().getName();

        return new String(bytes, Charset.forName(charset));
    }

    /**
     * Changes the extension to a new new extension
     * @param path the original path
     * @param newExtension the new extension
     * @return the new path
     */
    public String changeExtension(String path, String newExtension) {
        if (path.endsWith(newExtension)) {
            return path;
        }
        if (path.contains(".")) {
            return path.substring(0, path.lastIndexOf('.')) + newExtension;
        } else {
            return path + newExtension;
        }
    }
}
