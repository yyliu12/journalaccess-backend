package com.info25.journalindex.repositories;

import com.info25.journalindex.apidtos.FileSearchDto;

public interface EventRepositoryCustom {
    int moveChildrenToNewParent(int oldParent, int newParent);
    void populateEventDtos(FileSearchDto f);
}
