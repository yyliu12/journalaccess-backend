package com.info25.journalindex.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.info25.journalindex.apidtos.TagJsTreeDto;
import com.info25.journalindex.apidtos.TagJsTreeDtoMapper;
import com.info25.journalindex.models.Tag;
import com.info25.journalindex.repositories.TagRepository;

@RestController
public class TagCrud {
    @Autowired
    TagRepository tagRepository;

    @Autowired
    TagJsTreeDtoMapper tagJsTreeDtoMapper;

    @PostMapping("/api/tags/getByIds")
    public List<TagJsTreeDto> getTagsByIds(@RequestParam("tags") JsonNode tags) {
        ArrayList<Integer> tagIds = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++) {
            tagIds.add(tags.get(i).asInt());
        }
        return tagRepository.findByManyIds(tagIds).stream()
                .map(tag -> tagJsTreeDtoMapper.fromTag(tag))
                .collect(Collectors.toList());
    }

    @PostMapping("/api/tags/search")
    public List<TagJsTreeDto> searchTags(@RequestParam("query") String filter) {
        List<Tag> tags = tagRepository.findByName(filter);
        return tags.stream()
                .map(tag -> tagJsTreeDtoMapper.fromTag(tag))
                .collect(Collectors.toList());
    }

    @GetMapping("/api/tags/getByFolder")
    public List<TagJsTreeDto> getTagsByFolder(@RequestParam("id") String folder) {
        int folderId;
        if (folder.equals("#"))
            folderId = -1;
        else
            folderId = Integer.parseInt(folder);

        List<Tag> tagList = tagRepository.findByFolder(folderId);
        return tagList.stream()
                .map(tag -> tagJsTreeDtoMapper.fromTag(tag))
                .collect(Collectors.toList());
    }

    @PostMapping("/api/tags/move")
    public String moveTag(@RequestParam("id") String tagId,
            @RequestParam("parent") String parent) {
        int parentId;
        if (parent.equals("#"))
            parentId = -1;
        else
            parentId = Integer.parseInt(parent);

        Tag tag = tagRepository.findById(Integer.parseInt(tagId));
        tag.setFolder(parentId);
        tagRepository.save(tag);
        return "OK";
    }

    @PostMapping("/api/tags/create")
    public String createTag(@RequestParam("tag") JsonNode tagData) {
        ObjectMapper objectMapper = new ObjectMapper();
        Tag tag = objectMapper.convertValue(tagData, Tag.class);
        tagRepository.save(tag);
        return "OK";
    }

    @PostMapping("/api/tags/get")
    public Tag getTag(@RequestParam("id") int id) {
        return tagRepository.findById(id);
    }

    @PostMapping("/api/tags/save")
    public String saveTag(@RequestParam("tag") JsonNode tagData) {
        ObjectMapper objectMapper = new ObjectMapper();
        Tag tag = objectMapper.convertValue(tagData, Tag.class);
        Tag existingTag = tagRepository.findById(tag.getId());
        existingTag.setName(tag.getName());
        existingTag.setFullName(tag.getFullName());
        existingTag.setContainer(tag.getContainer());
        tagRepository.save(existingTag);

        return "OK";
    }

    @PostMapping("/api/tags/delete")
    public String deleteTag(@RequestParam("id") int id) {
        Tag tag = tagRepository.findById(id);
        if (tag != null) {
            tagRepository.delete(tag);
            return "OK";
        } else {
            return "NOT_FOUND";
        }
    }
}
