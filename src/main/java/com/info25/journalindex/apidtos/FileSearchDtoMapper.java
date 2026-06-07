package com.info25.journalindex.apidtos;

import com.info25.journalindex.models.File;
import com.info25.journalindex.models.Location;
import com.info25.journalindex.models.Tag;
import com.info25.journalindex.repositories.BacklinkRepository;
import com.info25.journalindex.repositories.EventRepository;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.repositories.LocationRepository;
import com.info25.journalindex.repositories.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Mapper class used for creating FileSearchDtos.
 */
@Component
public class FileSearchDtoMapper {
    @Autowired
    TagRepository tagRepository;

    @Autowired
    BacklinkRepository backlinkRepository;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    FileRepository fileRepository;

    public FileSearchDto toDto(File f) {
        return toDto(f, null, null);
    }

    // creates a FileSearchDto and populates it with the appropriate fields
    public FileSearchDto toDto(File f, HashMap<Integer, Tag> tagCache, HashMap<Integer, Location> locationCache) {
        FileSearchDto dto = new FileSearchDto();
        dto.setId(f.getId());
        dto.setPath(f.getPath());
        dto.setLocations(getLocationsByIdsWithCaching(f.getLocationIds(), locationCache));
        dto.setDate(f.getDate());
        dto.setTitle(f.getTitle());
        dto.setDescription(f.getDescription());
        dto.setParent(f.getParent());
        dto.setAttachmentCode(f.getAttachmentCode());
        dto.setJournalId(f.getJournalId());
        dto.setHasAttachment(fileRepository.existsWithParent(f.getId()));

        List<Tag> tags = getTagsByIdsWithCaching(f.getTags(), tagCache);
        dto.setTags(tags);

        backlinkRepository.populateBacklinks(dto);
        eventRepository.populateEventDtos(dto);

        return dto;
    }

    // creates a filesearchdto with highlight data returned from solr
    public FileSearchDto toDtoWithHighlight(File f, String highlight) {
        FileSearchDto dto = toDto(f, null, null);
        dto.setHighlight(highlight);
        return dto;
    }

    // bulk action for creating filesearchdtos
    public List<FileSearchDto> toDtoList(List<File> files) {
        HashMap<Integer, Tag> tagCache = new HashMap<>();
        HashMap<Integer, Location> locationCache = new HashMap<>();
        List<FileSearchDto> dtos = new ArrayList<>(files.size());
        for (File f : files) {
            dtos.add(toDto(f, tagCache, locationCache));
        }
        return dtos;
    }

    // Retrieves tags with caching
    // pass in cache = null to disable caching
    public List<Tag> getTagsByIdsWithCaching(List<Integer> ids, HashMap<Integer, Tag> cache) {
        List<Tag> tags = new ArrayList<>();
        for (int id : ids) {
            if (cache != null && cache.containsKey(id)) {
                tags.add(cache.get(id));
            } else {
                Tag t = tagRepository.findById(id);
                tags.add(t);
                if (cache != null) {
                    cache.put(id, t);
                }
            }
        }

        return tags;
    }

    public List<Location> getLocationsByIdsWithCaching(List<Integer> ids, HashMap<Integer, Location> cache) {
        List<Location> locations = new ArrayList<>();
        for (int id : ids) {
            if (cache != null && cache.containsKey(id)) {
                locations.add(cache.get(id));
            } else {
                Location l = locationRepository.findById(id);
                locations.add(l);
                if (cache != null) {
                    cache.put(id, l);
                }
            }
        }

        return locations;
    }
}
