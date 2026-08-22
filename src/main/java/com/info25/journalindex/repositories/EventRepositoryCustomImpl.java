package com.info25.journalindex.repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.info25.generated.tables.pojos.Events;
import com.info25.journalindex.apidtos.EventDtoMapper;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.info25.journalindex.apidtos.EventDto;
import com.info25.journalindex.apidtos.FileSearchDto;
import com.info25.journalindex.models.Event;
import com.info25.journalindex.models.EventFile;

import static com.info25.generated.tables.Events.EVENTS;

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

    @Autowired
    DSLContext dsl;

    @Autowired
    EventDtoMapper eventDtoMapper;

    // Updates all children in a (soon to be deleted) parent to have a new parent
    // of the original parent's parent
    @Override
    public void moveChildrenToNewParent(int oldParent, int newParent) {
        dsl.update(EVENTS)
                .set(EVENTS.PARENT, new BigDecimal(newParent))
                .where(EVENTS.PARENT.eq(new BigDecimal(oldParent)))
                .execute();
    }

    @Override
    public List<Event> findByManyIds(List<Integer> ids) {
        if (ids.size() == 0) {
            return List.of();
        } else {
            return dsl.select(EVENTS.asterisk())
                    .from(EVENTS)
                    .where(EVENTS.ID.in(ids))
                    .fetchInto(Events.class)
                    .stream()
                    .map(Event::fromJooqEvent)
                    .collect(Collectors.toList());
        }
    }

    // Function to populate the events field of a FileSearchDto object.
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
                        .build();
                f.getEvents().add(eventDto);
            }
        }
    }

    public void populateManyEventDtos(List<FileSearchDto> files) {
        List<EventFile> events = eventFileRepository.findByManyFileIds(files.stream().map(FileSearchDto::getId).toList());
        Map<Integer, Event> eventData = eventRepository.findByManyIds(events.stream().map(EventFile::getEvent).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Event::getId, e -> e));

        for (FileSearchDto fsd : files) {
            fsd.setEvents(events.stream()
                    .filter(x -> x.getFile() == fsd.getId())
                    .map(y -> eventData.get(y.getEvent()))
                    .map(eventDtoMapper::eventToEventDto)
                    .toList());
        }
    }
}
