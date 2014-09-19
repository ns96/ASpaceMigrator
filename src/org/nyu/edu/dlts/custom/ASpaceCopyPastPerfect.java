package org.nyu.edu.dlts.custom;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nyu.edu.dlts.aspace.ASpaceClient;
import org.nyu.edu.dlts.aspace.ASpaceCopy;
import org.nyu.edu.dlts.aspace.ASpaceMapper;
import org.nyu.edu.dlts.models.RowRecord;
import org.nyu.edu.dlts.utils.FileManager;
import org.nyu.edu.dlts.utils.MapperUtil;
import org.nyu.edu.dlts.utils.RowRecordUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple class for testing importing of data from a past perfect xml.
 *
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 7/14/14
 * Time: 2:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class ASpaceCopyPastPerfect extends ASpaceCopy {
    // static field used to store/access records in the db4o database
    public static final String ACCESSION_TYPE = "accession";
    public static final String ARCHIVE_TYPE = "archive";
    public static final String LIBRARY_TYPE = "library";
    public static final String OBJECT_TYPE = "object";
    public static final String PHOTO_TYPE = "photo";
    public static final String SUBJECT_TYPE = "subject";
    public static final String NAME_TYPE = "name";

    // the hashmaps that store people and creators
    private HashMap<String, String> namesMap = new HashMap<String, String>();
    private HashMap<String, String> subjectsMap = new HashMap<String, String>();

    // field used for creation of unique ids
    private int nextId = 0;

    /**
     * The main constructor which just calls the constructor of the parent
     *
     * @param host
     * @param admin
     * @param adminPassword
     */
    public ASpaceCopyPastPerfect(String host, String admin, String adminPassword) {
        super(host, admin, adminPassword);

        // set the global data source to past perfect
        MapperUtil.dataSource = "PastPerfect";
    }

    /**
     * Method to store records read in from an xml file into a db4o database
     *
     * @param xmlFile
     * @param recordType
     */
    public void storeXMLRecords(File xmlFile, String recordType) {
        ArrayList<RowRecord> records = RowRecordUtil.getRowRecordFromXML(xmlFile, recordType, "export");

        // now process each record and extract the names and subjects records
        for(RowRecord record: records) {
            // get the creator and people
            String people = record.get("people");
            if(people != null && !people.isEmpty()) {
                addNames("person", people);
            }

            String creator = record.get("creator");
            if(creator != null && !creator.isEmpty()) {
                addNames("creator", creator);
            }

            // get the subjects
            String subject = record.get("subjects");
            if(subject != null && !subject.isEmpty()) {
                addSubject("subject", subject);
            }

            String subjectTerm = record.get("sterms");
            if(subjectTerm != null && !subjectTerm.isEmpty()) {
                addSubject("subjectTerm", subjectTerm);
            }

            db.store(record);
        }
    }

    /**
     * Method to add names to the hash map
     * @param nameType person, or creator for now
     * @param names
     */
    private void addNames(String nameType, String names) {
        if(names.contains("\n")) {
            String[] sa = names.split("\n");
            for(String name: sa) {
                namesMap.put(name, nameType);
            }
        } else {
            namesMap.put(names, nameType);
        }
    }

    /**
     * Method to add names to the hash map
     * @param subjectType person, or creator for now
     * @param subjects
     */
    private void addSubject(String subjectType, String subjects) {
        if(subjects.contains("\n")) {
            String[] sa = subjects.split("\n");
            for(String subject: sa) {
                subjectsMap.put(subject, subjectType);
            }
        } else {
            subjectsMap.put(subjects, subjectType);
        }
    }

    /**
     * Method to store the names and subjects
     */
    private void storeNamesAndSubjects() {
        RowRecord namesRecord = new RowRecord(NAME_TYPE, "-1", namesMap);
        db.store(namesRecord);

        RowRecord subjectsRecord = new RowRecord(SUBJECT_TYPE, "-1", subjectsMap);
        db.store(subjectsRecord);

        System.out.println("Number of Names: " + namesMap.size());
        System.out.println("Number of Subjects: " + subjectsMap.size());
    }

    /**
     * Method to create repository
     *
     * @throws Exception
     */
    public void createRepository() throws Exception {
        JSONObject repository = mapper.createRepository();
        copyRepositoryRecord(repository);
    }

    /**
     * Method to create a new workbook containing information for creation of agent records
     *
     * @return
     */
    public void copySubjectRecords() throws Exception {
        print("Copying Subject records ...");

        // now iterate through the creator records and create the Agent JSON objects
        List<RowRecord> rowList = getRowList(SUBJECT_TYPE);
        if(rowList.size() == 0) {
            print("No subjects to copy ...");
            return;
        }

        RowRecord subjectRecord = rowList.get(0);
        HashMap<String, String> subjects = subjectRecord.getRecordMap();

        int total = subjects.size();
        int count = 0;
        int success = 0;

        /**
         * Iterate the row data of the spreadsheet
         */
        for (Map.Entry<String, String> entry: subjects.entrySet()) {
            if (stopCopy) return;

            String subject = entry.getKey();
            String subjectType = entry.getValue();
            String recordId = subject + " (" + subjectType + ")";

            JSONObject recordJS = mapper.convertSubject(null, subject, null, recordId);
            String jsonText = recordJS.toString();

            String id = saveRecord(ASpaceClient.SUBJECT_ENDPOINT, jsonText, "Subject->" + recordId);

            if (!id.equalsIgnoreCase(NO_ID)) {
                String uri = ASpaceClient.SUBJECT_ENDPOINT + "/" + id;
                subjectURIMap.put(subject, uri);
                print("Copied Subject: " + recordId + " :: " + id);
                success++;
            } else {
                print("Fail -- Subject: " + recordId);
            }

            count++;
            updateProgress("Subjects", total, count);
        }

        updateRecordTotals("Subjects", total, success);
    }

    /**
     * Method to create a new workbook containing information for creation of agent records
     *
     * @return
     */
    public void copyNameRecords() throws Exception {
        print("Copying Name records ...");

        // now iterate through the creator records and create the Agent JSON objects
        List<RowRecord> rowList = getRowList(NAME_TYPE);
        if(rowList.size() == 0) {
            print("No names to copy ...");
            return;
        }

        RowRecord nameRecord = rowList.get(0);
        HashMap<String, String> names = nameRecord.getRecordMap();

        int total = names.size();
        int count = 0;
        int success = 0;

        /**
         * Iterate the row data of the spreadsheet
         */
        for (Map.Entry<String, String> entry: names.entrySet()) {
            if (stopCopy) return;

            String name = entry.getKey();
            String nameType = entry.getValue();
            String recordId = name + " (" + nameType + ")";

            // get the contact information in the row
            JSONObject recordJS = mapper.convertName(null, name, null, recordId);
            String jsonText = recordJS.toString();

            // based on the type of name copy to the correct location
            String type = recordJS.getString("agent_type");
            String id;
            String uri;

            if (type.equals("agent_person")) {
                id = saveRecord(ASpaceClient.AGENT_PEOPLE_ENDPOINT, jsonText, "Name_Person->" + recordId);
                uri = ASpaceClient.AGENT_PEOPLE_ENDPOINT + "/" + id;
            } else if (type.equals("agent_family")) {
                id = saveRecord(ASpaceClient.AGENT_FAMILY_ENDPOINT, jsonText, "Name_Family->" + recordId);
                uri = ASpaceClient.AGENT_FAMILY_ENDPOINT + "/" + id;
            } else { // must be a corporate name
                id = saveRecord(ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT, jsonText, "Name_Corporate->" + recordId);
                uri = ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT + "/" + id;
            }

            if (!id.equalsIgnoreCase(NO_ID)) {
                nameURIMap.put(name, uri);
                print("Copied Name: " + recordId + " :: " + id);
                success++;
            } else {
                print("Failed -- Name: " + recordId);
            }

            count++;
            updateProgress("Names", total, count);
        }

        updateRecordTotals("Names", total, success);
    }

    /**
     * Method to create a new workbook containing information for creation of agent records
     *
     * @return
     */
    public void copyAccessionRecords() throws Exception {
        print("Copying Accession records ...");

        // load the collection aka resource records
        List<RowRecord> rowList = getRowList(ACCESSION_TYPE);

        int total = rowList.size();
        int count = 0;
        int success = 0;

        for (RowRecord rowRecord : rowList) {
            if (stopCopy) return;

            /* DEBUG */
            //if(count >= 10) return;

            String recordId = rowRecord.get(0);

            JSONObject accessionJS = mapper.convertAccession(null, rowRecord, null, recordId);

            // add the linked agents aka Names records
            addNames(recordId, accessionJS);

            // add an instance that holds the location information
            addLocationInstance(recordId, accessionJS);

            String repoURI = getRepositoryURI();
            String uri = repoURI + ASpaceClient.ACCESSION_ENDPOINT;
            String id = saveRecord(uri, accessionJS.toString(), "Accession->" + recordId);

            if (!id.equalsIgnoreCase(NO_ID)) {
                uri = uri + "/" + id;

                // now add the event objects
                addAccessionEvents(recordId, accessionJS, repoURI, uri);

                accessionURIMap.put(recordId, uri);

                print("Copied Accession: " + recordId + " :: " + id);
                success++;
            } else {
                print("Fail -- Accession: " + recordId);
            }

            count++;
            updateProgress("Accessions", total, count);
        }

        updateRecordTotals("Accessions", total, success);
    }

    /**
     * Method to copy collection records
     *
     * @throws Exception
     */
    private void copyResourceRecords() throws Exception {
        currentRecordType = "Resource Record";

        // get the collection records
        List<RowRecord> rowList = getRowList(ARCHIVE_TYPE);

        // update the progress bar now to indicate the records are being loaded
        updateProgress("Resource Records", 0, 0);

        print("\nCopying " + rowList.size() + " Resource records ...\n");

        int total = rowList.size();
        int count = 0;
        int success = 0;

        // initialize the REST endpoints needed to save records
        String repoURI = getRepositoryURI();
        String endpoint = repoURI + ASpaceClient.RESOURCE_ENDPOINT;
        String aoEndpoint = repoURI + ASpaceClient.ARCHIVAL_OBJECT_ENDPOINT;
        String batchEndpoint = repoURI + ASpaceClient.BATCH_IMPORT_ENDPOINT;

        // iterate over the row data
        for(RowRecord rowRecord : rowList) {
            // check if to stop copy process
            if(stopCopy) {
                updateRecordTotals("Resource Records", total, count);
                return;
            }

            // increment the count
            count++;

            // create the resource title from row Id
            String recordId = rowRecord.getRowId();
            String resourceTitle = "Collection_" + recordId;

            // get the at resource identifier to see if to only copy a specified resource
            // and to use for trouble shooting purposes
            currentRecordIdentifier = "DB ID: " + resourceTitle;

            // set the excel Id in the mapper object
            mapper.setCurrentResourceRecordIdentifier(resourceTitle);

            if (resourceURIMap.containsKey(recordId) && !developerMode) {
                print("Not Copied: Resource already in database " + resourceTitle);
                updateProgress("Resource Records", total, count);
                continue;
            }

            // create the batch import JSON array in case we need it
            JSONArray batchJA = new JSONArray();

            // we need to update the progress bar here
            updateProgress("Resource Records", total, count);

            // indicate we are copying the resource record
            print("Copying Resource: " + resourceTitle);

            // get the main json object
            JSONObject resourceJS = mapper.convertResource(null, rowRecord, null, recordId);

            // add the resource record to batch object
            String resourceURI = endpoint + "/" + recordId;

            resourceJS.put("uri", resourceURI);
            resourceJS.put("jsonmodel_type", "resource");

            // add the subjects
            addSubjects(recordId, resourceJS);

            // add the linked agents aka Names records
            addNames(recordId, resourceJS);

            // add the instances
            addInstances(recordId, resourceJS);

            // add the linked accessions
            addRelatedAccessions(resourceJS.getString("id_0"), resourceJS);

            // add the resource to the batch array now
            batchJA.put(resourceJS);

            // any any series records


            print("Batch Copying Resource # " + count + " || Title: " + resourceTitle);

            String bids = saveRecord(batchEndpoint, batchJA.toString(2), recordId);

            if (!bids.equals(NO_ID)) {
                if (!simulateRESTCalls) {
                    JSONObject bidsJS = new JSONObject(bids);
                    resourceURI = (new JSONArray(bidsJS.getString(resourceURI))).getString(0);
                }

                updateResourceURIMap(recordId, resourceURI);
                success++;

                print("Batch Copied Resource: " + resourceTitle + " :: " + resourceURI);
            } else {
                print("Batch Copy Fail -- Resource: " + resourceTitle);
            }

        }

        // update the number of resource actually copied
        updateRecordTotals("Resource Records", total, success);
    }

    /**
     * Method to clean things up and print out any messages
     */
    public String getMigrationErrors() {
        cleanUp();

        String errorCount = "" + getSaveErrorCount();
        String migrationErrors = getSaveErrorMessages() + "\n\nTotal errors: " + errorCount;

        return migrationErrors;
    }


    /**
     * Method to test the conversion without having to startup the gui
     *
     * @param args
     */
    public static void main(String[] args) throws JSONException {
        String homeDirectory  = System.getProperty("user.home");

        // the db40 database filename
        String databaseFilename = homeDirectory +"/temp/TestData/PPSData/cacheXMLDatabase.db4o";

        File logFile = new File(homeDirectory +"/temp/TestData/PPSData/migrationLog.txt");

        File accessionFile = new File(homeDirectory +"/temp/TestData/PPSData/PPSdata_Accession.xml");
        File archiveFile = new File(homeDirectory +"/temp/TestData/PPSData/PPSdata_Archive.xml");
        File libraryFile = new File(homeDirectory +"/temp/TestData/PPSData/PPSdata_Library.xml");
        File objectFile = new File(homeDirectory +"/temp/TestData/PPSData/PPSdata_Object.xml");
        File photoFile = new File(homeDirectory +"/temp/TestData/PPSData/PPSdata_Photos.xml");

        File bsiMapperScriptFile = new File(homeDirectory + "/temp/TestData/PPSData/mapper.bsh");

        ASpaceCopyPastPerfect aspaceCopy = new ASpaceCopyPastPerfect("http://localhost:8089", "admin", "admin");
        //ASpaceCopyPastPerfect aspaceCopy = new ASpaceCopyPastPerfect("http://54.235.231.8:8089", "admin", "admin");
        //aspaceCopy.setSimulateRESTCalls(true);
        if (!aspaceCopy.getSession()) System.exit(-100);

        try {
            // load the mapper scripts
            String bsiMapperScript = FileManager.readTextData(bsiMapperScriptFile);

            /*
             * Initial the db4o database and add records if needs
             */
            boolean createCache = aspaceCopy.initializeDB4O(databaseFilename);
            //createCache = true;
            if (createCache) {
                System.out.println("Loading Accessions file " + accessionFile);
                aspaceCopy.storeXMLRecords(accessionFile, ACCESSION_TYPE);

                System.out.println("Loading Archive file " + archiveFile);
                aspaceCopy.storeXMLRecords(archiveFile, ARCHIVE_TYPE);

                System.out.println("Loading Library file " + libraryFile);
                aspaceCopy.storeXMLRecords(libraryFile, LIBRARY_TYPE);

                System.out.println("Loading Object file " + objectFile);
                aspaceCopy.storeXMLRecords(objectFile, OBJECT_TYPE);

                System.out.println("Loading Photo file " + photoFile);
                aspaceCopy.storeXMLRecords(photoFile, PHOTO_TYPE);

                aspaceCopy.storeNamesAndSubjects();
            }

            // test the mapper scripts
            System.out.println("Test mapping excel file using BeanShell");

            aspaceCopy.setMapperScriptType(ASpaceMapper.BEANSHELL_SCRIPT);
            aspaceCopy.setMapperScript(bsiMapperScript);

            // first create and copy the repository record
            aspaceCopy.createRepository();

            // copy the subject record
            System.out.println("\n\n");
            aspaceCopy.copySubjectRecords();

            // copy the name records
            System.out.println("\n\n");
            aspaceCopy.copyNameRecords();

            /*
            // copy the accession records
            System.out.println("\n\n");
            //aspaceCopy.copyAccessionRecords();

            // copy the resource records
            System.out.println("\n\n");
            aspaceCopy.copyResourceRecords();

            // save the log file
            String migrationErrors = aspaceCopy.getMigrationErrors();
            FileManager.saveTextData(logFile,migrationErrors);

            System.out.println("\n\nMigration Log ...\n" + migrationErrors); */
        } catch (Exception e) {
            e.printStackTrace();
        }

        // method to close the db4o database
        aspaceCopy.closeDB4O();
    }
}
