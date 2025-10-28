package com.info25.journalindex.controllers;

import com.info25.journalindex.models.Journal;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.repositories.JournalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Streamable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/journal/")
public class JournalCrud {
    @Autowired
    JournalRepository journalRepository;

    @Autowired
    FileRepository fileRepository;

    @PostMapping("getAll")
    public List<Journal> getAll() {
        return Streamable.of(journalRepository.findAll()).toList();
    }

    @PostMapping("create")
    public int create(@RequestParam("name") String name, @RequestParam("description") String description) {
        Journal journal = new Journal();
        journal.setName(name);
        journal.setDescription(description);
        journalRepository.save(journal);
		
		return journal.getId();
    }

    @PostMapping("delete")
    public void delete(@RequestParam("id") int id) {
        journalRepository.deleteById(id);
        fileRepository.deleteJournalIdFromFiles(id);
    }

    @PostMapping("update")
    public void update(@RequestParam("id") int id, 
                       @RequestParam("name") String name, 
                       @RequestParam("description") String description) {
        Journal journal = journalRepository.findById(id);
        journal.setName(name);
        journal.setDescription(description);
        journalRepository.save(journal);
    }
}
