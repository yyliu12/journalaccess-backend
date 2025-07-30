package com.info25.journalindex.repositories;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.info25.journalindex.apidtos.EventDto;
import com.info25.journalindex.apidtos.FileSearchDto;
import com.info25.journalindex.models.Event;
import com.info25.journalindex.models.EventFile;

@Repository
public class EventRepositoryCustomImpl implements EventRepositoryCustom {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Lazy
    @Autowired
    private EventFileRepository eventFileRepository;

    @Lazy
    @Autowired
    private EventRepository eventRepository;

    @Override
    public int moveChildrenToNewParent(int oldParent, int newParent) {
        String sql = "UPDATE events SET parent = ? WHERE parent = ?";
        return jdbcTemplate.update(sql, newParent, oldParent);
    }

    public void populateEventDtos(FileSearchDto f) {
        List<EventFile> events = eventFileRepository.findByFile(f.getId());
        for (EventFile event : events) {
            Event e = eventRepository.findById(event.getEvent());
            if (e != null) {
                EventDto eventDto = EventDto.builder()
                        .id(e.getId())
                        .name(e.getName())
                        .parent(e.getParent())
                        .description(e.getDescription())
                        .isFolder(e.isFolder())
                        .hasChildren(eventRepository.existsByParent(e.getId()))
                        .build();
                f.getEvents().add(eventDto);
            }
        }
    }
}
