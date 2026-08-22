package com.info25.journalindex.repositories;

import com.info25.journalindex.apidtos.FileSearchDto;
import com.info25.journalindex.models.Event;
import com.info25.journalindex.models.EventFile;

import java.util.List;

public interface EventRepositoryCustom {
    void moveChildrenToNewParent(int oldParent, int newParent);
    void populateEventDtos(FileSearchDto f);
    List<Event> findByManyIds(List<Integer> ids);
    void populateManyEventDtos(List<FileSearchDto> files);
}
