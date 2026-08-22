package com.info25.journalindex.repositories;

import static com.info25.generated.tables.FileLocations.FILE_LOCATIONS;
import static com.info25.generated.tables.FileTags.FILE_TAGS;
import static com.info25.generated.tables.Files.FILES;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.info25.generated.tables.records.FilesRecord;
import com.info25.journalindex.models.File;
import com.info25.journalindex.models.File.Location;
import com.info25.journalindex.models.JooqFile;
import com.info25.journalindex.models.OOFile;
import com.info25.journalindex.services.SolrClient;
import com.info25.journalindex.util.DateUtils;
import com.info25.journalindex.util.FileSolrSerializer;
import com.info25.journalindex.util.FsUtils;
import com.info25.journalindex.util.SolrSelectQuery;
import com.info25.journalindex.util.SolrUpdateBuffer;

import jakarta.annotation.PostConstruct;
import lombok.Data;

/**
 * This class is responsible for saving files to Solr and SQL.
 * 
 * Complicated. Be careful.
 * 
 * How the database works is like this: All pieces of data are
 * saved to the PostgreSQL database. Whenever the data changes
 * we take a copy of that data and send it to solr. Yes, this
 * duplicates data, but it also prevents complicated data access
 * screnarios when we have to retrieve data from two sources.
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

    @Autowired
    EventFileRepository eventFileRepository;

    @Autowired
    OOFileRepository ooFileRepository;

    @Autowired
    DSLContext dsl;

    public BigDecimal toBd(int i) {
        return new BigDecimal(i);
    }

    public File toClassicFile(JooqFile f) {
        return File.fromJooqFile(f);
    }

    public List<File> toClassicFiles(List<JooqFile> files) {
        return files.stream().map(this::toClassicFile).collect(Collectors.toList());
    }

    Field<List<BigDecimal>> locationIds;

    Field<List<BigDecimal>> tagIds;

    @PostConstruct
    void init() {
        tagIds = DSL.multiset(dsl.select(FILE_TAGS.TAG_ID)
            .from(FILE_TAGS)
            .where(FILES.ID.eq(FILE_TAGS.FILE_ID)))
            .convertFrom(r -> r.map(Record1::value1))
            .as("tagIds");
        
        locationIds = DSL.multiset(dsl.select(FILE_LOCATIONS.LOCATION_ID)
            .from(FILE_LOCATIONS)
            .where(FILES.ID.eq(FILE_LOCATIONS.FILE_ID)))
            .convertFrom(r -> r.map(Record1::value1))
            .as("locationIds");
    }

    /**
     * This function returns a file with SQL and Solr data baesd on id.
     *
     * @param id The id of the file to get.
     * @return The file with SQL and Solr data.
     */
    @Transactional
    public File getById(int id) {
        return toClassicFile(dsl.select(DSL.asterisk(), locationIds, tagIds)
                .from(FILES)
                .where(FILES.ID.eq(toBd(id)))
                .fetchOneInto(JooqFile.class));
    }

    public File getByDateAndPath(LocalDate date, String path) {
        return toClassicFile(dsl.select(DSL.asterisk(), locationIds, tagIds)
                .from(FILES)
                .where(FILES.FILE_DATE.eq(date))
                .and(FILES.PATH.eq(path))
                .fetchOneInto(JooqFile.class));
    }

    /**
     * This function deletes a tag from all files that have the tag
     *
     * @param tagId The id of the tag to delete from files.
     */
    public void deleteTagFromFiles(int tagId) {
        DSL.delete(FILE_TAGS)
                .where(FILE_TAGS.TAG_ID.eq(toBd(tagId)))
                .execute();
    }

    public void deleteLocationFromFiles(int locationId) {
        List<File> files = getFilesByLocation(locationId, null);
        DSL.delete(FILE_LOCATIONS)
                .where(FILE_LOCATIONS.LOCATION_ID.eq(toBd(locationId)))
                .execute();

        SolrUpdateBuffer solrUpdateBuffer = new SolrUpdateBuffer();
        HashMap<Integer, com.info25.journalindex.models.Location> locationCache = new HashMap<>();

        for (File f : files) {
            f.getLocationIds().removeIf(id -> id == locationId);
            solrUpdateBuffer.addToBuffer(
                    fileSolrSerializer.serializeLocationsForSolrModifyQuery(f, locationCache));
        }

        saveSolrBuffer(solrUpdateBuffer);
    }

    public void updateFilesWithLocation(int locationId) {
        List<File> files = getFilesByLocation(locationId, null);

        SolrUpdateBuffer solrUpdateBuffer = new SolrUpdateBuffer();
        HashMap<Integer, com.info25.journalindex.models.Location> locationCache = new HashMap<>();

        for (File f : files) {
            solrUpdateBuffer.addToBuffer(
                    fileSolrSerializer.serializeLocationsForSolrModifyQuery(f, locationCache));
        }

        saveSolrBuffer(solrUpdateBuffer);
    }

    public void deleteLocationFromFile(int locationId, int fileId) {
        DSL.delete(FILE_LOCATIONS)
                .where(FILE_LOCATIONS.LOCATION_ID.eq(toBd(locationId)))
                .and(FILE_LOCATIONS.FILE_ID.eq(toBd(fileId)))
                .execute();

        __saveToSolr(getById(fileId));
    }

    public void deleteJournalIdFromFiles(int journalId) {
        DSL.update(FILES)
                .set(FILES.JOURNAL_ID, toBd(1))
                .where(FILES.JOURNAL_ID.eq(toBd(journalId)))
                .execute();
    }

    /**
     * This function checks if a file with the given date and path exists.
     *
     * @param date The date of the file.
     * @param path The path of the file.
     * @return true if the file exists, false otherwise.
     */
    public boolean existsByDateAndPath(LocalDate date, String path) {
        return dsl.fetchExists(DSL.selectOne()
                .from(FILES)
                .where(FILES.FILE_DATE.eq(date))
                .and(FILES.PATH.eq(path)));
    }

    private <R extends Record> SelectConditionStep<R> applyJournalFilter(SelectConditionStep<R> step, int[] journals) {
        return step.and(journals != null ? FILES.JOURNAL_ID.in(List.of(journals)) : DSL.trueCondition());
    }

    /**
     * This function returns a list of files for a specific date with Solr and SQL
     * data.
     *
     * @param date The date of the files.
     * @return A list of files for the specified date.
     */
    public List<File> getFilesByDate(LocalDate date, int[] journals) {
        return toClassicFiles(applyJournalFilter(dsl.select(DSL.asterisk(), locationIds, tagIds)
                .from(FILES)
                .where(FILES.FILE_DATE.eq(date)), journals)
                .fetchInto(JooqFile.class));
    }

    /**
     * This function retrieves all files within a date range
     * 
     * @param startDate
     * @param endDate
     * @return
     */
    public List<File> getFilesByDateRange(LocalDate startDate, LocalDate endDate, int[] journals) {
        return toClassicFiles(applyJournalFilter(dsl.select(DSL.asterisk(), locationIds, tagIds)
                .from(FILES)
                .where(FILES.FILE_DATE.between(startDate, endDate)), journals)
                .orderBy(FILES.FILE_DATE.asc())
                .fetchInto(JooqFile.class));
    }

    /**
     * Retrieves the file and its attachments by file id.
     */
    public List<File> getAttachmentsAndFile(int id) {
        return toClassicFiles(dsl.select(DSL.asterisk(), locationIds, tagIds)
                .from(FILES)
                .where(FILES.ID.eq(toBd(id)).or(FILES.PARENT.eq(toBd(id))))
                .fetchInto(JooqFile.class));
    }

    public int totalEntries(int[] journals) {
        return dsl.selectCount()
                .from(FILES)
                .where(journals != null ? FILES.JOURNAL_ID.in(List.of(journals)) : DSL.trueCondition())
                .fetchOne(0, int.class);
    }

    public int totalDates(int[] journals) {
        return dsl.select(DSL.countDistinct(FILES.FILE_DATE))
                .from(FILES)
                .where(journals != null ? FILES.JOURNAL_ID.in(List.of(journals)) : DSL.trueCondition())
                .fetchOne(0, int.class);
    }

    public List<File> getFilesByLocation(int locationId, int[] journals) {
        return toClassicFiles(applyJournalFilter(dsl.select(FILES.asterisk(), locationIds, tagIds)
                .from(FILES)
                .join(FILE_LOCATIONS).on(FILES.ID.eq(FILE_LOCATIONS.FILE_ID))
                .where(FILE_LOCATIONS.LOCATION_ID.eq(toBd(locationId))), journals)
                .fetchInto(JooqFile.class));
    }

    public List<File> getFilesByJournal(int journalId) {
        return toClassicFiles(dsl.select(DSL.asterisk(), locationIds, tagIds)
                .from(FILES)
                .where(FILES.JOURNAL_ID.eq(toBd(journalId)))
                .fetchInto(JooqFile.class));
    }

    public boolean existsWithParent(int parentId) {
        return dsl.fetchExists(DSL.selectOne()
                .from(FILES)
                .where(FILES.PARENT.eq(toBd(parentId))));
    }

    public Map<Integer, Map<Integer, Integer>> getFileCountsByYearAndMonth(boolean countFiles, int[] journals) {
        Map<Integer, Map<Integer, Integer>> counts = new HashMap<>();
        String sql;
        Object[] args;
        String count;
        if (countFiles) {
            count = "count(*)";
        } else {
            count = "count(distinct date)";
        }
        if (journals != null) {
            sql = "select TRUNC(FILES.FILE_DATE, 'MM') as ym, " + count
                    + " from files where journal_id = ANY(?) group by ym";
            args = new Object[] { journals };
        } else {
            sql = "select TRUNC(FILES.FILE_DATE, 'MM') as ym, " + count
                    + " from files group by ym";
            args = new Object[0];
        }

        jdbcTemplate.query(sql, args, (ResultSetExtractor<Void>) rs -> {
            while (rs.next()) {
                Timestamp timestamp = rs.getTimestamp(1);
                int num = rs.getInt(2);
                LocalDate date = timestamp.toLocalDateTime().toLocalDate();
                counts.putIfAbsent(date.getYear(), new java.util.HashMap<>());
                counts.get(date.getYear()).put(date.getMonthValue(), num);
            }
            return null;
        });

        return counts;
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
        __saveToSql(f);
        __saveToSolr(f);
        __saveToFilesystem(f);
    }

    public void saveToSolrBuffer(int id, SolrUpdateBuffer buffer) {
        saveToSolrBuffer(getById(id), buffer);
    }

    public void saveToSolrBuffer(File f, SolrUpdateBuffer buffer) {
        JsonNode serialization = fileSolrSerializer.serializeForSolrModifyQuery(f);
        buffer.addToBuffer(serialization);
    }

    public void saveEventsToSolrBuffer(int id, SolrUpdateBuffer buffer) {
        File f = getById(id);
        ObjectNode serialization = fileSolrSerializer.serializeEventsForSolrModifyQuery(f);
        buffer.addToBuffer(serialization);
    }

    public void saveSolrBuffer(SolrUpdateBuffer buffer) {
        ArrayNode rootNode = buffer.getUpdateBuffer();
        solrClient.modify(rootNode);
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
        // move file to its modified path first
        if (f.__isPathModified()) {
            java.io.File fsFile = new java.io.File(fsUtils.getFileByDateAndPath(curDate, f.__getOriginalPath()));
            java.io.File renamedFile = new java.io.File(fsUtils.getFileByDateAndPath(curDate, null));
            renamedFile.getParentFile().mkdirs();
            fsFile.renameTo(renamedFile);
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

    /**
     * Deletes the given file in the database
     * 
     * @param f
     */
    public void delete(File f) {
        __deleteFromSql(f);
        __deleteFromSolr(f);

        // deletion of backlinks
        HashSet<Integer> involvedIn = new HashSet<Integer>();
        involvedIn.addAll(backlinkRepository.findByFrom(f.getId()).stream().map(x -> x.getId()).toList());
        involvedIn.addAll(backlinkRepository.findByFrom(f.getId()).stream().map(x -> x.getId()).toList());

        for (Integer id : involvedIn) {
            backlinkRepository.deleteById(id);
        }

        eventFileRepository.deleteByFile(f.getId());

        // trash ooFile object
        if (f.getOOFileId() != -1) {
            OOFile ooFile = ooFileRepository.findById(f.getOOFileId());
            ooFileRepository.deleteById(ooFile.getId());
            new java.io.File(fsUtils.getOOFilePath(ooFile)).delete();
        }

        if (f.isAsciidoc()) {
            new java.io.File(fsUtils.getAsciidocPath(f)).delete();
        }

        java.io.File osFile = new java.io.File(fsUtils.getFilePathByFile(f));
        osFile.delete();

        // Clear all parent file associations
        dsl.update(FILES)
                .set(FILES.PARENT, toBd(-1))
                .set(FILES.ATTACHMENT_CODE, "")
                .where(FILES.PARENT.eq(toBd(f.getId())))
                .execute();
    }

    /**
     * Gets the connection object of the jdbc database
     * 
     * @return
     * @throws SQLException
     */
    private Connection getConnection() {
        try {
            return jdbcTemplate.getDataSource().getConnection();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Prepares a PreparedStatement object based on a file object
     * 
     * @param ps the preparedstatement to prepare
     * @param f  the file object with which to prepare the preparedstatement
     * @throws SQLException
     */
    private void preparedStatementFromFile(PreparedStatement ps, File f) throws SQLException {
        ps.setString(1, f.getUuid());
        ps.setString(2, f.getPath());
        ps.setLong(3, DateUtils.localDateToTimestamp(f.getDate()));
        ps.setString(4, f.getAnnotation());
        ps.setString(5, f.getContent());
        Connection c = getConnection();

        ps.setArray(6, c.createArrayOf("integer", f.getTags().toArray(new Integer[0])));

        ps.setArray(7, c.createArrayOf("integer", f.getLocationIds().toArray(new Integer[0])));
        /*
         * Array coordinatesArray = c.createArrayOf("text", f.getLocations().stream()
         * .map(Location::getCoordinate).toArray(String[]::new));
         * ps.setArray(7, coordinatesArray);
         * 
         * Array addressArray = c.createArrayOf("text", f.getLocations().stream()
         * .map(Location::getAddress).toArray(String[]::new));
         * ps.setArray(8, addressArray);
         * 
         * Array buildingNameArray = c.createArrayOf("text", f.getLocations().stream()
         * .map(Location::getBuildingName).toArray(String[]::new));
         */

        c.close(); // ALWAYS CLOSE - OTHERWISE LEAKS CONNECTIONS

        ps.setString(8, f.getTitle());
        ps.setString(9, f.getDescription());
        ps.setInt(10, f.getParent());
        ps.setString(11, f.getAttachmentCode());
        ps.setInt(12, f.getJournalId());
        ps.setInt(13, f.getOOFileId());

        ps.setBoolean(14, f.isLegacyOnlineEditorFile());
        ps.setBoolean(15, f.isCKEditorFile());

        ps.setDate(16, f.getWrittenDate() != null ? Date.valueOf(f.getWrittenDate()) : null);

        ps.setBoolean(17, f.isAsciidoc());
        // update ps.setInt in id == -1 when adding new statements -- the update
        // sql statement requires the id at the end
    }

    /**
     * Assembles an SQL commant based on the preparedStatement
     */
    private void __saveToSql(File f) {
        FilesRecord record = dsl.fetchOne(FILES, FILES.ID.eq(toBd(f.getId())));
        record.from(f.toJooqFile());
        record.store();
        f.setId(record.getId().intValue());
    }

    /**
     * Sends a solr update command
     * 
     * @param f the file with which to update in solr
     */
    private void __saveToSolr(File f) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode rootNode = mapper.createArrayNode();
        JsonNode serialization = fileSolrSerializer.serializeForSolrModifyQuery(f);
        rootNode.add(serialization);
        solrClient.modify(rootNode);
    }

    /**
     * Deletes a file from Sql
     * 
     * @param f
     */
    private void __deleteFromSql(File f) {
        dsl.delete(FILES)
                .where(FILES.ID.eq(toBd(f.getId())))
                .execute();
    }

    /**
     * Deletes a file from Solr
     * 
     * @param f
     */
    private void __deleteFromSolr(File f) {
        String query = "id:" + f.getId();
        solrClient.delete(query);
    }

    /**
     * Gets dates where there exists files in given month and year
     * 
     * @param month
     * @param year
     * @return a list of dates
     */
    public List<LocalDate> getDatesWithFilesInJournals(int month, int year, int[] journals) {
        return applyJournalFilter(dsl.selectDistinct(FILES.FILE_DATE)
                .from(FILES)
                .where(FILES.FILE_DATE.between(
                        LocalDate.of(year, month, 1),
                        LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth()))), journals)
                .fetchInto(LocalDate.class);
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
            file.setJournalId(rs.getInt("journal_id"));
            file.setOOFileId(rs.getInt("oo_file_id"));

            file.setLegacyOnlineEditorFile(rs.getBoolean("is_legacy_online_editor_file"));
            file.setCKEditorFile(rs.getBoolean("is_ck_editor_file"));
            file.setAsciidoc(rs.getBoolean("is_asciidoc"));

            Date writtenDate = rs.getDate("written_date");
            if (writtenDate != null) {
                file.setWrittenDate(writtenDate.toLocalDate());
            }

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

            // assemble location objects out of the three array objects stored in SQL
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

            ArrayList<Integer> locationIds = new ArrayList<>();
            Array locationIdsArray = rs.getArray("location_ids");
            if (locationIdsArray != null) {
                Integer[] locIds = (Integer[]) locationIdsArray.getArray();
                locationIds.addAll(List.of(locIds));
            }

            file.setLocations(locations);
            file.setLocationIds(locationIds);

            return file;
        }

        public List<File> extractData(ResultSet rs) throws SQLException {
            List<File> files = new ArrayList<>();

            while (!rs.isAfterLast()) {
                files.add(mapRow(rs, 0));
                rs.next();
            }

            return files;
        }
    }

    // helper class to add the number of files found and if there is a solr error
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
