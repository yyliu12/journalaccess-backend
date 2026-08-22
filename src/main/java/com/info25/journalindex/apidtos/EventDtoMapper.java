package com.info25.journalindex.apidtos;

import com.info25.journalindex.models.Event;
import org.mapstruct.Mapper;

@Mapper
public interface EventDtoMapper {
    EventDto eventToEventDto(Event event);
}
