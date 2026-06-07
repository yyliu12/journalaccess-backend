package com.info25.journalindex.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;

@Data
@Table("oo_files")
public class OOFile {
    @Id
    int id;
    int type;

    public String getFileExtension() {
        switch (type) {
            case 1:
                return "odt";
            case 2:
                return "odp";
            case 3:
                return "ods";
            case 4:
                return "docx";
            case 5:
                return "pptx";
            case 6:
                return "xlsx";
        }

        throw new IllegalArgumentException("Invalid OO file type: " + type);
    }

    public static String getFileExtensionForType(int type) {
        switch (type) {
            case 1:
                return "odt";
            case 2:
                return "odp";
            case 3:
                return "ods";
            case 4:
                return "docx";
            case 5:
                return "pptx";
            case 6:
                return "xlsx";
        }

        throw new IllegalArgumentException("Invalid OO file type: " + type);
    }

    public boolean isCollabora() {
        return type >= 1 && type <= 3;
    }

    public boolean isOnlyOffice() {
        return type >= 4 && type <= 6;
    }
}
