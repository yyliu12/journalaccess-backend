package com.info25.journalindex.apidtos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.info25.journalindex.models.Tag;
import com.info25.journalindex.repositories.TagRepository;

/**
 * Mapper to create TagJsTreeDto
 */
@Component
public class TagJsTreeDtoMapper {
    @Autowired
    TagRepository tagRepository;

    public TagJsTreeDto fromTag(Tag tag) {
        TagJsTreeDto dto = new TagJsTreeDto();
        dto.setId(tag.getId());
        // # is the parent in jsTree
        dto.setParent(tag.getParent() == -1 ? "#" : String.valueOf(tag.getParent()));
        dto.setChildren(tagRepository.hasChildren(tag.getId()));
        dto.setText(tag.getName() + " (" + tag.getFullName() + ")");
        // folder and tag are defined clientside to show the correct icon
        dto.setType(tag.isContainer() ? "folder" : "tag");
        return dto;
    }
}
