package com.info25.journalindex.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.info25.journalindex.apidtos.TagJsTreeDto;
import com.info25.journalindex.apidtos.TagJsTreeDtoMapper;
import com.info25.journalindex.models.Tag;
import com.info25.journalindex.repositories.FileRepository;
import com.info25.journalindex.repositories.TagRepository;

/**
 * Collection of functions that enables editing of tags
 * 
 * when communicating with jsTree, we return TagJsTreeDto objects. Otherwise,
 * when communicating with tag editing features of the client, we return
 * Tag objects.
 */
@RestController
public class TagCrud {
    @Autowired
    TagRepository tagRepository;

    @Autowired
    TagJsTreeDtoMapper tagJsTreeDtoMapper;

    @Autowired
    FileRepository fileRepository;

    /**
     * Get many tags by ids
     * @param tags a list of tag ids
     * @return tag data formatted for jsTree
     */
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

    /**
     * searches for tags by name
     * 
     * this function looks for the specified string in either the name or full name
     * @param filter what to look for
     * @return a list of tag data formatted for jsTree
     */
    @PostMapping("/api/tags/search")
    public List<TagJsTreeDto> searchTags(@RequestParam("query") String filter) {
        List<Tag> tags = tagRepository.findByName(filter);
        return tags.stream()
                .map(tag -> tagJsTreeDtoMapper.fromTag(tag))
                .collect(Collectors.toList());
    }

    /**
     * Gets tags in a specific folder
     * @param folder the id of the tag that serves as a folder -- # represents tags at the top
     * @return a list of tag data, formatted for jsTree
     */
    @GetMapping("/api/tags/getByFolder")
    public List<TagJsTreeDto> getTagsByFolder(@RequestParam("id") String folder) {
        int folderId;
        if (folder.equals("#"))
            folderId = -1;
        else
            folderId = Integer.parseInt(folder);

        List<Tag> tagList = tagRepository.findByParent(folderId);
        return tagList.stream()
                .map(tag -> tagJsTreeDtoMapper.fromTag(tag))
                .collect(Collectors.toList());
    }

    /**
     * Moves a tag
     * @param tagId the tag to move
     * @param parent the id of the new parent of the tag
     * @return OK
     */
    @PostMapping("/api/tags/move")
    public String moveTag(@RequestParam("id") String tagId,
            @RequestParam("parent") String parent) {
        int parentId;
        if (parent.equals("#"))
            parentId = -1;
        else
            parentId = Integer.parseInt(parent);

        Tag tag = tagRepository.findById(Integer.parseInt(tagId));
        tag.setParent(parentId);
        tagRepository.save(tag);
        return "OK";
    }

    /**
     * Creates a tag
     * @param tagData data for the tag, in Tag format as JSON
     * @return OK
     */
    @PostMapping("/api/tags/create")
    public String createTag(@RequestParam("tag") JsonNode tagData) {
        ObjectMapper objectMapper = new ObjectMapper();
        Tag tag = objectMapper.convertValue(tagData, Tag.class);
        tagRepository.save(tag);
        return "OK";
    }

    /**
     * Gets data for a tag
     * @param id the tag id
     * @return the tag's data as a Tag class in JSON
     */
    @PostMapping("/api/tags/get")
    public Tag getTag(@RequestParam("id") int id) {
        return tagRepository.findById(id);
    }

    /**
     * Saves tag data
     * @param tagData the new tag data, as a Tag class in JSON
     * @return OK
     */
    @PostMapping("/api/tags/save")
    public String saveTag(@RequestParam("tag") JsonNode tagData) {
        ObjectMapper objectMapper = new ObjectMapper();
        Tag tag = objectMapper.convertValue(tagData, Tag.class);
        Tag existingTag = tagRepository.findById(tag.getId());
        existingTag.setName(tag.getName());
        existingTag.setFullName(tag.getFullName());
        existingTag.setContainer(tag.isContainer());
        tagRepository.save(existingTag);

        return "OK";
    }

    /**
     * Deletes a tag
     * @param id the id of the tag to delete
     * @return OK
     */
    @PostMapping("/api/tags/delete")
    public String deleteTag(@RequestParam("id") int id) {
        Tag tag = tagRepository.findById(id);
        if (tag != null) {
            fileRepository.deleteTagFromFiles(tag.getId());;
            tagRepository.delete(tag);
            return "OK";
        } else {
            return "NOT_FOUND";
        }
    }
}
