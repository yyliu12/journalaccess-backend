package com.info25.journalindex.controllers;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import com.info25.journalindex.apidtos.FileModifyDto;
import com.info25.journalindex.apidtos.FileModifyDtoMapper;
import com.info25.journalindex.apidtos.FinalizeUpload;
import com.info25.journalindex.models.File;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.services.OCRServerClient;
import com.info25.journalindex.util.ContentType;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.FsUtils;
import com.info25.journalindex.util.FileTypes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;

@RestController
public class FileCrud {
    @Autowired
    FileModifyDtoMapper fileMapper;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    FsUtils fsUtils;

    @Autowired
    OCRServerClient ocrServerClient;

    @GetMapping("/api/files/getFile/byId/{id}")
    public ResponseEntity<FileSystemResource> getFileById(@PathVariable("id") int id) throws IOException {
        File file = fileRepository.getById(id);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource fsr = new FileSystemResource(fsUtils.getFilePathByFile(file));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(ContentType.getContentTypeFromFileName(file.getPath()));
        headers.setContentLength(fsr.contentLength());
        return new ResponseEntity<>(fsr, headers, HttpStatus.OK);
    }

    @GetMapping("/api/files/getFile/{date}/**")
    public ResponseEntity<FileSystemResource> getFile(@PathVariable("date") String date, HttpServletRequest request)
            throws IOException {
        LocalDate parsedDate = DateUtils.parseFromString(date);
        Object uriObject = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String urlParts = "";
        if (null != uriObject) {
            String uri = uriObject.toString();
            urlParts = uri.substring(uri.indexOf("/" + date + "/") + ("/" + date + "/").length());
        }

        urlParts = URLDecoder.decode(urlParts, "UTF-8");

        FileSystemResource fsr = new FileSystemResource(fsUtils.getFileByDateAndPath(parsedDate, urlParts));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(ContentType.getContentTypeFromFileName(urlParts));
        headers.setContentLength(fsr.contentLength());

        return new ResponseEntity<>(fsr, headers, HttpStatus.OK);
    }

    @PostMapping("/api/files/save")
    public String saveFile(@RequestParam("file") FileModifyDto fileM) {
        File file = fileRepository.getById(fileM.getId());
        fileMapper.updateFileFromDto(fileM, file);
        fileRepository.save(file);
        return "OK";
    }

    @PostMapping("/api/files/get")
    public FileModifyDto getFile(@RequestParam("id") int id) {
        return fileMapper.fileToFileModifyDto(fileRepository.getById(id));
    }

    @PostMapping("/api/files/delete")
    public String deleteFile(@RequestParam("id") int id) {
        File file = fileRepository.getById(id);
        java.io.File osFile = new java.io.File(fsUtils.getFilePathByFile(file));
        if (osFile.exists()) {
            osFile.delete();
        }
        fileRepository.delete(file);
        return "OK";
    }

    @PostMapping("/api/files/finalizeUpload")
    public String finalizeUpload(@RequestBody List<FinalizeUpload> data) {
        for (FinalizeUpload uploadData : data) {
            File file = fileRepository.getById(uploadData.getFile().getId());
            fileMapper.updateFileFromDto(uploadData.getFile(), file);

            if (uploadData.isRunOCR()) {
                switch (ContentType.getFileType(file)) {
                    case FileTypes.IMAGE:
                        file.setContent(ocrServerClient.getTextOfImage(file));
						break;
                    case FileTypes.PDF:
                        file.setContent(ocrServerClient.getTextOfPDF(file, uploadData.isIncludePdfTextLayer()));
						break;
                    case FileTypes.WEBPAGE:
                        try {
                            byte[] fileData = Files.readAllBytes(Paths.get(fsUtils.getFilePathByFile(file)));
                            file.setContent(Jsoup.parse(fsUtils.decodeBytesWithCharset(fileData)).body().wholeText());
                        } catch (Exception e) {
                            System.out.println(e);
                        }
						break;
                }
				
				System.out.println(file.getContent());
            }

            fileRepository.save(file);
        }

        return "OK";
    }

    @PostMapping("/api/files/upload")
    public FileUploadResult uploadFile(@RequestParam("files") MultipartFile[] files,
            @RequestParam("date") String date,
            @RequestParam("type") String type) {
        LocalDate uploadDate = DateUtils.parseFromString(date);
        HashMap<String, MultipartFile> fileMap = new HashMap<>();

        new java.io.File(fsUtils.getFolderByDate(uploadDate)).mkdirs();

        FileUploadResult result = new FileUploadResult();

        for (MultipartFile file : files) {
            String fileName;
            if (type.equals("folders")) {
                fileName = FsUtils.removeFirstFolderFromPath(file.getOriginalFilename().strip());
            } else {
                fileName = file.getOriginalFilename().strip();
            }
            if (fileRepository.existsByDateAndPath(uploadDate, fileName)
                    || new java.io.File(fsUtils.getFileByDateAndPath(uploadDate, fileName)).exists()) {
                result.setOk(false);
                return result;
            }

            fileMap.put(fileName, file);
        }

        // now we are sure we won't be overwriting data

        List<FileModifyDto> fileDtos = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName;
            if (type.equals("folder")) {
                fileName = FsUtils.removeFirstFolderFromPath(file.getOriginalFilename().strip());
            } else {
                fileName = file.getOriginalFilename().strip();
            }

            if (!fileName.endsWith(".pdf") &&
                    !fileName.endsWith(".html") &&
                    !fileName.endsWith(".jpg") &&
                    !fileName.endsWith(".jpeg") &&
                    !fileName.endsWith(".png")) {
                continue;
            }

            fileName = fileName.replace("/", "\\"); // use windows style slashes to keep consistent with existing data

            File newFile = new File();
            newFile.setPath(fileName);
            newFile.setDate(uploadDate);
            newFile.setContent("");

            // attempt to read both a.pdf.txt and a.txt (as an example)
            String[] attemptedContent = new String[3];

            attemptedContent[0] = attemptReadText(fileMap, file.getOriginalFilename() + ".txt");
            attemptedContent[1] = attemptReadText(fileMap, fsUtils.changeExtension(file.getOriginalFilename(), ".txt"));
            attemptedContent[2] = attemptReadText(fileMap, fsUtils.changeExtension(file.getOriginalFilename(), ".TXT"));

            for (String attempt : attemptedContent) {
                if (attempt != null)
                    newFile.setContent(attempt);
            }

            if (fileName.endsWith(".html")) {
                Document doc = null;
                try {
                    doc = Jsoup.parse(fsUtils.decodeBytesWithCharset(file.getBytes()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                newFile.setContent(doc.body().wholeText());
            }

            java.io.File osFile = new java.io.File(fsUtils.getFileByDateAndPath(uploadDate, fileName));

            try {
                osFile.mkdirs();
                file.transferTo(osFile);
            } catch (Exception e) {
                e.printStackTrace();
            }

            fileRepository.save(newFile);
            fileDtos.add(fileMapper.fileToFileModifyDto(newFile));
        }

        result.setFiles(fileDtos);
        result.setOk(true);
        return result;
    }

    private String attemptReadText(HashMap<String, MultipartFile> fileMap, String fileName) {
        String content = null;
        if (fileMap.containsKey(fileName)) {
            try {
                content = fsUtils.decodeBytesWithCharset(fileMap.get(fileName).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return content;
    }

    @Data
    class FileUploadResult {
        private boolean ok;
        private List<FileModifyDto> files;
    }
}
