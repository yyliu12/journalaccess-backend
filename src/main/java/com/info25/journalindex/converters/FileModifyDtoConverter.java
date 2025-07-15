package com.info25.journalindex.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.info25.journalindex.apidtos.FileModifyDto;


@Component
public class FileModifyDtoConverter implements Converter<String, FileModifyDto> {
    @Override
    public FileModifyDto convert(String in)  {
        ObjectMapper om = new ObjectMapper().findAndRegisterModules();
        FileModifyDto obj = null;
        try {
            obj = om.readValue(in, FileModifyDto.class);
        } catch (Exception e) {
            System.out.println(e);
        }

        return obj;
        
    }
}
