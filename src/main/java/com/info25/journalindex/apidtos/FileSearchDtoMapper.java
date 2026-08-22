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
        return toDto(f, null, null, true);
    }

    // creates a FileSearchDto and populates it with the appropriate fields
    public FileSearchDto toDto(File f, HashMap<Integer, Tag> tagCache, HashMap<Integer, Location> locationCache, boolean doExtPopulation) {
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
        dto.setHasAttachment(f.getHasParent() != null ? f.getHasParent() : fileRepository.existsWithParent(f.getId()));

        List<Tag> tags = getTagsByIdsWithCaching(f.getTags(), tagCache);
        dto.setTags(tags);

        if (doExtPopulation) {
            backlinkRepository.populateBacklinks(dto);
            eventRepository.populateEventDtos(dto);
        }

        return dto;
    }

    // creates a filesearchdto with highlight data returned from solr
    public FileSearchDto toDtoWithHighlight(File f, String highlight) {
        FileSearchDto dto = toDto(f, null, null, true);
        dto.setHighlight(highlight);
        return dto;
    }

    // bulk action for creating filesearchdtos
    public List<FileSearchDto> toDtoList(List<File> files) {
        HashMap<Integer, Tag> tagCache = new HashMap<>();
        HashMap<Integer, Location> locationCache = new HashMap<>();
        List<FileSearchDto> dtos = new ArrayList<>(files.size());
        // Warm up tags & location caches
        getTagsByIdsWithCaching(files.stream().flatMap(x -> x.getTags().stream()).distinct().toList(), tagCache);
        getLocationsByIdsWithCaching(files.stream().flatMap(x -> x.getLocationIds().stream()).distinct().toList(), locationCache);
        for (File f : files) {
            dtos.add(toDto(f, tagCache, locationCache, false));
        }

        eventRepository.populateManyEventDtos(dtos);
        backlinkRepository.populateManyBacklinks(dtos);

        return dtos;
    }

    // Retrieves tags with caching
    // pass in cache = null to disable caching
    public List<Tag> getTagsByIdsWithCaching(List<Integer> ids, HashMap<Integer, Tag> cache) {
        List<Tag> tags = new ArrayList<>();
        
        for (int x : ids) {
            if (cache != null && cache.containsKey(x)) {
                tags.add(cache.get(x));
            }
        }

        List<Tag> results = tagRepository.findByManyIds(
            ids.stream()
            .filter(x -> !cache.containsKey(x))
            .distinct()
            .toList()
        );

        for (Tag t : results) {
            cache.put(t.getId(), t);
        }

        tags.addAll(results);

        return tags;
    }

    public List<Location> getLocationsByIdsWithCaching(List<Integer> ids, HashMap<Integer, Location> cache) {
        List<Location> locations = new ArrayList<>();
        
        for (int id : ids) {
            if (cache != null && cache.containsKey(id)) {
                locations.add(cache.get(id));
            }
        }

        List<Location> results = locationRepository.findByManyIds(
            ids.stream()
            .filter(x -> !cache.containsKey(x))
            .distinct()
            .toList()
        );

        for (Location l : results) {
            cache.put(l.getId(), l);
        }

        locations.addAll(results);

        return locations;
    }
}
