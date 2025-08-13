package com.info25.journalindex.repositories;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.info25.journalindex.models.Backlink;
import com.info25.journalindex.models.File;
import com.info25.journalindex.models.File.Location;
import com.info25.journalindex.services.SolrClient;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.FileSolrSerializer;
import com.info25.journalindex.util.FsUtils;
import com.info25.journalindex.util.SolrSelectQuery;

import lombok.Data;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for saving files to Solr and SQL.
 * <p>
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

    @Autowired
    FileSolrSerializer fileSolrSerializer;

    @Autowired
    FsUtils fsUtils;

    /**
     * This function returns a file with SQL and Solr data baesd on id.
     *
     * @param id The id of the file to get.
     * @return The file with SQL and Solr data.
     */
    @Transactional
    public File getById(int id) {
        String sql = "SELECT * FROM files WHERE id = ?";
        File file = jdbcTemplate.queryForObject(sql, new FileRowMapper(), new Object[]{id});
        return file;
    }

    /**
     * This function deletes a tag from all files that have the tag
     *
     * @param tagId The id of the tag to delete from files.
     */
    public void deleteTagFromFiles(int tagId) {
        String sql = "UPDATE files SET tags = array_remove(tags, ?)";
        jdbcTemplate.update(sql, tagId);
    }

    /**
     * This function checks if a file with the given date and path exists.
     *
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
     * This function returns a list of files for a specific date with Solr and SQL
     * data.
     *
     * @param date The date of the files.
     * @return A list of files for the specified date.
     */
    public List<File> getFilesByDate(LocalDate date) {
        String sql = "SELECT * FROM files WHERE date = ?";
        List<File> files = jdbcTemplate.query(sql, new FileRowMapper(), DateUtils.localDateToTimestamp(date));

        return files;
    }

    /**
     * Retrieves the file and its attachments by file id.
     */
    public List<File> getAttachmentsAndFile(int id) {
        List<File> files = new ArrayList<>();
        files.add(getById(id));

        String sql = "SELECT * FROM files WHERE parent = ?";
        List<File> attachments = jdbcTemplate.query(sql, new FileRowMapper(), id);
        files.addAll(attachments);
        return files;
    }

    /**
     * This function performs a Solr query and returns files with both Solr and SQL
     * data.
     * This is designed for API usage as it also includes highlight information and
     * expands
     * tag information.
     *
     * @param query The Solr query to perform.
     * @return A SolrFileResponse containing the files and additional information.
     */
    @Transactional
    public SolrFileResponse solrQueryForApi(SolrSelectQuery query) {
        JsonNode response = solrClient.select(query);
        // Return error
        if (response.has("error")) {
            String errorMessage = response.get("error").get("msg").asText();
            return new SolrFileResponse(0, new ArrayList<>(), errorMessage, null);
        }

        int numFound = response.get("response").get("numFound").asInt();
        ArrayNode docs = (ArrayNode) response.get("response").get("docs");
        JsonNode highlights = response.get("highlighting");
        List<File> files = new ArrayList<File>(numFound);

        for (JsonNode jnDoc : docs) {
            ObjectNode doc = (ObjectNode) jnDoc;
            files.add(this.getById(doc.get("id").asInt()));
        }

        return new SolrFileResponse(numFound, files, null, highlights);
    }

    /**
     * This function saves a file to SQL, Solr, and the filesystem.
     *
     * @param f The file to save.
     */
    public void save(File f) {
        if (f.getUuid() == null) {
            f.setUuid(UUID.randomUUID().toString());
        }
        System.out.println("saving to sql");
        __saveToSql(f);
        System.out.println("Saving to solr");
        __saveToSolr(f);
        __saveToFilesystem(f);
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
            java.io.File fsFile = new java.io.File(fsUtils.getFileByDateAndPath(curDate, f.__getOriginalPath()));
            fsFile.renameTo(new java.io.File(fsUtils.getFileByDateAndPath(curDate, null)));
        }

        // we know that the file exists at its set path now
        if (f.__isDateModified()) {

            java.io.File fsFile = new java.io.File(fsUtils.getFileByDateAndPath(f.__getOriginalDate(), f.getPath()));

            java.io.File toFile = new java.io.File(fsUtils.getFileByDateAndPath(f.getDate(), f.getPath()));
            toFile.getParentFile().mkdirs();
            fsFile.renameTo(toFile);
        }

        f.__savedByRepository();
    }

    public void delete(File f) {
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

    private Connection getConnection() throws SQLException {
        return jdbcTemplate.getDataSource().getConnection();
    }

    private void preparedStatementFromFile(PreparedStatement ps, File f) throws SQLException {
        ps.setString(1, f.getUuid());
        ps.setString(2, f.getPath());
        ps.setLong(3, DateUtils.localDateToTimestamp(f.getDate()));
        ps.setString(4, f.getAnnotation());
        ps.setString(5, f.getContent());
        Connection c = getConnection();

        ps.setArray(6, c.createArrayOf("integer", f.getTags().toArray(new Integer[0])));

        Array coordinatesArray = c.createArrayOf("text", f.getLocations().stream()
                .map(Location::getCoordinate).toArray(String[]::new));
        ps.setArray(7, coordinatesArray);

        Array addressArray = c.createArrayOf("text", f.getLocations().stream()
                .map(Location::getAddress).toArray(String[]::new));
        ps.setArray(8, addressArray);

        Array buildingNameArray = c.createArrayOf("text", f.getLocations().stream()
                .map(Location::getBuildingName).toArray(String[]::new));

        c.close(); // ALWAYS CLOSE - OTHERWISE LEAKS CONNECTIONS
        ps.setArray(9, buildingNameArray);

        ps.setString(10, f.getTitle());
        ps.setString(11, f.getDescription());
        ps.setInt(12, f.getParent());
        ps.setString(13, f.getAttachmentCode());
        // update ps.setInt in id == -1 when adding new statements -- the update
        // sql statement requires the id at the end
    }

    private void __saveToSql(File f) {
        int id = f.getId();
        boolean newEntry = id == -1;

        if (id != -1) {
            String sql = "UPDATE files SET uuid = ?, path = ?, " +
                    "date = ?, annotation = ?, content = ?, tags = ?, " +
                    "location_coordinates = ?, location_address = ?, " +
                    "location_buildingname = ?, title = ?, description = ?, " + 
                    "parent = ?, attachment_code = ? WHERE id = ?";
            jdbcTemplate.update(sql, ps -> {
                preparedStatementFromFile(ps, f);
                ps.setInt(14, f.getId());
            });
        } else {
            String sql = "INSERT INTO files (uuid, path, date, annotation, content," +
                    "tags, location_coordinates, location_address," +
                    "location_buildingname, title, description, parent, attachment_code) VALUES (?, ?, ?, ?," +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            KeyHolder kh = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                preparedStatementFromFile(ps, f);
                return ps;
            }, kh);
            // After insert, we need to get the generated ID

            f.setId(kh.getKey().intValue());
        }
    }

    private void __saveToSolr(File f) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode rootNode = mapper.createArrayNode();
        JsonNode serialization = fileSolrSerializer.serializeForSolrModifyQuery(f);
        rootNode.add(serialization);
        solrClient.modify(rootNode);
    }

    private void __deleteFromSql(File f) {
        String sql = "DELETE FROM files WHERE id = ?";
        jdbcTemplate.update(sql, f.getId());
    }

    private void __deleteFromSolr(File f) {
        String query = "id:" + f.getId();
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
            file.setAnnotation(rs.getString("annotation"));
            file.setContent(rs.getString("content"));
            file.setTitle(rs.getString("title"));
            file.setDescription(rs.getString("description"));
            file.setParent(rs.getInt("parent"));
            file.setAttachmentCode(rs.getString("attachment_code"));
            Array tags = rs.getArray("tags");
            if (tags != null) {
                Integer[] tagIds = (Integer[]) tags.getArray();
                file.setTags(new ArrayList<>(List.of(tagIds)));
            } else {
                file.setTags(new ArrayList<>());
            }
            Array coordinates = rs.getArray("location_coordinates");
            Array addresses = rs.getArray("location_address");
            Array buildingNames = rs.getArray("location_buildingname");

            ArrayList<Location> locations = new ArrayList<>();
            if (coordinates != null && addresses != null && buildingNames != null) {
                String[] coords = (String[]) coordinates.getArray();
                String[] addrs = (String[]) addresses.getArray();
                String[] buildings = (String[]) buildingNames.getArray();

                for (int i = 0; i < coords.length; i++) {
                    Location loc = new Location();
                    loc.setCoordinate(coords[i]);
                    loc.setAddress(addrs[i]);
                    loc.setBuildingName(buildings[i]);
                    locations.add(loc);
                }
            }

            file.setLocations(locations);

            return file;
        }
    }

    @Data
    public static class SolrFileResponse {
        @JsonProperty("numFound")
        int numFound;
        @JsonProperty("files")
        List<File> files;
        @JsonProperty("error")
        String error;
        JsonNode highlights;

        public SolrFileResponse(int numFound, List<File> files, String error, JsonNode highlights) {
            this.numFound = numFound;
            this.files = files;
            this.error = error;
            this.highlights = highlights;
        }
    }

}
