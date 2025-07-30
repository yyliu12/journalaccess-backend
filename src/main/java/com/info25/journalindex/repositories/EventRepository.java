package com.info25.journalindex.repositories;


import com.info25.journalindex.models.Event;
import org.springframework.data.repository.CrudRepository;

import java.util.List;



public interface EventRepository extends CrudRepository<Event, Integer>, EventRepositoryCustom {
    Event findById(int id);
    void deleteById(int id);
    List<Event> findByParent(int parent);
    boolean existsByParent(int parent);
}
