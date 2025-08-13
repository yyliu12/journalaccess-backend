package com.info25.journalindex.util;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.ibm.icu.text.CharsetDetector;
import com.info25.journalindex.models.File;
import com.info25.journalindex.services.ConfigService;

@Component
public class FsUtils {
    Path rootPath;
    String sep = java.io.File.separator;

    public FsUtils(ConfigService configService) {
        rootPath = Paths.get(configService.getConfigOption("fsRoot"));
    }

    public String getFileByDateAndPath(LocalDate date, String path) {
        Path p = rootPath.resolve(
                date.getYear() + sep + date.getMonthValue() + sep + date.getDayOfMonth())
                .resolve(path.replace("\\", sep));
        System.out.println(p.toAbsolutePath().toString());
        return p.toAbsolutePath().toString();
    }

    public String getFilePathByFile(File f) {
        return getFileByDateAndPath(f.getDate(), f.getPath());
    }

    public String getFolderByDate(LocalDate date) {
        return rootPath.resolve(
                date.getYear() + sep + date.getMonthValue() + sep + date.getDayOfMonth()).toAbsolutePath().toString();
    }

    public static String removeFirstFolderFromPath(String path) {
        Path p = Paths.get(path);
        if (p.getNameCount() > 0) {
            return p.subpath(1, p.getNameCount()).toString();
        } else {
            return path;
        }
    }

    public String decodeBytesWithCharset(byte[] bytes) {
        CharsetDetector detector = new CharsetDetector();
        detector.setText(bytes);
        String charset = detector.detect().getName();

        return new String(bytes, Charset.forName(charset));
    }

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
