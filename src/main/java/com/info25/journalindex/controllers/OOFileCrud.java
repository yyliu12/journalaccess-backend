package com.info25.journalindex.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.info25.journalindex.models.File;
import com.info25.journalindex.models.OOFile;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.repositories.OOFileRepository;
import com.info25.journalindex.services.ConfigService;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.FsUtils;
import com.info25.journalindex.util.ContentType;

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
    Path OO_TEMPLATES_PATH;

    public OOFileCrud(ConfigService configService) {
        OO_RESOURCES_PATH = Path.of(configService.getConfigOption("resourcesFolder"))
                .resolve("oo-resources");
        
        OO_TEMPLATES_PATH = Path.of(configService.getConfigOption("resourcesFolder"))
                .resolve("oo-templates");
    }

    @GetMapping("templates")
    public List<String> getTemplates() {
        return Arrays.asList(OO_TEMPLATES_PATH.toFile().list());
    }

    @PostMapping("createTemplate")
    public String createTemplate(@RequestParam("filename") String fileName,
                         @RequestParam("date") String date,
                         @RequestParam("template") String template) throws IOException {
        fileName = fsUtils.cleanUnusablePathCharacters(fileName);
        LocalDate parsedDate = DateUtils.parseFromString(date);
        return create(fileName, parsedDate, null, template);
    }

    @PostMapping("create")
    public String create(@RequestParam("filename") String fileName,
                         @RequestParam("date") String date,
                         @RequestParam("type") int type) throws IOException {
        fileName = fsUtils.cleanUnusablePathCharacters(fileName);
        LocalDate parsedDate = DateUtils.parseFromString(date);
        return create(fileName, parsedDate, type, null);
    }

    private String create(String fileName, LocalDate parsedDate, Integer type, String template) throws IOException {
        File file = new File();
        file.setDate(parsedDate);
        file.setPath(fileName + ".pdf");

        OOFile ooFile = new OOFile();
        if (type != null)
            ooFile.setType(type);
        else {
            ooFile.setType(OOFile.getTypeForFileExtension(ContentType.getFileExt(template)));
        }


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
        if (type != null)
            Files.copy(getBlankFileForType(type), OOFilePath);
        else if (template != null)
            Files.copy(getTemplateFile(template), OOFilePath);

        return "OK";
    }

    private Path getBlankFileForType(int type) {
        return OO_RESOURCES_PATH.resolve("blank." + OOFile.getFileExtensionForType(type));
    }

    private Path getTemplateFile(String template) {
        return OO_TEMPLATES_PATH.resolve(template);
    }

    private Path getBlankPDF() {
        return OO_RESOURCES_PATH.resolve("blank.pdf");
    }
}
