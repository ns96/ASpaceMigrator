package org.nyu.edu.dlts.custom;


import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nyu.edu.dlts.aspace.ASpaceClient;
import org.nyu.edu.dlts.aspace.ASpaceCopy;
import org.nyu.edu.dlts.aspace.ASpaceMapper;
import org.nyu.edu.dlts.models.RowRecord;
import org.nyu.edu.dlts.utils.FileManager;
import org.nyu.edu.dlts.utils.MapperUtil;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;

/**
 * A simple class for testing creation and usage of a custom ASpace copy class.
 *
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 7/14/14
 * Time: 2:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class ASpaceCopyCustom extends ASpaceCopy {
    // static field used to store/access records in the db4o database
    public static final String ACCESSION_TYPE = "accession";
    public static final String CONTACT_TYPE = "contact";
    public static final String CREATOR_TYPE = "creator";
    public static final String COLLECTION_TYPE = "collection";
    public static final String SERIES_TYPE = "series";
    public static final String BOX_TYPE = "box";
    public static final String FILE_TYPE = "file";

    // field used for creation of unique ids
    private int nextId = 0;

    /**
     * The main constructor which just calls the constructor of the parent
     *
     * @param host
     * @param admin
     * @param adminPassword
     */
    public ASpaceCopyCustom(String host, String admin, String adminPassword) {
        super(host, admin, adminPassword);
    }

    /**
     * Method to set the workbook and specify the record type for caching into the
     * db4o database
     *
     * @param workbook
     * @param recordType
     */
    public void setWorkbook(XSSFWorkbook workbook, String recordType) {
        XSSFSheet xssfSheet = workbook.getSheetAt(0);
        cacheRowRecord(recordType, xssfSheet);
    }

    /**
     * Method to create a new workbook containing information for creation of agent records
     *
     * @return
     */
    public void copyNameRecords() throws Exception {
        print("Copying Name records ...");

        // first get record that maps creators and contact information
        HashMap<String, String> creatorContactMap = getCreatorContactMap();

        // now iterate through the creator records and create the Agent JSON objects
        List<RowRecord> rowList = getRowList(CREATOR_TYPE);

        int total = rowList.size();
        int count = 0;
        int success = 0;

        /**
         * Iterate the row data of the spreadsheet
         */
        for (RowRecord record: rowList) {
            if (stopCopy) return;

            String rowId = record.getRowId();
            String recordId = "Creator_" + rowId;

            // get the contact information in the row
            String contactRowId = creatorContactMap.get(rowId);

            RowRecord contactRecord = getRowData(CONTACT_TYPE, contactRowId);

            JSONObject recordJS = mapper.convertName(null, record, contactRecord, recordId);
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
                nameURIMap.put(rowId, uri);
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
     * Method to get the creator and contact link
     * @return
     */
    private HashMap<String,String> getCreatorContactMap() throws Exception {
        // first step is to create a hashmap which relates creator to contacts.
        HashMap<String, String> recordMap = new HashMap<String, String>();

        // load the current spreadsheet from the work book
        List<RowRecord> rowList = getRowList(ACCESSION_TYPE);

        /**
         * Iterate the row data of the spreadsheet
         */
        for (RowRecord rowRecord : rowList) {
            String creatorId = rowRecord.getValue(2);
            String contactId = rowRecord.getValue(1);

            if(!creatorId.isEmpty() && !contactId.isEmpty()) {
                recordMap.put(creatorId, contactId);
                //System.out.println("Mapping Creator -- Contact: " + creatorId + "/" + contactId);
            }
        }

        return recordMap;
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

            String recordId = rowRecord.getValue(0);

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
        List<RowRecord> rowList = getRowList(COLLECTION_TYPE);

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
            addRelatedAccessions(recordId, resourceJS);

            // add the resource to the batch array now
            batchJA.put(resourceJS);

            // any any series records
            copySeriesRecords(batchJA, aoEndpoint, recordId, resourceURI);

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
     * Method to copy series level records
     *
     * @param batchJA
     * @param aoEndpoint
     * @param collectionId
     * @throws Exception
     */
    private void copySeriesRecords(JSONArray batchJA, String aoEndpoint, String collectionId, String resourceURI) throws Exception {
        List<RowRecord> rowList = getRowList(SERIES_TYPE, collectionId);

        for (RowRecord rowRecord : rowList) {
            if (stopCopy) return;

            String seriesId = rowRecord.getValue(0);
            String uniqueId = rowRecord.getUniqueId();
            String fullPath = collectionId + "->(" + uniqueId + ") " + seriesId;

            String componentTitle = "Series_" + fullPath;
            String cId = uniqueId;
            String cURI = aoEndpoint + "/" + cId;

            JSONObject componentJS = mapper.convertResourceComponent(SERIES_TYPE, rowRecord, null, seriesId);

            componentJS.put("uri", cURI);
            componentJS.put("jsonmodel_type", "archival_object");
            componentJS.put("resource", MapperUtil.getReferenceObject(resourceURI));

            /*String parentURI = getParentURI(dbId, aoEndpoint, childRow);
            if(parentURI != null) {
                componentJS.put("parent", MapperUtil.getReferenceObject(parentURI));
            }*/

            // add the subjects
            addSubjects(cId, componentJS);

            // add the linked agents aka Names records
            addNames(cId, componentJS);

            // add the instances
            addInstances(cId, componentJS);

            // add the component to batch JA now
            batchJA.put(componentJS);

            print("Copied Series Level Component: " + componentTitle + " :: " + cId + "\n");

            // now copy any box records
            copyBoxRecords(batchJA, aoEndpoint, seriesId, resourceURI, cURI, fullPath);
        }
    }

    /**
     * Method to copy box level components
     *
     * @param batchJA
     * @param aoEndpoint
     * @param seriesId
     * @param resourceURI
     * @param parentURI
     * @param parentPath
     * @throws Exception
     */
    private void copyBoxRecords(JSONArray batchJA, String aoEndpoint, String seriesId, String resourceURI,
                                String parentURI, String parentPath) throws Exception {

        // get any box level records
        List<RowRecord> rowList = getRowList(BOX_TYPE, seriesId);

        for (RowRecord rowRecord : rowList) {
            if (stopCopy) return;

            String boxId = rowRecord.getValue(0);
            String uniqueId = rowRecord.getUniqueId();
            String fullPath = parentPath + "->(" + uniqueId + ") " + boxId;

            String componentTitle = "Box_" + fullPath;
            String cId = uniqueId;
            String cURI = aoEndpoint + "/" + cId;

            JSONObject componentJS = mapper.convertResourceComponent(BOX_TYPE, rowRecord, null, boxId);

            componentJS.put("uri", cURI);
            componentJS.put("jsonmodel_type", "archival_object");
            componentJS.put("resource", MapperUtil.getReferenceObject(resourceURI));

            componentJS.put("parent", MapperUtil.getReferenceObject(parentURI));

            // add the subjects
            addSubjects(cId, componentJS);

            // add the linked agents aka Names records
            addNames(cId, componentJS);

            // add the instances
            addInstances(cId, componentJS);

            // add the component to batch JA now
            batchJA.put(componentJS);

            print("Copied Box Level Component: " + componentTitle + " :: " + cId + "\n");

            // copy any box records
            copyFileRecords(batchJA, aoEndpoint, boxId, resourceURI, cURI, fullPath);
        }
    }

    /**
     * Method to copy file level record
     *
     * @param batchJA
     * @param aoEndpoint
     * @param boxId
     * @param resourceURI
     * @param parentURI
     * @param parentPath
     * @throws Exception
     */
    private void copyFileRecords(JSONArray batchJA, String aoEndpoint, String boxId, String resourceURI,
                                String parentURI, String parentPath) throws Exception {

        List<RowRecord> rowList = getRowList(FILE_TYPE, boxId);

        for (RowRecord rowRecord : rowList) {
            String fileId = rowRecord.getValue(0);
            String uniqueId = rowRecord.getUniqueId();
            String fullPath = parentPath + "->(" + uniqueId + ") " + fileId;

            String componentTitle = "File_" + fullPath;
            String cId = uniqueId;
            String cURI = aoEndpoint + "/" + cId;

            JSONObject componentJS = mapper.convertResourceComponent(FILE_TYPE, rowRecord, null, fileId);

            componentJS.put("uri", cURI);
            componentJS.put("jsonmodel_type", "archival_object");
            componentJS.put("resource", MapperUtil.getReferenceObject(resourceURI));

            componentJS.put("parent", MapperUtil.getReferenceObject(parentURI));

            // add the subjects
            addSubjects(cId, componentJS);

            // add the linked agents aka Names records
            addNames(cId, componentJS);

            // add the instances
            addInstances(cId, componentJS);

            // add the component to batch JA now
            batchJA.put(componentJS);

            print("Copied File Level Component: " + componentTitle + " :: " + cId + "\n");
        }
    }

    /**
     * Method to generate find and store (in db4o) the relations needs to build a collection record
     */
    private void generateCollectionRelationships() {
        updateParentIds(SERIES_TYPE);
        updateParentIds(BOX_TYPE);
        updateParentIds(FILE_TYPE);
        System.out.println("Max Unique Id = " + nextId);
    }

    /**
     * Method to update the parentIds for a particular record type
     *
     * @param recordType
     */
    private void updateParentIds(String recordType) {
        List<RowRecord> rowList = getRowList(recordType);

        System.out.println("Updating ParentIds: " + recordType + " / " + rowList.size() + " records");

        for (RowRecord record : rowList) {
            String parentId = record.getValue(1);

            if(!parentId.isEmpty()) {
                record.setParentRowId(parentId);
            }

            // add a unique id for this record so we can safely create the collection tree
            String uniqueId = "" + nextId++;
            record.setUniqueId(uniqueId);

            // update the object in the database now
            db.store(record);
        }
    }

    /**
     * Method to test the conversion without having to startup the gui
     *
     * @param args
     */
    public static void main(String[] args) throws JSONException {
        String currentDirectory  = System.getProperty("user.home");

        // the db40 database filename
        String databaseFilename = currentDirectory +"/temp/TestData/cacheDatabase.db4o";

        File accessionFile = new File(currentDirectory +"/temp/TestData/Accessions.xlsx");
        File contactFile = new File(currentDirectory +"/temp/TestData/Contacts.xlsx");
        File creatorFile = new File(currentDirectory +"/temp/TestData/Creator.xlsx");
        File collectionFile = new File(currentDirectory +"/temp/TestData/CollectionsTable.xlsx");
        File seriesFile = new File(currentDirectory +"/temp/TestData/Series.xlsx");
        File boxFile = new File(currentDirectory +"/temp/TestData/Boxes.xlsx");
        File fileFile = new File(currentDirectory +"/temp/TestData/Files.xlsx");

        File bsiMapperScriptFile = new File(currentDirectory + "/temp/TestData/mapper.bsh");

        ASpaceCopyCustom aspaceCopy = new ASpaceCopyCustom("http://localhost:8089", "admin", "admin");
        aspaceCopy.setSimulateRESTCalls(true);
        //aspaceCopy.getSession();

        try {
            // load the mapper scripts
            String bsiMapperScript = FileManager.readTextData(bsiMapperScriptFile);

            /*
             * Initial the db4o database and add records if needs
             */
            boolean createCache = aspaceCopy.initializeDB4O(databaseFilename);

            if (createCache) {
                System.out.println("Loading Accessions excel file " + accessionFile);
                FileInputStream fileInputStream = new FileInputStream(accessionFile);
                XSSFWorkbook workBook = new XSSFWorkbook(fileInputStream);
                aspaceCopy.setWorkbook(workBook, ACCESSION_TYPE);

                System.out.println("Loading Contacts excel file " + contactFile);
                fileInputStream = new FileInputStream(contactFile);
                workBook = new XSSFWorkbook(fileInputStream);
                aspaceCopy.setWorkbook(workBook, CONTACT_TYPE);

                System.out.println("Loading Creator excel file " + creatorFile);
                fileInputStream = new FileInputStream(creatorFile);
                workBook = new XSSFWorkbook(fileInputStream);
                aspaceCopy.setWorkbook(workBook, CREATOR_TYPE);

                System.out.println("Loading Collection excel file " + collectionFile);
                fileInputStream = new FileInputStream(collectionFile);
                workBook = new XSSFWorkbook(fileInputStream);
                aspaceCopy.setWorkbook(workBook, COLLECTION_TYPE);

                System.out.println("Loading Series excel file " + seriesFile);
                fileInputStream = new FileInputStream(seriesFile);
                workBook = new XSSFWorkbook(fileInputStream);
                aspaceCopy.setWorkbook(workBook, SERIES_TYPE);

                System.out.println("Loading Boxes excel file " + boxFile);
                fileInputStream = new FileInputStream(boxFile);
                workBook = new XSSFWorkbook(fileInputStream);
                aspaceCopy.setWorkbook(workBook, BOX_TYPE);

                System.out.println("Loading Files excel file " + fileFile);
                fileInputStream = new FileInputStream(fileFile);
                workBook = new XSSFWorkbook(fileInputStream);
                aspaceCopy.setWorkbook(workBook, FILE_TYPE);

                aspaceCopy.generateCollectionRelationships();
            }

            // test the mapper scripts
            System.out.println("Test mapping excel file using BeanShell");

            aspaceCopy.setMapperScriptType(ASpaceMapper.BEANSHELL_SCRIPT);
            aspaceCopy.setMapperScript(bsiMapperScript);

            // copy the name records
            aspaceCopy.copyNameRecords();

            // copy the accession records
            aspaceCopy.copyAccessionRecords();

            // copy the resource records
            aspaceCopy.copyResourceRecords();

            /*System.out.println("\n\n");
            aspaceCopy.copyLocationRecords(0);

            System.out.println("\n\n");
            aspaceCopy.copySubjectRecords(1);

            System.out.println("\n\n");
            aspaceCopy.copyNameRecords(2);
            */

            System.out.println("\n\n");
            //aspaceCopy.copyAccessionRecords(3);
            //aspaceCopy.copyAccessionRecords(2);

            /*
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
            */
        } catch (Exception e) {
            e.printStackTrace();
        }

        // method to close the db4o database
        aspaceCopy.closeDB4O();
    }
}
