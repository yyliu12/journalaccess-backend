package com.info25.journalindex.repositories;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.info25.journalindex.models.Tag;

public class CustomTagRepositoryImpl implements CustomTagRepository {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Lazy
    TagRepository tagRepository;

    @Autowired
    @Lazy
    FileRepository fileRepository;

    /**
     * Returns a list of tag data by a list of ids
     */
    @Override
    public List<Tag> findByManyIds(List<Integer> ids) {
        String sql = "SELECT * FROM tags WHERE id IN (" + String.join(",", ids.stream().map(String::valueOf).collect(Collectors.toList())) + ")";

        return jdbcTemplate.query(sql, new TagRowMapper());
    }

    /**
     * Returns tag data when the name string appears in either the name or full_name field
     */
    @Override
    public List<Tag> findByName(String name) {
        // ILIKE = case-insensitive like
        String sql = "SELECT * FROM tags WHERE (name ILIKE ? OR full_name ILIKE ?)";
        String searchPattern = "%" + name + "%";

        return jdbcTemplate.query(sql, new Object[]{searchPattern, searchPattern}, new TagRowMapper());
    }

    /**
     * True or false: whether the tag has children
     */
    @Override
    public boolean hasChildren(int id) {
        String sql = "SELECT COUNT(*) FROM tags WHERE parent = ?";
        Integer count = jdbcTemplate.queryForObject(sql, new Object[]{id}, Integer.class);
        return count != null && count > 0;
    }

    /**
     * Recursively finds tags under a given tag id. 
     */
    @Override
    public List<Tag> findRecursively(int id, boolean includeFolders) {
        String sql = "WITH RECURSIVE tag_tree AS (" +
                     "  SELECT * FROM tags WHERE id = ?" +
                     "  UNION ALL" +
                     "  SELECT t.* FROM tags t" +
                     "  JOIN tag_tree tt ON t.parent = tt.id" +
                     ") SELECT * FROM tag_tree";

        List<Tag> tags = jdbcTemplate.query(sql, new Object[]{id}, new TagRowMapper());

        if (!includeFolders) {
            return tags.stream().filter(tag -> !tag.isContainer()).collect(Collectors.toList());
        }
        return tags;
    }

    public class TagRowMapper implements RowMapper<Tag> {
        @Override
        public Tag mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            Tag tag = new Tag();
            tag.setId(rs.getInt("id"));
            tag.setName(rs.getString("name"));
            tag.setFullName(rs.getString("full_name"));
            tag.setParent(rs.getInt("parent"));
            tag.setContainer(rs.getBoolean("is_folder"));
            return tag;
        }
    }
}
