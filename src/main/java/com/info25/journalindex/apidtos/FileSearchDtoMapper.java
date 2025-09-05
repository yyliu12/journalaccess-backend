package com.info25.journalindex.apidtos;

import com.info25.journalindex.models.File;
import com.info25.journalindex.models.Tag;
import com.info25.journalindex.repositories.BacklinkRepository;
import com.info25.journalindex.repositories.EventRepository;
import com.info25.journalindex.repositories.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper class used for creating FileSearchDtos.
 */
@Component
public class FileSearchDtoMapper {
    @Autowired
    TagRepository tagRepository;

    @Autowired
    @Lazy
    BacklinkRepository backlinkRepository;

    @Autowired
    EventRepository eventRepository;

    // creates a FileSearchDto and populates it with the appropriate fields
    public FileSearchDto toDto(File f) {
        FileSearchDto dto = new FileSearchDto();
        dto.setId(f.getId());
        dto.setPath(f.getPath());
        dto.setLocations(f.getLocations());
        dto.setDate(f.getDate());
        dto.setTitle(f.getTitle());
        dto.setDescription(f.getDescription());
        dto.setParent(f.getParent());
        dto.setAttachmentCode(f.getAttachmentCode());

        ArrayList<Tag> tags = new ArrayList<>();
        for (int tagId : f.getTags()) {
            Tag tag = tagRepository.findById(tagId);
            if (tag != null) {
                tags.add(tag);
            }
        }
        dto.setTags(tags);

        backlinkRepository.populateBacklinks(dto);
        eventRepository.populateEventDtos(dto);

        return dto;
    }

    // creates a filesearchdto with highlight data returned from solr
    public FileSearchDto toDtoWithHighlight(File f, String highlight) {
        FileSearchDto dto = toDto(f);
        dto.setHighlight(highlight);
        return dto;
    }

    // bulk action for creating filesearchdtos
    public List<FileSearchDto> toDtoList(List<File> files) {
        List<FileSearchDto> dtos = new ArrayList<>(files.size());
        for (File f : files) {
            dtos.add(toDto(f));
        }
        return dtos;
    }
}
