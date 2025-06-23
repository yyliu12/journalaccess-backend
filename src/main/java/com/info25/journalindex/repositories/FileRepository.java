package com.info25.journalindex.repositories;

import java.beans.JavaBean;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.info25.journalindex.util.FileSolrSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.info25.journalindex.models.Backlink;
import com.info25.journalindex.models.File;
import com.info25.journalindex.services.SolrClient;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.FileSolrDeserializer;
import com.info25.journalindex.util.SolrSelectQuery;

import lombok.Data;

import com.info25.journalindex.apidtos.FileSearchDto;

@Component
public class FileRepository {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Lazy
    TagRepository tagRepository;

    @Autowired
    @Lazy
    BacklinkRepository backlinkRepository;

    @Autowired
    SolrClient solrClient;

    public File getById(int id) {
        String sql = "SELECT * FROM files WHERE id = ?";
        File file = jdbcTemplate.queryForObject(sql, new FileRowMapper(), new Object[] { id });
        loadFromSolr(file);
        return file;
    }

    public File getWithoutSolr(int id) {

        String sql = "SELECT * FROM files WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new FileRowMapper(), new Object[] { id });
    }

    public File getByUuid(String uuid) {
        String sql = "SELECT * FROM files WHERE uuid = ?";
        File file = jdbcTemplate.queryForObject(sql, new FileRowMapper(), new Object[] { uuid });
        loadFromSolr(file);
        return file;
    }

    public boolean existsByDateAndPath(LocalDate date, String path) {
        String sql = "SELECT COUNT(*) FROM files WHERE date = ? AND path = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, DateUtils.localDateToTimestamp(date), path);
        return count > 0;
    }

    public List<FileSearchDto> getFilesByDateForApi(LocalDate date) {
        String sql = "SELECT * FROM files WHERE date = ?";
        List<File> files = jdbcTemplate.query(sql, new FileRowMapper(), DateUtils.localDateToTimestamp(date));
        List<FileSearchDto> fileDtos = new ArrayList<>();
        for (File file : files) {
            // loadFromSolr(file);
            fileDtos.add(FileSearchDto.fromFile(file, ""));
        }
        return fileDtos;
    }

    public List<File> solrQuery(SolrSelectQuery query) {
        JsonNode response = solrClient.select(query).get("response").get("docs");
        List<File> files = FileSolrDeserializer.deserializeMany(response);
        return files;
    }

    public SolrFileResponse solrQueryForApi(SolrSelectQuery query) {
        JsonNode response = solrClient.select(query);

        if (response.has("error")) {
            String errorMessage = response.get("error").get("msg").asText();
            return new SolrFileResponse(0, new ArrayList<>(), errorMessage);
        }

        JsonNode docs = response.get("response").get("docs");
        JsonNode highlights = response.get("highlighting");
        List<File> files = FileSolrDeserializer.deserializeMany(docs);
        List<FileSearchDto> output = new ArrayList<>();

        int numFound = response.get("response").get("numFound").asInt();

        for (File file : files) {
            JsonNode highlight = highlights.get(file.getUuid());
            String highlightText = "";
            if (highlight != null && highlight.has("content")) {
                highlightText = highlight.get("content").get(0).asText();
            } else {
                highlightText = "";
            }
            this.loadFromSql(file);

            FileSearchDto fileSearchDto = FileSearchDto.fromFile(file, highlightText);
            fileSearchDto.setTags(tagRepository.findByManyIds(file.getTags()));
            output.add(fileSearchDto);
        }
        
        return new SolrFileResponse(numFound, output, null);
    }

    public void loadFromSolr(File f) {
        JsonNode response = solrClient.select(new SolrSelectQuery("id:" + f.getUuid()));
        JsonNode doc = response.get("response").get("docs").get(0);

        File fSolr = FileSolrDeserializer.deserializeSingle(doc);

        f.setPath(fSolr.getPath());
        f.setDate(fSolr.getDate());
        f.setLocations(fSolr.getLocations());
        f.setTags(fSolr.getTags());
        f.setContent(fSolr.getContent());


    }

    public void loadFromSql(File f) {
        File fSql;
        if (f.getId() != -1) {
            fSql = getById(f.getId());
        } else {
            fSql = getByUuid(f.getUuid());
        }

        f.setPath(fSql.getPath());
        f.setDate(fSql.getDate());
        f.setUuid(fSql.getUuid());
        f.setId(fSql.getId());
        f.setAnnotations(fSql.getAnnotations());
    }

    public void save(File f) {
        if (f.getUuid() == null) {
            f.setUuid(UUID.randomUUID().toString());
        }
        __saveToSql(f);
        __saveToSolr(f);
    }

    // SHOULD BE USED WITH EXTREME CAUTION!
    // Only when you are confident you only modified data that exists
    // in the SQL database ONLY!
    // DON'T USE TO CREATE FILE
    public void saveToSolr(File f) {
        __saveToSolr(f);
    }

    // SHOULD BE USED WITH EXTREME CAUTION!
    // Only when you are confident you only modified data that exists
    // in the SQL database ONLY!
    // DON'T USE TO CREATE FILE
    public void saveToSql(File f) {
        __saveToSql(f);
    }



    public void delete(File f) {
        if (f.getId() == -1) {
            loadFromSql(f);
        }
        __deleteFromSql(f);
        __deleteFromSolr(f);


        // deletion of backlinks
        HashSet<Integer> involvedIn = new HashSet<Integer>();
        for (Backlink b : backlinkRepository.findByFrom(f.getId())) {
            involvedIn.add(b.getTo());
        }
        for (Backlink b : backlinkRepository.findByTo(f.getId())) {
            involvedIn.add(b.getFrom());
        }

        for (Integer id : involvedIn) {
            backlinkRepository.deleteById(id);
        }

    }

    private void __saveToSql(File f) {
        int id = f.getId();
        if (id != -1) {
            String sql = "UPDATE files SET uuid = ?, path = ?, date = ?, annotations = ? WHERE id = ?";
            jdbcTemplate.update(sql, f.getUuid(), f.getPath(), DateUtils.localDateToTimestamp(f.getDate()), f.getAnnotations(), id);
        } else {
            String sql = "INSERT INTO files (uuid, path, date, annotations) VALUES (?, ?, ?, ?) RETURNING RowId";
            id = jdbcTemplate.queryForObject(sql,
                    new Object[] { f.getUuid(), f.getPath(), DateUtils.localDateToTimestamp(f.getDate()), f.getAnnotations() },
                    Integer.class);
            // After insert, we need to get the generated ID

            f.setId(id);
        }
    }

    private void __saveToSolr(File f) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode rootNode = mapper.createArrayNode();
        JsonNode serialization = FileSolrSerializer.serializeForSolrModifyQuery(f);
        rootNode.add(serialization);
        solrClient.modify(rootNode);
    }

    private void __deleteFromSql(File f) {
        String sql = "DELETE FROM files WHERE id = ?";
        jdbcTemplate.update(sql, f.getId());
    }

    private void __deleteFromSolr(File f) {
        String query = "id:" + f.getUuid();
        solrClient.delete(query);
    }

    public List<LocalDate> getDatesWithFiles(int month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        long startDateTimestamp = DateUtils.localDateToTimestamp(startDate);
        long endDateTimestamp = DateUtils.localDateToTimestamp(endDate);

        String sql = "SELECT DISTINCT date FROM files WHERE date BETWEEN ? AND ?";
        List<Long> dates = jdbcTemplate.queryForList(sql, Long.class, startDateTimestamp, endDateTimestamp);
        return dates.stream()
                .map(DateUtils::timestampToLocalDate)
                .collect(Collectors.toList());
    }

    public class FileRowMapper implements RowMapper<File> {
        @Override
        public File mapRow(ResultSet rs, int rowNum) throws SQLException {
            File file = new File();
            file.setId(rs.getInt("id"));
            file.setPath(rs.getString("path"));
            file.setDate(DateUtils.timestampToLocalDate(rs.getInt("date")));
            file.setUuid(rs.getString("uuid"));
            file.setAnnotations(rs.getString("annotations"));
            return file;
        }
    }

    @Data
    public static class SolrFileResponse {
        @JsonProperty("numFound")
        int numFound;
        @JsonProperty("files")
        List<FileSearchDto> files;
        @JsonProperty("error")
        String error;

        public SolrFileResponse(int numFound, List<FileSearchDto> files, String error) {
            this.numFound = numFound;
            this.files = files;
            this.error = error;
        }
    }

}
