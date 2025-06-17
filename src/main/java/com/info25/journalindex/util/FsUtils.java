package com.info25.journalindex.util;

import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import com.ibm.icu.text.CharsetDetector;
import com.info25.journalindex.models.File;

public class FsUtils {
    static Path rootPath = Paths.get("C:\\Users\\yuyan\\Desktop\\journalindex\\fs");

    public static String getFileByDateAndPath(LocalDate date, String path) {
        Path p = rootPath.resolve(
                date.getYear() + "\\" + date.getMonthValue() + "\\" + date.getDayOfMonth()
        ).resolve(path.replace("/", "\\"));
        System.out.println(p.toAbsolutePath().toString());
        return p.toAbsolutePath().toString();
    }

    public static String getFilePathByFile(File f) {
        return getFileByDateAndPath(f.getDate(), f.getPath());
    } 

    public static String getFolderByDate(LocalDate date) {
        return rootPath.resolve(
                date.getYear() + "\\" + date.getMonthValue() + "\\" + date.getDayOfMonth()
        ).toAbsolutePath().toString();
    }

    public static String removeFirstFolderFromPath(String path) {
        Path p = Paths.get(path);
        if (p.getNameCount() > 0) {
            return p.subpath(1, p.getNameCount()).toString();
        } else {
            return path;
        }
    }

    public static String decodeBytesWithCharset(byte[] bytes) {
        CharsetDetector detector = new CharsetDetector();
        detector.setText(bytes);
        String charset = detector.detect().getName();

        return new String(bytes, Charset.forName(charset));
    }

    public static String changeToTxtExtension(String path) {
        if (path.endsWith(".txt")) {
            return path;
        }
        if (path.contains(".")) {
            return path.substring(0, path.lastIndexOf('.')) + ".txt";
        } else {
            return path + ".txt";
        }
    }
}
