package com.info25.journalindex.apidtos;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.info25.journalindex.models.File;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FileModifyDtoMapper {
    File fileModifyDtoToFile(FileModifyDto file);
    FileModifyDto fileToFileModifyDto(File fileModifyDto);
    void updateFileFromDto(FileModifyDto fileModifyDto, @MappingTarget File file);
}
