package org.nyu.edu.dlts.aspace;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nyu.edu.dlts.dbCopyFrame;
import org.nyu.edu.dlts.models.RelatedRowData;
import org.nyu.edu.dlts.utils.*;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 *
 * User: Nathan Stevens
 *
 * Date: 03/03/14
 * Time: 1:48 PM
 * Utility class for copying data from to Archives Space
 */
public class ASpaceCopy implements PrintConsole {
    // String to indicate when no ids where return from aspace backend
    private final String NO_ID = "no id assigned";

    // the excell workbook object containing all the data
    private XSSFWorkbook workBook = null;

    // used to create the Archive Space JSON data
    private ASpaceMapper mapper;

    // used to make REST calls to archive space backend service
    private ASpaceClient aspaceClient = null;

    // hashmap that maps repository from old database with copy in new database
    private HashMap<String, String> repositoryURIMap = new HashMap<String, String>();

    // hasmap to store the repo agents for use in creating event objects
    private HashMap<String, String> repositoryAgentURIMap = new HashMap<String, String>();

    // hashmap that stores the repository groups from the archive space database
    private HashMap<String, JSONObject> repositoryGroupURIMap = new HashMap<String, JSONObject>();

    // hashmap that maps location from the old database with copy in new database
    private HashMap<String, String> locationURIMap = new HashMap<String, String>();

    // hashmap that maps subjects from old database with copy in new database
    private HashMap<String, String> subjectURIMap = new HashMap<String, String>();

    // hashmap that maps names from old database with copy in new database
    private HashMap<String, String> nameURIMap = new HashMap<String, String>();

    // hashmap that maps accessions from old database with copy in new database
    private HashMap<String, String> accessionURIMap = new HashMap<String, String>();

    // hashmap that maps digital objects from old database with copy in new database
    private HashMap<String, String> digitalObjectURIMap = new HashMap<String, String>();

    // hashmap that stores the converted digital objects so that they can be save to the correct repo
    // when saving the collection content
    private HashMap<String, ArrayList<JSONArray>> digitalObjectMap = new HashMap<String, ArrayList<JSONArray>>();

    // hashmap that maps resource from old database with copy in new database
    private HashMap<String, String> resourceURIMap = new HashMap<String, String>();

    // stop watch object for keeping tract of time
    private StopWatch stopWatch = null;

    // specify debug the boolean
    private boolean debug = true;

    // specify the current record type and ID in case we have fetal error during migration
    private String currentRecordType = "";
    private String currentRecordIdentifier = "";

    // These fields are used to track of the number of messages posted to the output console
    // in order to prevent memory usage errors
    private int messageCount = 0;
    private final int MAX_MESSAGES = 100;

    // keep tract of the number of errors when converting and saving records
    private int saveErrorCount = 0;

    private int aspaceErrorCount = 0;

    // this is used to output text to user when doing the data transfer
    private JTextArea outputConsole;

    // this are used to give user better feedback on progress
    private JProgressBar progressBar;
    private JLabel errorCountLabel;

    // used to specify the stop the copying process. Only get checked when copying resources
    private boolean stopCopy = false;

    // used to specified the the copying process is running
    private boolean copying = false;

    /* Variables used to save URI mapping to file to make data transfer more efficient */

    // file where the uri maps is saved
    private static File uriMapFile = null;

    // keys use to store objects in hash map
    private final String REPOSITORY_KEY = "repositoryURIMap";
    private final String REPOSITORY_AGENT_KEY = "repositoryAgentURIMap";
    private final String LOCATION_KEY = "locationURIMap";
    private final String USER_KEY = "userURIMap";
    private final String SUBJECT_KEY = "subjectURIMap";
    private final String NAME_KEY = "nameURIMap";
    private final String ACCESSION_KEY = "accessionURIMap";
    private final String DIGITAL_OBJECT_KEY = "digitalObjectURIMap";
    private final String RESOURCE_KEY = "resourceURIMap";
    private final String RECORD_TOTAL_KEY = "copyProgress";

    // An Array List for storing the total number of main records transferred
    private ArrayList<String> recordTotals = new ArrayList<String>();

    // Specifies whether or not to simulate the REST calls
    private boolean simulateRESTCalls = false;

    // Specify whether to run in developer mode in which the Names are Subjects records are copied once
    // and the ids for Accessions, Digital Objects, and Resource records are randomized in order to
    // be able to save the same record over and over.
    private boolean developerMode = false;

    // A string builder object to track errors
    private StringBuilder errorBuffer = new StringBuilder();

    // used to set the repository where records should be copied
    private String repositoryURI = "";

    // store information about the ASpace version
    private String aspaceInformation = "";

    /**
     * The main constructor, used when running as a stand alone application
     *
     */
    public ASpaceCopy(String host, String admin, String adminPassword) {
        this.aspaceClient = new ASpaceClient(host, admin, adminPassword);
        init();
    }

    /**
     * Method to set the mapper script
     *
     * @param mapperScript
     */
    public void setMapperScript(String mapperScript) {
        mapper.setMapperScript(mapperScript);
    }

    /**
     * Method to set the mapper script type
     * @param type
     */
    public void setMapperScriptType(String type) {
        mapper.initializeScriptInterpreter(type);
    }

    /**
     * Method to set the excel workbook
     *
     * @param workBook
     */
    public void setWorkbook(XSSFWorkbook workBook) {
        this.workBook = workBook;
    }
    /**
     * Method to initiate certain variables that are needed to work
     */
    private void init() {
        print("Starting record copy ... ");

        // set the error buffer for the mapper
        mapper = new ASpaceMapper(this);

        // set the file that contains the record map
        uriMapFile = new File(System.getProperty("user.home") + File.separator + "aspaceURIMaps.bin");

        // first add the admin repo to the repository URI map
        repositoryURIMap.put("adminRepo", ASpaceClient.ADMIN_REPOSITORY_ENDPOINT);

        // start the stop watch object so we can see how long this data transfer takes
        startWatch();
    }

    /**
     * Method to set the output console
     *
     * @param outputConsole
     */
    public void setOutputConsole(JTextArea outputConsole) {
        this.outputConsole = outputConsole;
    }

    /**
     * Method to set the progress bar
     *
     * @param progressBar
     * @param errorCountLabel
     */
    public void setProgressIndicators(JProgressBar progressBar, JLabel errorCountLabel) {
        this.progressBar = progressBar;
        this.errorCountLabel = errorCountLabel;
    }

    /**
     * Method to copy the repository records
     *
     * @throws Exception
     */
    public String copyRepositoryRecord(JSONObject repository) throws Exception {
        print("Creating repository records ...");

        // update the progress bar to indicate loading of records
        updateProgress("Repositories", 0, 0);

        // these are used to update the progress bar
        int success = 0;

        String shortName = repository.getString("ShortName");
        String repoID = shortName;

        if (!repositoryURIMap.containsKey(repoID)) {
            String jsonText;
            String id;

            // get and save the agent object for the repository
            String agentURI = null;
            jsonText = mapper.getCorporateAgent(repository);
            id = saveRecord(ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT, jsonText, "Repository_Name_Corporate->" + shortName);

            if (!id.equalsIgnoreCase(NO_ID)) {
                agentURI = ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT + "/" + id;
            }

            jsonText = mapper.convertRepository(repository, agentURI);
            id = saveRecord(ASpaceClient.REPOSITORY_ENDPOINT, jsonText, "Repository->" + shortName);

            if (!id.equalsIgnoreCase(NO_ID)) {
                String uri = ASpaceClient.REPOSITORY_ENDPOINT + "/" + id;

                repositoryURIMap.put(repoID, uri);
                repositoryAgentURIMap.put(uri, agentURI);

                success++;

                print("Copied Repository: " + shortName + " :: " + id);
            } else {
                print("Fail -- Repository: " + shortName);
            }
        } else {
            print("Repository already in database " + shortName);
        }

        updateRecordTotals("Repositories", 1, success);

        // return the repo id
        return repositoryURIMap.get(repoID);
    }

    /**
     * Method to copy the location records
     *
     * @throws Exception
     */
    public void copyLocationRecords(int sheetNumber) throws Exception {
        print("Copying Location records ...");

        // load the current spreadsheet from the work book
        XSSFSheet xssfSheet = workBook.getSheetAt(sheetNumber);
        XSSFRow headerRow = null;
        ArrayList<XSSFRow> rowList = getRowData(headerRow, xssfSheet);

        int total = rowList.size();
        int count = 0;
        int success = 0;

        /**
         * Iterate the row data of the spreadsheet
         */
        for (XSSFRow xssfRow : rowList) {
            if (stopCopy) return;
            String recordId = getFullRecordID(xssfSheet, xssfRow);

            JSONObject recordJS = mapper.convertLocation(headerRow, xssfRow);
            String jsonText = recordJS.toString();

            String id = saveRecord(ASpaceClient.LOCATION_ENDPOINT, jsonText, "Location->" + recordId);

            if (!id.equalsIgnoreCase(NO_ID)) {
                String uri = ASpaceClient.LOCATION_ENDPOINT + "/" + id;
                locationURIMap.put(getRecordID(xssfRow), uri);
                print("Copied Location: " + recordId + " :: " + id);
                success++;
            } else {
                print("Fail -- Location: " + recordId);
            }

            count++;
            updateProgress("Locations", total, count);
        }

        updateRecordTotals("Locations", total, success);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to copy the subject records
     *
     * @throws Exception
     */
    public void copySubjectRecords(int sheetNumber) throws Exception {
        print("Copying Subject records ...");

        // load the current spreadsheet from the work book
        XSSFSheet xssfSheet = workBook.getSheetAt(sheetNumber);
        XSSFRow headerRow = null;
        ArrayList<XSSFRow> rowList = getRowData(headerRow, xssfSheet);

        int total = rowList.size();
        int count = 0;
        int success = 0;

        /**
         * Iterate the row data of the spreadsheet
         */
        for (XSSFRow xssfRow : rowList) {
            if (stopCopy) return;
            String recordId = getFullRecordID(xssfSheet, xssfRow);

            JSONObject recordJS = mapper.convertSubject(headerRow, xssfRow);
            String jsonText = recordJS.toString();

            String id = saveRecord(ASpaceClient.SUBJECT_ENDPOINT, jsonText, "Subject->" + recordId);

            if (!id.equalsIgnoreCase(NO_ID)) {
                String uri = ASpaceClient.SUBJECT_ENDPOINT + "/" + id;
                subjectURIMap.put(getRecordID(xssfRow), uri);
                print("Copied Subject: " + recordId + " :: " + id);
                success++;
            } else {
                print("Fail -- Subject: " + recordId);
            }

            count++;
            updateProgress("Subjects", total, count);
        }

        updateRecordTotals("Subjects", total, success);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to copy the name records
     *
     * @throws Exception
     */
    public void copyNameRecords(int sheetNumber) throws Exception {
        print("Copying Name records ...");

        // load the current spreadsheet from the work book
        XSSFSheet xssfSheet = workBook.getSheetAt(sheetNumber);
        XSSFRow headerRow = null;
        ArrayList<XSSFRow> rowList = getRowData(headerRow, xssfSheet);

        int total = rowList.size();
        int count = 0;
        int success = 0;

        /**
         * Iterate the row data of the spreadsheet
         */
        for (XSSFRow xssfRow : rowList) {
            if (stopCopy) return;
            String recordId = getFullRecordID(xssfSheet,xssfRow);

            JSONObject recordJS = mapper.convertName(headerRow, xssfRow);
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
                nameURIMap.put(getRecordID(xssfRow), uri);
                print("Copied Name: " + recordId + " :: " + id);
                success++;
            } else {
                print("Failed -- Name: " + recordId);
            }

            count++;
            updateProgress("Names", total, count);
        }

        updateRecordTotals("Names", total, success);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to copy the accession records
     *
     * @throws Exception
     */
    public void copyAccessionRecords(int sheetNumber) throws Exception {
        print("Copying Accession records ...");

        // load the current spreadsheet from the work book
        XSSFSheet xssfSheet = workBook.getSheetAt(sheetNumber);
        XSSFRow headerRow = null;
        ArrayList<XSSFRow> rowList = getRowData(headerRow, xssfSheet);

        int total = rowList.size();
        int count = 0;
        int success = 0;

        /**
         * Iterate the row data of the spreadsheet
         */
        for (XSSFRow xssfRow : rowList) {
            if (stopCopy) return;
            String recordId = getFullRecordID(xssfSheet, xssfRow);

            JSONObject accessionJS = mapper.convertAccession(headerRow, xssfRow);

            // add the subjects
            addSubjects(recordId, accessionJS);

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

                accessionURIMap.put(getRecordID(xssfRow), uri);
                print("Copied Accession: " + recordId + " :: " + id);
                success++;
            } else {
                print("Fail -- Accession: " + recordId);
            }

            count++;
            updateProgress("Accessions", total, count);
        }

        updateRecordTotals("Accessions", total, success);

        // refresh the interpreter to prevent heap space error
        freeMemory();
    }

    /**
     * Method to add a dummy instance to the accession json object in order to store
     * the location information
     *
     * @param recordJS
     * @throws Exception
     */
    public void addLocationInstance(String recordId, JSONObject recordJS) throws Exception {
        // check to see if there are link subjects
        if(!recordJS.has("location_id") || recordJS.getString("location_id").isEmpty()) {
            return;
        }

        String locationId = recordJS.getString("location_id").replace(".0", "");

        // now add a dummy instance record to store location
        JSONArray instancesJA = new JSONArray();

        String locationURI = locationURIMap.get(locationId);
        if(locationURI != null) {
            String locationNote = "";
            if(recordJS.has("location_note")) {
                locationNote = recordJS.getString("location_note");
            }

            JSONObject instanceJS = MapperUtil.createAccessionInstance(recordId, locationURI, locationNote);
            instancesJA.put(instanceJS);
        }

        recordJS.put("instances", instancesJA);
    }

    /**
     * Method to add events object to an accession object
     *
     * @param recordId
     * @param accessionJS
     * @param accessionURI
     */
    private void addAccessionEvents(String recordId, JSONObject accessionJS, String repoURI, String accessionURI) throws Exception {
        String uri = repoURI + ASpaceClient.EVENT_ENDPOINT;
        String agentURI = repositoryAgentURIMap.get(repoURI);

        ArrayList<JSONObject> eventList = MapperUtil.getAccessionEvents(accessionJS, agentURI, accessionURI);

        for (JSONObject eventJS: eventList) {
            String id = saveRecord(uri, eventJS.toString(), "Accession Event->" + recordId);
        }
    }

    /**
     * Method to copy the digital object records
     *
     * @throws Exception
     */
    public void copyDigitalObjectRecords(int sheetNumber) throws Exception {
        print("Copying Digital Object records ...");

        // update the progress so that the title changes
        updateProgress("Digital Objects", 0, 0);

        // get the digital object records from the specific spreadsheet
        XSSFSheet xssfSheet = workBook.getSheetAt(sheetNumber);
        XSSFRow headerRow = null;
        HashMap<String, RelatedRowData> relatedRowDataMap = getRelatedRowData(headerRow, xssfSheet);

        int total = relatedRowDataMap.size();
        int count = 0;
        int success = 0;

        // iterate over the row data
        for(String key: relatedRowDataMap.keySet()) {
            if (stopCopy) return;

            RelatedRowData relatedRowData = relatedRowDataMap.get(key);
            XSSFRow parentRow = relatedRowData.getParentRow();

            String digitalObjectId = key;
            String digitalObjectTitle = getFullRecordID(xssfSheet, parentRow);

            // create the batch import JSON array and dummy URI now
            JSONArray batchJA = new JSONArray();

            String repoURI = getRepositoryURI();
            String batchEndpoint = repoURI + ASpaceClient.BATCH_IMPORT_ENDPOINT;
            String digitalObjectURI = repoURI + ASpaceClient.DIGITAL_OBJECT_ENDPOINT + "/" + digitalObjectId;

            JSONObject digitalObjectJS = mapper.convertDigitalObject(headerRow, parentRow);

            digitalObjectJS.put("uri", digitalObjectURI);
            digitalObjectJS.put("jsonmodel_type", "digital_object");
            batchJA.put(digitalObjectJS);

            // add the subjects
            addSubjects(digitalObjectId, digitalObjectJS);

            // add the linked agents aka Names records
            addNames(digitalObjectId, digitalObjectJS);

            // add any child archival objects here
            ArrayList<XSSFRow> relatedRowDataList = relatedRowData.getChildRowsList();

            for (XSSFRow childRow : relatedRowDataList) {
                if (stopCopy) return;

                JSONObject digitalObjectChildJS = mapper.convertToDigitalObjectComponent(headerRow, childRow);

                String digitalObjectChildTitle = getFullRecordID(xssfSheet, childRow);

                String cId = getRecordID(childRow);

                String digitalObjectChildURI = repoURI + ASpaceClient.DIGITAL_OBJECT_COMPONENT_ENDPOINT + "/" + cId;

                digitalObjectChildJS.put("uri", digitalObjectChildURI);
                digitalObjectChildJS.put("jsonmodel_type", "digital_object_component");
                digitalObjectChildJS.put("digital_object", MapperUtil.getReferenceObject(digitalObjectURI));

                // add the subjects
                addSubjects(digitalObjectId, digitalObjectJS);

                // add the linked agents aka Names records
                addNames(digitalObjectId, digitalObjectJS);

                batchJA.put(digitalObjectChildJS);

                print("Added Digital Object Component: " + digitalObjectChildTitle + " :: " + cId);
            }

            // check to see we just not saving the digital objects or copying them now
            String bids = saveRecord(batchEndpoint, batchJA.toString(2), digitalObjectId);

            if (!bids.equals(NO_ID)) {
                if (!simulateRESTCalls) {
                    JSONObject bidsJS = new JSONObject(bids);
                    digitalObjectURI = (new JSONArray(bidsJS.getString(digitalObjectURI))).getString(0);
                }

                digitalObjectURIMap.put(digitalObjectId, digitalObjectURI);

                success++;

                print("Batch Copied Digital Object: " + digitalObjectTitle + " :: " + digitalObjectId);
            } else {
                print("Batch Copy Fail -- Digital Object: " + digitalObjectTitle);
            }

            count++;
            updateProgress("Digital Objects", total, count);
        }

        updateRecordTotals("Digital Objects", total, success);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to copy the resource records from the specified sheet numbers
     *
     * @param sheetNumbers
     */
    public void copyResourceRecords(String sheetNumbers) throws Exception {
        XSSFRow headerRow = null;
        HashMap<String, RelatedRowData> relatedRowDataMap = new HashMap<String, RelatedRowData>();

        String[] sa = sheetNumbers.split("\\s*,\\s*");

        for(String ns: sa) {
            try {
                int sheetNumber = Integer.parseInt(ns) - 1;

                XSSFSheet xssfSheet = workBook.getSheetAt(sheetNumber);
                getRelatedRowData(relatedRowDataMap, headerRow, xssfSheet);
            } catch(NumberFormatException nfe) {
                print("Invalid sheet number for resource record");
            }
        }

        // now call the actual method to copy the resource records
        copyResourceRecords(relatedRowDataMap, headerRow);
    }
    /**
     * Method to copy resource records from one database to the next
     *
     * @throws Exception
     */
    public void copyResourceRecords(HashMap<String, RelatedRowData> relatedRowDataMap, XSSFRow headerRow) throws Exception {
        currentRecordType = "Resource Record";

        // update the progress bar now to indicate the records are being loaded
        updateProgress("Resource Records", 0, 0);

        print("\nCopying " + relatedRowDataMap.size() + " Resource records ...\n");

        int total = relatedRowDataMap.size();
        int count = 0;
        int success = 0;

        // initialize the REST endpoints needed to save records
        String repoURI = getRepositoryURI();
        String endpoint = repoURI + ASpaceClient.RESOURCE_ENDPOINT;
        String aoEndpoint = repoURI + ASpaceClient.ARCHIVAL_OBJECT_ENDPOINT;
        String batchEndpoint = repoURI + ASpaceClient.BATCH_IMPORT_ENDPOINT;

        // iterate over the row data
        for(String key: relatedRowDataMap.keySet()) {
            if (stopCopy) return;
            count++;

            // check if to stop copy process
            if(stopCopy) {
                updateRecordTotals("Resource Records", total, count);
                return;
            }

            // get the resource record
            RelatedRowData relatedRowData = relatedRowDataMap.get(key);
            XSSFSheet xssfSheet = relatedRowData.getXssfSheet();
            XSSFRow parentRow = relatedRowData.getParentRow();

            // get the resource title
            String resourceTitle = getFullRecordID(xssfSheet, parentRow);

            // get the record id
            String dbId = key;

            // get the at resource identifier to see if to only copy a specified resource
            // and to use for trouble shooting purposes
            currentRecordIdentifier = "DB ID: " + resourceTitle;

            // set the excel Id in the mapper object
            mapper.setCurrentResourceRecordIdentifier(resourceTitle);

            if (resourceURIMap.containsKey(dbId) && !developerMode) {
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
            JSONObject resourceJS = mapper.convertResource(headerRow, parentRow);

            // add the resource record to batch object
            String resourceURI = endpoint + "/" + dbId;

            resourceJS.put("uri", resourceURI);
            resourceJS.put("jsonmodel_type", "resource");

            // add the subjects
            addSubjects(dbId, resourceJS);

            // add the linked agents aka Names records
            addNames(dbId, resourceJS);

            // add the instances
            addInstances(dbId, resourceJS);

            // add the linked accessions
            addRelatedAccessions(dbId, resourceJS);

            // add the resource to the batch array now
            batchJA.put(resourceJS);

            // add the resource components
            ArrayList<XSSFRow> relatedRowDataList = relatedRowData.getChildRowsList();

            for (XSSFRow childRow : relatedRowDataList) {
                if (stopCopy) return;

                String componentTitle = getFullRecordID(xssfSheet, childRow);
                String cId = getRecordID(childRow);

                JSONObject componentJS = mapper.convertResourceComponent(headerRow, childRow);

                componentJS.put("uri", aoEndpoint + "/" + cId);
                componentJS.put("jsonmodel_type", "archival_object");
                componentJS.put("resource", MapperUtil.getReferenceObject(resourceURI));

                String parentURI = getParentURI(dbId, aoEndpoint, childRow);
                if(parentURI != null) {
                    componentJS.put("parent", MapperUtil.getReferenceObject(parentURI));
                }

                // add the subjects
                addSubjects(cId, componentJS);

                // add the linked agents aka Names records
                addNames(cId, componentJS);

                // add the instances
                addInstances(cId, componentJS);

                // add the component to batch JA now
                batchJA.put(componentJS);

                print("Copied Resource Component: " + componentTitle + " :: " + cId + "\n");
            }

            print("Batch Copying Resource # " + count + " || Title: " + resourceTitle);

            String bids = saveRecord(batchEndpoint, batchJA.toString(2), dbId);

            if (!bids.equals(NO_ID)) {
                if (!simulateRESTCalls) {
                    JSONObject bidsJS = new JSONObject(bids);
                    resourceURI = (new JSONArray(bidsJS.getString(resourceURI))).getString(0);
                }

                updateResourceURIMap(dbId, resourceURI);
                success++;

                print("Batch Copied Resource: " + resourceTitle + " :: " + resourceURI);
            } else {
                print("Batch Copy Fail -- Resource: " + resourceTitle);
            }

        }

        // free some memory
        freeMemory();

        // update the number of resource actually copied
        updateRecordTotals("Resource Records", total, success);
    }

    /**
     * Add the subjects to the json resource, or resource component record
     *
     * @param recordJS   The json representation of the AT record
     * @throws Exception
     */
    private void addSubjects(String recordId, JSONObject recordJS) throws Exception {
        // check to see if there are link subjects
        if(!recordJS.has("linked_subjects") || recordJS.getString("linked_subjects").isEmpty()) {
            return;
        }

        String[] subjectLinks = recordJS.getString("linked_subjects").split("\\s*,\\s*");
        JSONArray subjectsJA = new JSONArray();

        for (String subjectId : subjectLinks) {
            String subjectURI = subjectURIMap.get(subjectId);

            if (subjectURI != null) {
                subjectsJA.put(MapperUtil.getReferenceObject(subjectURI));

                if (debug) print("Added subject to " + recordId);
            } else {
                print("No mapped subject found ...");
            }
        }

        // if we had any subjects add them parent json record
        if (subjectsJA.length() != 0) {
            recordJS.put("subjects", subjectsJA);
        }
    }

    /**
     * Add the names to the ASpace record
     *
     * @param recordId
     * @param recordJS
     * @throws Exception
     */
    private void addNames(String recordId, JSONObject recordJS) throws Exception {
        // check to see if there are link names
        if(!recordJS.has("linked_names") || recordJS.getString("linked_names").isEmpty()) {
            return;
        }

        String[] nameLinks = recordJS.getString("linked_names").split("\\s*;\\s*");
        JSONArray linkedAgentsJA = new JSONArray();

        for (String nameLink : nameLinks) {
            String[] linkInfo = nameLink.split("\\s*,\\s*");

            String nameURI = nameURIMap.get(linkInfo[0]);

            if(nameURI != null) {
                JSONObject linkedAgentJS = new JSONObject();

                linkedAgentJS.put("role", linkInfo[1]);

                if(linkInfo.length == 3) {
                    linkedAgentJS.put("relator", linkInfo[2]);
                }

                linkedAgentJS.put("ref", nameURI);
                linkedAgentsJA.put(linkedAgentJS);

                if (debug) print("Added name to " + recordId);
            } else {
                print("No mapped name found ...");
            }
        }

        // if we had any subjects add them parent json record
        if (linkedAgentsJA.length() != 0) {
            recordJS.put("linked_agents", linkedAgentsJA);
        }
    }

    /**
     * Method to add an instance to resource, or resource component record
     *
     * @param recordJS
     * @param recordId The title or id of the record
     * @throws Exception
     */
    private void addInstances(String recordId, JSONObject recordJS) throws Exception {
        // array to hold the instances
        JSONArray instancesJA = new JSONArray();

        if (recordJS.has("analog_instances") && !recordJS.getString("analog_instances").isEmpty()) {
            // first split along lines
            String[] sa = recordJS.getString("analog_instances").split("\n");

            for (String instances : sa) {
                String locationURI = null;

                // get the information for the instance record
                String instanceType = "";
                String barcode = "";

                String[] info1 = instances.split("\\s*:\\s*");
                if (info1[0].contains("-")) {
                    String[] info2 = info1[0].split("\\s*-\\s*");
                    instanceType = info2[0];

                    if(!info2[1].equals("0")) {
                        barcode = info2[1];
                    }

                    // we have location information so load it
                    if(info2.length == 3) {
                        locationURI = locationURIMap.get(info2[2]);
                    }
                } else {
                    instanceType = info1[0];
                }

                // get the container information now. This can be done in loop but this is more readable
                String[] info3 = info1[1].split("\\s*,\\s*");

                String[] container = info3[0].split("\\s*-\\s*");
                String type1 = container[0];
                String indicator1 = container[1];

                String type2 = "";
                String indicator2 = "";
                if (info3.length == 2) {
                    container = info3[1].split("\\s*-\\s*");
                    type2 = container[0];
                    indicator2 = container[1];
                }

                String type3 = "";
                String indicator3 = "";
                if (info3.length == 3) {
                    container = info3[2].split("\\s*-\\s*");
                    type3 = container[0];
                    indicator3 = container[1];
                }

                // create the instance object now
                JSONObject instanceJS = MapperUtil.createAnalogInstance(instanceType, barcode,
                        type1, indicator1,
                        type2, indicator2,
                        type3, indicator3, locationURI);

                instancesJA.put(instanceJS);
                if (debug) print("Added analog instance to " + recordId);
            }
        } else if (recordJS.has("digital_instances") && !recordJS.getString("digital_instances").isEmpty()) {
            // first split along lines
            String[] sa = recordJS.getString("digital_instances").split("\\s*,\\s*");

            for (String key : sa) {
                String digitalObjectURI = digitalObjectURIMap.get(key);

                if(digitalObjectURI != null) {
                    JSONObject instanceJS = MapperUtil.createDigitalInstance(digitalObjectURI);
                    instancesJA.put(instanceJS);
                }
            }
        }

        if (instancesJA.length() != 0) {
            recordJS.put("instances", instancesJA);
        }
    }

    /**
     * Method to add a related accessions to a resource record
     *
     * @param recordId
     * @param recordJS
     */
    private void addRelatedAccessions(String recordId, JSONObject recordJS) throws Exception {
        // check to see if there are link names
        if(!recordJS.has("linked_accessions") || recordJS.getString("linked_accessions").isEmpty()) {
            return;
        }

        JSONArray accessionsJA = new JSONArray();
        String[] sa = recordJS.getString("linked_accessions").split("\\s*,\\s*");

        for(String accessionId: sa) {
            String accessionURI = accessionURIMap.get(accessionId);

            if(accessionURI != null) {
                accessionsJA.put(MapperUtil.getReferenceObject(accessionURI));
                if (debug) print("Added Accession to Resource: " + recordId);
            } else {
                String message = "Linked Accession Not Found: " + accessionId + "\n";
                addErrorMessage(message);
            }
        }

        if (accessionsJA.length() != 0) {
            recordJS.put("related_accessions", accessionsJA);
        }
    }

    /**
     * Method to get the parent URI
     *
     * @param topLevelId
     * @param endpoint
     * @param childRow
     * @return
     */
    private String getParentURI(String topLevelId, String endpoint, XSSFRow childRow) {
        XSSFCell cell = childRow.getCell(1);

        if(cell != null && !cell.toString().isEmpty()) {
            String parentId = cell.toString().replace(".0", "");
            if(!parentId.equals(topLevelId)) {
                return endpoint + "/" + parentId;
            }
        }

        return null;
    }

    /**
     * Method to load row data from a sheet which have an ID for the row
     *
     * @param xssfSheet
     * @return
     */
    public ArrayList<XSSFRow> getRowData(XSSFRow headerRow, XSSFSheet xssfSheet) {
        ArrayList<XSSFRow> rowList = new ArrayList<XSSFRow>();

        Iterator rowIterator = xssfSheet.rowIterator();
        int rowNumber = 1;

        while (rowIterator.hasNext()) {
            if (stopCopy) return null;

            XSSFRow xssfRow = (XSSFRow) rowIterator.next();

            // skip the first row and any empty rows
            if (rowNumber != 1) {
                XSSFCell cell = xssfRow.getCell(0);

                if (cell != null && !cell.toString().isEmpty()) {
                    rowList.add(xssfRow);
                }
            } else {
                headerRow = xssfRow;
            }

            rowNumber++;
        }

        return rowList;
    }

    /**
     * Method to load related data from a related row data from a given sheet
     *
     * @param headerRow
     * @param xssfSheet
     * @return
     */
    public HashMap<String, RelatedRowData> getRelatedRowData(XSSFRow headerRow, XSSFSheet xssfSheet) {
        HashMap<String, RelatedRowData> relatedRowDataMap = new HashMap<String, RelatedRowData>();
        return getRelatedRowData(relatedRowDataMap, headerRow, xssfSheet);
    }


    /**
     * Method to load related row data from a sheet which have an ID for the row
     *
     * @param xssfSheet
     * @return
     */
    public HashMap<String, RelatedRowData> getRelatedRowData(HashMap<String, RelatedRowData> relatedRowDataMap, XSSFRow headerRow, XSSFSheet xssfSheet) {
        Iterator rowIterator = xssfSheet.rowIterator();
        int rowNumber = 1;

        // stores the parent row along with it children
        RelatedRowData relatedRowData = null;

        while (rowIterator.hasNext()) {
            if (stopCopy) return null;

            XSSFRow xssfRow = (XSSFRow) rowIterator.next();

            // skip the first row and any rows without an id
            if (rowNumber != 1) {
                XSSFCell idCell = xssfRow.getCell(0);
                XSSFCell pidCell = xssfRow.getCell(1);

                if (idCell != null && !idCell.toString().isEmpty()) {
                    String idString = idCell.toString().replace(".0", "");
                    String pidString = pidCell.toString().replace(".0", "");

                    // we have a parent row
                    if(pidString.equals("0")) {
                        relatedRowData = new RelatedRowData(idString, xssfSheet, xssfRow);
                        relatedRowDataMap.put(idString, relatedRowData);
                    } else {
                        relatedRowData.addChildRow(xssfRow);
                    }
                }
            } else {
                headerRow = xssfRow;
            }

            rowNumber++;
        }

        return relatedRowDataMap;
    }

    /**
     * Function to get the record ID so it can be easily identified in a spreadsheet
     *
     * @param xssfSheet
     * @param xssfRow
     * @return
     */
    public String getFullRecordID(XSSFSheet xssfSheet, XSSFRow xssfRow) {
        String recordId = xssfSheet.getSheetName() + "_" + getRecordID(xssfRow);
        return recordId.toUpperCase();
    }

    /**
     * Method to return the record id
     *
     * @param xssfRow
     * @return
     */
    public String getRecordID(XSSFRow xssfRow) {
        String idString = xssfRow.getCell(0).toString().replace(".0", "");
        return idString;
    }

    /**
     * Method to start the start the time watch
     */
    private void startWatch() {
        stopWatch = new StopWatch();
        stopWatch.start();
    }

    private String stopWatch() {
        stopWatch.stop();
        return stopWatch.getPrettyTime();
    }

    /**
     * Method to return the status of getting the session needed to create certain records
     *
     * @return
     */
    public boolean getSession() {
        boolean connected = aspaceClient.getSession();

        if(connected) {
            aspaceInformation = aspaceClient.getArchivesSpaceInformation();
        }

        return connected;
    }

    /** Method to add to resource map in a thread safe manner
     *
     * @param oldIdentifier
     * @param uri
     */
    private void updateResourceURIMap(String oldIdentifier, String uri) {
        resourceURIMap.put(oldIdentifier, uri);
        saveURIMaps();
    }

    /**
     * Method to return the new repository for a given domain object.
     *
     * @return The URI of the new repository
     */
    public String getRepositoryURI() {
        if(!repositoryURI.isEmpty()) {
            return repositoryURI;
        } else {
            return "/repositories/2";
        }
    }

    /**
     * Method to set the repository URI
     */
    public void setRepositoryURI(String repositoryURI) {
        this.repositoryURI = repositoryURI;
    }

    /**
     * Method to save the record that takes into account running in stand alone
     *
     * @param endpoint to make post to
     * @param jsonText record
     */
    public synchronized String saveRecord(String endpoint, String jsonText, String atId) {
        return saveRecord(endpoint, jsonText, null, atId);
    }

    /**
     * Method to save the record that takes into account running in stand alone
     * or within the AT
     *
     * @param endpoint to make post to
     * @param jsonText record
     * @param params   parameters to pass to service
     */
    public synchronized String saveRecord(String endpoint, String jsonText, NameValuePair[] params, String atId) {
        String id = NO_ID;

        try {
            // Make sure we don't try to print out a batch import record since they can
            // be thousands of lines long
            if(endpoint.contains(ASpaceClient.BATCH_IMPORT_ENDPOINT)) {
                print("Route: " + endpoint + "\nBatch Record Length: " +
                        jsonText.length() + " bytes\n" + jsonText);
            } else {
                print("Route: " + endpoint + "\n" + jsonText);
            }

            if(simulateRESTCalls) {
                id = "10000001";
                Thread.sleep(2);
            } else {
                id = aspaceClient.post(endpoint, jsonText, params, atId);
            }
        } catch (Exception e) {
            if(endpoint.contains(ASpaceClient.BATCH_IMPORT_ENDPOINT)) {
                print("Error saving batch import record ...\n" + jsonText);
            } else {
                print("Error saving record" + jsonText);
            }

            incrementErrorCount();
            incrementASpaceErrorCount();
        }

        return id;
    }

    /**
     * Method to increment the error count
     */
    private synchronized void incrementErrorCount() {
        saveErrorCount++;

        if(errorCountLabel != null) {
            errorCountLabel.setText(saveErrorCount + " and counting ...");
        }
    }

    /**
     * Method to increment the aspace error count that occur when saving to the
     * backend
     */
    private synchronized void incrementASpaceErrorCount() {
        aspaceErrorCount++;
    }

    /**
     * Convenient print method for printing string in the text console in the future
     *
     * @param string
     */
    public synchronized void print(String string) {
        if(outputConsole != null) {
            messageCount++;

            if(messageCount < MAX_MESSAGES) {
                outputConsole.append(string + "\n");
            } else {
                messageCount = 0;
                outputConsole.setText(string + "\n");
            }
        } else {
            System.out.println(string);
        }
    }

    /**
     * Method to update the progress bar if not running in command line mode
     *
     * @param recordType
     * @param total
     * @param count
     */
    private synchronized void updateProgress(String recordType, int total, int count) {
        if(progressBar == null) return;

        if(count == -1) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(total);
            progressBar.setString("Deleting " + total + " " + recordType);
        } else if(count == 0) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(1);
            progressBar.setString("Loading " + recordType);
        } else if(count == 1) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(total);
            progressBar.setString("Copying " + total + " " + recordType);
        }

        progressBar.setValue(count);
    }

    /**
     * Method to update the record totals
     *
     * @param recordType
     * @param total
     * @param success
     */
    private void updateRecordTotals(String recordType, int total, int success) {
        float percent = (new Float(success)/new Float(total))*100.0f;
        recordTotals.add(recordType + " : " + success + " / " + total + " (" + String.format("%.2f", percent) + "%)");
    }

    /**
     * Method to return the number of errors when saving records
     *
     * @return
     */
    public int getSaveErrorCount() {
        return saveErrorCount;
    }

    /**
     * Method to add an error message to the buffer
     *
     * @param message
     */
    public synchronized void addErrorMessage(String message) {
        errorBuffer.append(message).append("\n");
        incrementErrorCount();
    }

    /**
     * Method to return the error messages that occurred during the transfer process
     * @return
     */
    public String getSaveErrorMessages() {
        int errorsAndWarnings = saveErrorCount - aspaceErrorCount;

        String errorMessage = "RECORD CONVERSION ERRORS/WARNINGS ( " + errorsAndWarnings + " ) ::\n\n" + errorBuffer.toString() +
                "\n\n\nRECORD SAVE ERRORS ( " + aspaceErrorCount + " ) ::\n\n" + aspaceClient.getErrorMessages() +
                "\n\nTOTAL COPY TIME: " + stopWatch.getPrettyTime() +
                "\n\nNUMBER OF RECORDS COPIED: \n" + getTotalRecordsCopiedMessage() +
                "\n\n" + getSystemInformation();

        return errorMessage;
    }

    /**
     * Method to do certain task after the copy has completed
     */
    public void cleanUp() {
        copying = false;

        String totalRecordsCopied = getTotalRecordsCopiedMessage();

        print("\n\nFinish coping data ... Total time: " + stopWatch.getPrettyTime());
        print("\nNumber of Records copied: \n" + totalRecordsCopied);

        print("\nNumber of errors/warnings: " + saveErrorCount);
    }

    /**
     * Method to return the current status of the migration
     *
     * @return
     */
    public String getCurrentProgressMessage() {
        int errorsAndWarnings = saveErrorCount - aspaceErrorCount;

        String totalRecordsCopied = getTotalRecordsCopiedMessage();

        String errorMessages = "RECORD CONVERSION ERRORS/WARNINGS ( " + errorsAndWarnings + " ) ::\n\n" + errorBuffer.toString() +
                "\n\n\nRECORD SAVE ERRORS ( " + aspaceErrorCount + " ) ::\n\n" + aspaceClient.getErrorMessages();

        String message = errorMessages +
                "\n\nRunning for: " + stopWatch.getPrettyTime() +
                "\n\nCurrent # of Records Copied: \n" + totalRecordsCopied +
                "\n\n" + getSystemInformation();

        return message;
    }

    /**
     * Method to return string with total records copied
     * @return
     */
    private String getTotalRecordsCopiedMessage() {
        String totalRecordsCopied = "";

        for(String entry: recordTotals) {
            totalRecordsCopied += entry + "\n";
        }

        return totalRecordsCopied;
    }

    /**
     * Method to return information about the ASpace and Migration tool version
     *
     * @return
     */
    public String getSystemInformation() {
        return dbCopyFrame.VERSION + "\n" + aspaceInformation;
    }

    /**
     * Method to set the boolean which specifies whether to stop copying the resources
     */
    public void stopCopy() {
        stopCopy = true;
    }

    /**
     * Method to check if the copying process is running
     *
     * @return
     */
    public boolean isCopying() {
        return copying;
    }

    /**
     * Method to set the whether the copying process is running
     *
     * @param copying
     */
    public void setCopying(boolean copying) {
        this.copying = copying;
    }

    /**
     * Method to try and free some memory by refreshing the hibernate session and running GC
     */
    private void freeMemory() {
        if(outputConsole != null) {
            outputConsole.setText("");
        }

        // set the interpreters to null so the record they have will get GCed
        mapper.destroyInterpreter();

        Runtime runtime = Runtime.getRuntime();

        long freeMem = runtime.freeMemory();
        System.out.println("\nFree memory before GC: " + freeMem/1048576L + "MB");

        runtime.gc();

        freeMem = runtime.freeMemory();
        System.out.println("Free memory after GC:  " + freeMem/1048576L + "MB");

        // initialize a new mapper script object
        mapper.initializeScriptInterpreter();
    }

    /**
     * Method to save the URI maps to a binary file
     */
    public void saveURIMaps() {
        HashMap uriMap = new HashMap();

        // only save maps we are going to need,
        // or we not generating from ASpace backend data
        uriMap.put(REPOSITORY_KEY, repositoryURIMap);
        uriMap.put(REPOSITORY_AGENT_KEY, repositoryAgentURIMap);
        uriMap.put(LOCATION_KEY, locationURIMap);
        uriMap.put(SUBJECT_KEY, subjectURIMap);
        uriMap.put(NAME_KEY, nameURIMap);
        uriMap.put(ACCESSION_KEY, accessionURIMap);
        uriMap.put(DIGITAL_OBJECT_KEY, digitalObjectURIMap);
        uriMap.put(RESOURCE_KEY, resourceURIMap);

        // store the record totals array list here also
        uriMap.put(RECORD_TOTAL_KEY, recordTotals);

        // save to file system now
        print("\nSaving URI Maps ...");

        try {
            FileManager.saveUriMapData(uriMapFile, uriMap);
        } catch (Exception e) {
            print("Unable to save URI map file " + uriMapFile.getName());
        }
    }

    /**
     * Method to load the save URI maps
     */
    public boolean loadURIMaps() {
        try {
            HashMap uriMap  = (HashMap) FileManager.getUriMapData(uriMapFile);

            repositoryURIMap = (HashMap<String,String>)uriMap.get(REPOSITORY_KEY);
            repositoryAgentURIMap = (HashMap<String,String>)uriMap.get(REPOSITORY_AGENT_KEY);
            locationURIMap = (HashMap<String,String>)uriMap.get(LOCATION_KEY);
            subjectURIMap = (HashMap<String,String>)uriMap.get(SUBJECT_KEY);
            nameURIMap = (HashMap<String,String>)uriMap.get(NAME_KEY);
            accessionURIMap = (HashMap<String,String>)uriMap.get(ACCESSION_KEY);
            digitalObjectURIMap = (HashMap<String,String>)uriMap.get(DIGITAL_OBJECT_KEY);
            resourceURIMap = (HashMap<String,String>)uriMap.get(RESOURCE_KEY);

            // load the record totals so far
            if(uriMap.containsKey(RECORD_TOTAL_KEY)) {
                recordTotals = (ArrayList<String>)uriMap.get(RECORD_TOTAL_KEY);
            }

            print("Loaded URI Maps");
        } catch (Exception e) {
            print("Unable to load URI map file: " + uriMapFile.getName());
        }

        if(!locationURIMap.isEmpty() || !subjectURIMap.isEmpty() || !nameURIMap.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Method to see if the URI map file exist
     *
     * @return
     */
    public boolean uriMapFileExist() {
        return uriMapFile.exists();
    }

    /**
     * Method used to simulate the REST calls. Useful for testing memory usage and for setting baseline
     * data transfer time
     *
     * @param simulateRESTCalls
     */
    public void setSimulateRESTCalls(boolean simulateRESTCalls) {
        this.simulateRESTCalls = simulateRESTCalls;
    }

    /**
     * Method to set the developer mode
     *
     * @param developerMode
     */
    public void setDeveloperMode(boolean developerMode) {
        this.developerMode = developerMode;
        mapper.setMakeUnique(developerMode);
    }

    /**
     * Method to get the current
     * @return
     */
    public String getCurrentRecordInfo()  {
        String info = "Current Record Type: " + currentRecordType +
                "\nRecord Identifier : " + currentRecordIdentifier;

        return info;
    }

    /**
     * Method to test the conversion without having to startup the gui
     *
     * @param args
     */
    public static void main(String[] args) throws JSONException {
        String currentDirectory  = System.getProperty("user.dir");

        File excelFile = new File(currentDirectory +"/sample_data/Sample Ingest Data.xlsx");

        File bsiMapperScriptFile = new File(currentDirectory + "/src/org/nyu/edu/dlts/scripts/mapper.bsh");
        File jriMapperScriptFile = new File(currentDirectory + "/src/org/nyu/edu/dlts/scripts/mapper.rb");
        File pyiMapperScriptFile = new File(currentDirectory + "/src/org/nyu/edu/dlts/scripts/mapper.py");
        File jsiMapperScriptFile = new File(currentDirectory + "/src/org/nyu/edu/dlts/scripts/mapper.js");

        ASpaceCopy aspaceCopy = new ASpaceCopy("http://localhost:8089", "admin", "admin");

        aspaceCopy.setSimulateRESTCalls(true);

        try {
            // load the mapper scripts
            String bsiMapperScript = FileManager.readTextData(bsiMapperScriptFile);
            String jriMapperScript = FileManager.readTextData(jriMapperScriptFile);
            String pyiMapperScript = FileManager.readTextData(pyiMapperScriptFile);
            String jsiMapperScript = FileManager.readTextData(jsiMapperScriptFile);

            /**
             * Create a new instance for HSSFWorkBook Class
             */
            System.out.println("Loading excel file " + excelFile);

            // set the work book
            FileInputStream fileInputStream = new FileInputStream(excelFile);
            XSSFWorkbook workBook = new XSSFWorkbook(fileInputStream);
            aspaceCopy.setWorkbook(workBook);

            // test the mapper scripts
            System.out.println("Test mapping excel file using BeanShell");

            aspaceCopy.setMapperScriptType(ASpaceMapper.BEANSHELL_SCRIPT);
            aspaceCopy.setMapperScript(bsiMapperScript);

            System.out.println("\n\n");
            aspaceCopy.copyLocationRecords(0);

            System.out.println("\n\n");
            aspaceCopy.copySubjectRecords(1);

            System.out.println("\n\n");
            aspaceCopy.copyNameRecords(2);

            System.out.println("\n\n");
            aspaceCopy.copyAccessionRecords(3);

            System.out.println("\n\n");
            aspaceCopy.copyDigitalObjectRecords(4);

            System.out.println("\n\n");
            aspaceCopy.copyResourceRecords("5,6");

            System.out.println("\n\nTest mapping excel file using Python\n\n");
            aspaceCopy.setMapperScriptType(ASpaceMapper.JYTHON_SCRIPT);
            aspaceCopy.setMapperScript(pyiMapperScript);
            aspaceCopy.copySubjectRecords(1);

            System.out.println("\n\nTest mapping excel file using Javascript\n\n");
            aspaceCopy.setMapperScriptType(ASpaceMapper.JAVASCRIPT_SCRIPT);
            aspaceCopy.setMapperScript(jsiMapperScript);
            aspaceCopy.copySubjectRecords(1);

            System.out.println("\n\nTest mapping excel file using JRuby\n\n");
            aspaceCopy.setMapperScriptType(ASpaceMapper.JRUBY_SCRIPT);
            aspaceCopy.setMapperScript(jriMapperScript);

            System.out.println("\n\n");
            aspaceCopy.copyLocationRecords(0);

            System.out.println("\n\n");
            aspaceCopy.copySubjectRecords(1);

            System.out.println("\n\n");
            aspaceCopy.copyNameRecords(2);

            System.out.println("\n\n");
            aspaceCopy.copyAccessionRecords(3);

            System.out.println("\n\n");
            aspaceCopy.copyDigitalObjectRecords(4);

            System.out.println("\n\n");
            aspaceCopy.copyResourceRecords("5,6");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
