# Journal Access

Journal Access is an app designed for those who keep a digital journal of their life. It's easy to use and contains a variety of options for organizing your journal to make it easier to access.

## Setting up the backend

First, set up a Solr instance with the provided `managed-schema.xml` in the setup folder and set the configuration options solrUrl and solrCoreName in `/src/main/resouurces/config.xml`, hereby referred to as the config file.

Second, set up a PostgreSQL instance with the provided schema in the setup folder. Enter the correct credentials for the PostgreSQL server into `/src/main/java/com/info25/journalindex/config/JdbcConfig.java`.

Third, create an empty folder on your computer for sorage of journal files. Enter that path into `fsRoot` inthe config file.

Fourth, obtain an API key from TomTom (free plan is acceptable) and place it in `/src/main/resources/config.xml`.

Fifth, (optional) if you are running on MacOS and want to enable the text extraction features for images and PDFs set up an ocrServer (available at yyliu12/ocrserver) and input the url and secret into ocrServerUrl and ocrServerSecret respectively. 

## Running the server

Run `./mvnw spring-boot:start` to start the server. Alternatively, you may choose to deploy the server using Jetty or something similar. The server listens on `localhost:8080`. The username and password may be changed in `/src/main/java/com/info25/journalindex/config/SecurityConfig.java`.

