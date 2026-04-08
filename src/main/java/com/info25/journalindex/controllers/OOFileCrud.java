package com.info25.journalindex.controllers;

import com.info25.journalindex.models.File;
import com.info25.journalindex.models.OOFile;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.repositories.OOFileRepository;
import com.info25.journalindex.services.ConfigService;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.FsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/oofile/")
public class OOFileCrud {
    @Autowired
    FsUtils fsUtils;

    @Autowired
    ConfigService configService;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    OOFileRepository ooFileRepository;

    Path OO_RESOURCES_PATH;

    public OOFileCrud(ConfigService configService) {
        OO_RESOURCES_PATH = Path.of(configService.getConfigOption("resourcesFolder"))
                .resolve("oo-resources");
    }

    @PostMapping("create")
    public String create(@RequestParam("filename") String fileName,
                         @RequestParam("date") String date,
                         @RequestParam("type") int type) throws IOException {
        LocalDate parsedDate = DateUtils.parseFromString(date);
        File file = new File();
        file.setDate(parsedDate);
        file.setPath(fileName + ".pdf");

        OOFile ooFile = new OOFile();
        ooFile.setType(type);


        Path PDFPath = Path.of(fsUtils.getFilePathByFile(file));
        PDFPath.getParent().toFile().mkdirs();


        if (PDFPath.toFile().exists()) {
            return "exists";
        }

        ooFileRepository.save(ooFile);

        file.setOOFileId(ooFile.getId());

        fileRepository.save(file);

        Path OOFilePath = Path.of(fsUtils.getOOFilePath(ooFile));
        OOFilePath.getParent().toFile().mkdirs();



        Files.copy(getBlankPDF(), PDFPath);
        Files.copy(getBlankFileForType(type), OOFilePath);

        return "OK";
    }

    private Path getBlankFileForType(int type) {
        return OO_RESOURCES_PATH.resolve("blank." + OOFile.getFileExtensionForType(type));
    }

    private Path getBlankPDF() {
        return OO_RESOURCES_PATH.resolve("blank.pdf");
    }
}
