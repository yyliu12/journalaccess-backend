package com.info25.journalindex.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.info25.journalindex.apidtos.FileSearchDto;
import com.info25.journalindex.models.Backlink;
import com.info25.journalindex.models.File;
import com.info25.journalindex.services.SolrClient;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.FileSolrDeserializer;
import com.info25.journalindex.util.FileSolrSerializer;
import com.info25.journalindex.util.FsUtils;
import com.info25.journalindex.util.SolrSelectQuery;

import lombok.Data;

/**
 * This class is responsible for saving files to Solr and SQL.
 * 
 * Complicated. Be careful.
 */
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

    /**
     * This function returns a file with SQL and Solr data baesd on id.
     * @param id The id of the file to get.
     * @return The file with SQL and Solr data.
     */
    public File getById(int id) {
        String sql = "SELECT * FROM files WHERE id = ?";
        File file = jdbcTemplate.queryForObject(sql, new FileRowMapper(), new Object[] { id });
        loadFromSolr(file);
        return file;
    }

    /**
     * This function returns a file with SQL and Solr data based on uuid.
     * @param uuid The uuid of the file to get.
     * @return The file with SQL and Solr data.
     */
    public File getByUuid(String uuid) {
        String sql = "SELECT * FROM files WHERE uuid = ?";
        File file = jdbcTemplate.queryForObject(sql, new FileRowMapper(), new Object[] { uuid });
        loadFromSolr(file);
        return file;
    }

    /**
     * This function returns a file with SQL data based on id.
     * It does not load data from Solr.
     * @param id The id of the file to get.
     * @return The file with SQL data ONLY.
     */
    public File getWithoutSolr(int id) {
        String sql = "SELECT * FROM files WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new FileRowMapper(), new Object[] { id });
    }

    /**
     * This function returns files with a specific tag with Solr data.
     * @param id the tag id.
     * @return files with the specified tag
     */
    public List<File> getFilesByTag(int id) {
        return this.solrQuery(new SolrSelectQuery()
            .setQ("tags:" + id)
            .setRows(2147483647)
        );
    }

    /**
     * This function deletes a tag from all files that have the tag
     * @param tagId The id of the tag to delete from files.
     */
    public void deleteTagFromFiles(int tagId) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode rootNode = mapper.createArrayNode();
        for (File f : getFilesByTag(tagId)) {
            rootNode.add(
                mapper.createObjectNode()
                    .put("id", f.getUuid())
                    .set("tags", mapper.createObjectNode()
                        .put("remove", tagId))
            );
        }

        solrClient.modify(rootNode);
    }

    /**
     * This function checks if a file with the given date and path exists.
     * @param date The date of the file.
     * @param path The path of the file.
     * @return true if the file exists, false otherwise.
     */
    public boolean existsByDateAndPath(LocalDate date, String path) {
        String sql = "SELECT COUNT(*) FROM files WHERE date = ? AND path = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, DateUtils.localDateToTimestamp(date), path);
        return count > 0;
    }

    /**
     * This function returns a list of files for a specific date with Solr and SQL data.
     * @param date The date of the files.
     * @return A list of files for the specified date.
     */
    public List<FileSearchDto> getFilesByDateForApi(LocalDate date) {
        SolrSelectQuery query = new SolrSelectQuery()
            .setQ("date:" + DateUtils.localDateToTimestamp(date))
            .setRows(2147483647);

        return solrQueryForApi(query).getFiles();
    }

    /**
     * This performs a Solr query and returns the data. Note that the File objects will
     * only contain Solr data.
     * @param query The Solr query to perform.
     * @return A list of files containing Solr data ONLY.
     */
    public List<File> solrQuery(SolrSelectQuery query) {
        JsonNode response = solrClient.select(query).get("response").get("docs");
        List<File> files = FileSolrDeserializer.deserializeMany(response);
        return files;
    }

    /**
     * This function performs a Solr query and returns files with both Solr and SQL data.
     * This is designed for API usage as it also includes highlight information and expands
     * tag information.
     * @param query The Solr query to perform.
     * @return A SolrFileResponse containing the files and additional information.
     */
    public SolrFileResponse solrQueryForApi(SolrSelectQuery query) {
        JsonNode response = solrClient.select(query);
        // Return error
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
			String highlightText = "";
            // if highlights were enabled
			if (highlights != null) {
				JsonNode highlight = highlights.get(file.getUuid());
                // if there actually is a highlight (some solr queries don't return highlights)
				if (highlight != null && highlight.has("content")) {
					highlightText = highlight.get("content").get(0).asText();
				} else {
					highlightText = "";
				}
			} else {
				highlightText = "";
			}
            this.loadFromSql(file);

            // Expand tag info and put into the file object
            FileSearchDto fileSearchDto = FileSearchDto.fromFile(file, highlightText);
            fileSearchDto.setTags(tagRepository.findByManyIds(file.getTags()));
            output.add(fileSearchDto);
        }
        
        return new SolrFileResponse(numFound, output, null);
    }

    /**
     * This function takes a File and modifies it with data from Solr.
     * It requires that the f has the uuid set.
     * @param f The file for which to load data from Solr. 
     */
    public void loadFromSolr(File f) {
        JsonNode response = solrClient.select(new SolrSelectQuery("id:" + f.getUuid()));
        JsonNode doc = response.get("response").get("docs").get(0);

        File fSolr = FileSolrDeserializer.deserializeSingle(doc);

        f.setPath(fSolr.getPath());
        f.setDate(fSolr.getDate());
        f.setLocations(fSolr.getLocations());
        f.setTags(fSolr.getTags());
        f.setContent(fSolr.getContent());
        f.setAnnotationContent(fSolr.getAnnotationContent());
    }

    /**
     * This function loads data from the SQL database into f.
     * It requires that either the id or the uuid of the file is set.
     * @param f The file for which to load data from SQL db.
     */
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

    /**
     * This function saves a file to SQL, Solr, and the filesystem.
     * @param f The file to save.
     */
    public void save(File f) {
        if (f.getUuid() == null) {
            f.setUuid(UUID.randomUUID().toString());
        }
        __saveToSql(f);
        __saveToSolr(f);
        __saveToFilesystem(f);
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

    // if more properties arise then you need to figure out a better
    // solution than doing this... this is OK as of right now
    private void __saveToFilesystem(File f) {
        LocalDate curDate = null;
        if (f.__isDateModified()) {
            curDate = f.__getOriginalDate();
        } else {
            curDate = f.getDate();
        }

        if (f.__isPathModified()) {
            java.io.File fsFile = new java.io.File(FsUtils.getFileByDateAndPath(curDate, f.__getOriginalPath()));
            fsFile.renameTo(new java.io.File(FsUtils.getFileByDateAndPath(curDate, null)));
        }

        // we know that the file exists at its set path now
        if (f.__isDateModified()) {

            java.io.File fsFile = new java.io.File(FsUtils.getFileByDateAndPath(f.__getOriginalDate(), f.getPath()));
            
            java.io.File toFile = new java.io.File(FsUtils.getFileByDateAndPath(f.getDate(), f.getPath()));
            toFile.getParentFile().mkdirs();
            fsFile.renameTo(toFile);
        }

        f.__savedByRepository();
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
