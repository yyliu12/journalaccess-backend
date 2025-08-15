package com.info25.journalindex.apidtos;

import lombok.Data;

/**
 * Class to represent data sent by client to finalize upload.
 * 
 * This, for now, contains the actual data that a user has 
 * entered in for the file itself as well as a boolean indicating
 * whether to submit the file for OCR work. 
 */
@Data
public class FinalizeUpload {
    FileModifyDto file;
    boolean runOCR;
    boolean includePdfTextLayer; // option only valid for pdf files
}
