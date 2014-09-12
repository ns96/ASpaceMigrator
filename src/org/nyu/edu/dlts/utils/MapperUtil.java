package org.nyu.edu.dlts.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nyu.edu.dlts.aspace.ASpaceClient;
import org.nyu.edu.dlts.aspace.ASpaceCopy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Nathan Stevens
 *
 * Date: 3/11/14
 * Time: 2:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class MapperUtil {
    // these store the ids of all accessions, resources, and digital objects loaded so we can
    // check for uniqueness before copying them to the ASpace backend
    private static ArrayList<String> digitalObjectIDs = new ArrayList<String>();
    private static ArrayList<String> accessionIDs = new ArrayList<String>();
    private static ArrayList<String> resourceIDs = new ArrayList<String>();
    private static ArrayList<String> eadIDs = new ArrayList<String>();

    private static RandomString randomStringLong = new RandomString(6);
    private static RandomString randomString = new RandomString(3);

    // used to send errors to the UI and add custom enums
    public static ASpaceCopy aspaceCopy;

    // used when adding dynamic enums
    public static HashMap<String, JSONObject> dynamicEnums;

    /**
     * Method to return a unique id, in cases where ASpace needs a unique id but AT doesn't
     *
     * @param endpoint
     * @param id
     * @return
     */
    public static String getUniqueID(String endpoint, String id, String[] idParts) {
        id = id.trim();

        if(endpoint.equals(ASpaceClient.DIGITAL_OBJECT_ENDPOINT)) {
            // if id is empty add text
            if(id.isEmpty()) {
                id = "Digital Object ID ##"+ randomStringLong.nextString();
            }

            if(!digitalObjectIDs.contains(id)) {
                digitalObjectIDs.add(id);
            } else {
                id += " ##" + randomStringLong.nextString();
                digitalObjectIDs.add(id);
            }

            return id;
        } else if(endpoint.equals(ASpaceClient.ACCESSION_ENDPOINT)) {
            String message = null;

            if(!accessionIDs.contains(id)) {
                accessionIDs.add(id);
            } else {
                String fullId = "";

                do {
                    idParts[0] += " ##" + randomString.nextString();
                    fullId = concatIdParts(idParts);
                } while(accessionIDs.contains(fullId));

                accessionIDs.add(fullId);

                message = "Duplicate Accession Id: "  + id  + " Changed to: " + fullId + "\n";
                aspaceCopy.addErrorMessage(message);
            }

            // we don't need to return the new id here, since the idParts array
            // is being used to to store the new id
            return "not used";
        } else if(endpoint.equals(ASpaceClient.RESOURCE_ENDPOINT)) {
            String message = null;

            if(!resourceIDs.contains(id)) {
                resourceIDs.add(id);
            } else {
                String fullId = "";

                do {
                    idParts[0] += " ##" + randomString.nextString();
                    fullId = concatIdParts(idParts);
                } while(resourceIDs.contains(fullId));

                resourceIDs.add(fullId);

                message = "Duplicate Resource Id: "  + id  + " Changed to: " + fullId + "\n";
                aspaceCopy.addErrorMessage(message);
            }

            // we don't need to return the new id here, since the idParts array
            // is being used to to store the new id
            return "not used";
        } else if(endpoint.equals("ead")) {
            if(id.isEmpty()) {
                return "";
            }

            if(!eadIDs.contains(id)) {
                eadIDs.add(id);
            } else {
                String nid = "";

                do {
                    nid = id + " ##" + randomString.nextString();
                } while(eadIDs.contains(nid));

                eadIDs.add(nid);

                id = nid;
            }

            return id;
        } else {
            return id;
        }
    }

    /**
     * Method to concat the id parts in a string array into a full id delimited by "."
     *
     * @param ids
     * @return
     */
    private static String concatIdParts(String[] ids) {
        String fullId = "";
        for(int i = 0; i < ids.length; i++) {
            if(!ids[i].isEmpty() && i == 0) {
                fullId += ids[0];
            } else if(!ids[i].isEmpty()) {
                fullId += "."  + ids[i];
            }
        }

        return fullId;
    }

    /**
     * Method to add the AT internal database ID as an external ID for the ASpace object
     *
     * @param recordID
     * @param source
     */
    public static void addExternalId(String recordID, JSONObject recordJS, String source) throws Exception {
        source = "Excel File::" + source.toUpperCase();

        // id is always the first cell
        JSONArray externalIdsJA = new JSONArray();
        JSONObject externalIdJS = new JSONObject();

        externalIdJS.put("external_id", recordID);
        externalIdJS.put("source", source);

        externalIdsJA.put(externalIdJS);

        recordJS.put("external_ids", externalIdsJA);
    }

    /**
     * Method to get a reference object which points to another URI
     *
     * @param recordURI
     * @return
     * @throws Exception
     */
    public static JSONObject getReferenceObject(String recordURI) throws Exception {
        JSONObject referenceJS = new JSONObject();
        referenceJS.put("ref", recordURI);
        return referenceJS;
    }

    /**
     * Method to add a date object
     *
     * @param recordJS
     * @param dateExpression
     * @param label
     * @return
     * @throws Exception
     */
    public static boolean addDate(JSONObject recordJS, String dateExpression, String label) throws Exception {
        if(dateExpression.isEmpty()) return false;

        JSONArray dateJA = new JSONArray();
        JSONObject dateJS = new JSONObject();

        dateJS.put("date_type", "single");
        dateJS.put("label", label);
        dateJS.put("expression", dateExpression);

        dateJA.put(dateJS);
        recordJS.put("dates", dateJA);

        return true;
    }

    /**
     * Method to add a fileversion to the main array
     *
     * @param fileVersionsJA
     * @param uri
     * @param useStatement
     * @param actuate
     * @param attribute
     * @throws JSONException
     */
    public static void addFileVersion(JSONArray fileVersionsJA, String uri, String useStatement,
                                      String actuate, String attribute) throws JSONException {
        JSONObject fileVersionJS = new JSONObject();

        fileVersionJS.put("file_uri", uri);
        fileVersionJS.put("use_statement", useStatement);
        fileVersionJS.put("xlink_actuate_attribute", actuate);
        fileVersionJS.put("xlink_show_attribute", attribute);

        fileVersionsJA.put(fileVersionJS);
    }

    /**
     * Method to convert an analog instance to an equivalent ASpace instance
     *
     * @param instanceType
     * @param locationURI
     * @return
     * @throws Exception
     */
    public static JSONObject createAnalogInstance(String instanceType, String barcode,
                                           String type1, String indicator1,
                                           String type2, String indicator2,
                                           String type3, String indicator3,
                                           String locationURI) throws Exception {
        JSONObject instanceJS = new JSONObject();

        // set the type
        instanceJS.put("instance_type", updateEnumValue("instance_instance_type", instanceType));

        // add the container now
        JSONObject containerJS = new JSONObject();

        containerJS.put("type_1", updateEnumValue("container_type", type1));
        containerJS.put("indicator_1", indicator1);
        containerJS.put("barcode_1", barcode);

        if(!type2.isEmpty()) {
            containerJS.put("type_2", updateEnumValue("container_type", type2));
            containerJS.put("indicator_2", indicator2);
        }

        if(!type3.isEmpty()) {
            containerJS.put("type_3", updateEnumValue("container_type",type3));
            containerJS.put("indicator_3", indicator3);
        }

        // add the location now if needed
        if(locationURI != null && !locationURI.isEmpty()) {
            Date date = new Date(); // this is need to have valid container_location json record

            JSONArray locationsJA = new JSONArray();

            JSONObject locationJS = new JSONObject();
            locationJS.put("status", "current");
            locationJS.put("start_date", date);
            locationJS.put("ref", locationURI);

            locationsJA.put(locationJS);
            containerJS.put("container_locations", locationsJA);
        }

        instanceJS.put("container", containerJS);

        return instanceJS;
    }

    /**
     * Method to convert a digital instance to a json record
     *
     * @param digitalObjectURI
     * @return
     * @throws Exception
     */
    public static JSONObject createDigitalInstance(String digitalObjectURI) throws Exception {
        JSONObject instanceJS = new JSONObject();

        if(digitalObjectURI == null || digitalObjectURI.isEmpty()) return null;

        instanceJS.put("instance_type", "digital_object");
        instanceJS.put("digital_object", getReferenceObject(digitalObjectURI));

        return instanceJS;
    }

    /**
     * Method to create a dummy instance to old the location information
     *
     *
     * @param accessionId
     * @param locationNote
     * @return
     * @throws Exception
     */
    public static JSONObject createAccessionInstance(String accessionId, String locationURI, String locationNote) throws Exception {
        JSONObject instanceJS = new JSONObject();

        // set the type
        instanceJS.put("instance_type", "accession");

        // add the container now
        JSONObject containerJS = new JSONObject();

        containerJS.put("type_1", "object");
        containerJS.put("indicator_1", accessionId);

        Date date = new Date(); // this is need to have valid container_location json record
        JSONArray locationsJA = new JSONArray();

        JSONObject locationJS = new JSONObject();
        locationJS.put("status", "current");
        locationJS.put("start_date", date);
        locationJS.put("ref", locationURI);
        locationJS.put("note", locationNote);

        locationsJA.put(locationJS);

        containerJS.put("container_locations", locationsJA);
        instanceJS.put("container", containerJS);

        return instanceJS;
    }

    /**
     * Method to add extent information
     *
     * @param extentJA
     * @param portion
     * @param type
     * @param extent
     * @throws Exception
     */
    public static void addExtent(JSONArray extentJA, String portion, String extent, String type) throws Exception {
        addExtent(extentJA, portion, extent, type, "", "", "");
    }

    /**
     * Method to add extent information
     *
     * @param extentJA
     * @param portion
     * @param type
     * @param extent
     * @throws Exception
     */
    public static void addExtent(JSONArray extentJA, String portion, String extent, String type,
                                 String containerSummary, String physicalDetails, String dimensions) throws Exception {
        JSONObject extentJS = new JSONObject();
        extentJS.put("portion", portion);
        extentJS.put("extent_type", updateEnumValue("extent_extent_type", type));
        extentJS.put("number", extent);
        extentJS.put("container_summary", containerSummary);
        extentJS.put("physical_details", physicalDetails);
        extentJS.put("dimensions", dimensions);
        extentJA.put(extentJS);
    }

    /**
     * Method to add a date json object
     *
     * @param dateJA
     * @param dateExpression
     */
    public static void addDateExpression(JSONArray dateJA, String dateExpression) throws Exception {
        JSONObject dateJS = new JSONObject();
        dateJS.put("date_type", "single");
        dateJS.put("label", "created");
        dateJS.put("expression", dateExpression);
        dateJA.put(dateJS);
    }

    /**
     * Method to add date inclusive date that just consist of date expression
     *
     * @param dateJA
     * @param label
     * @param dateExpression
     * @throws Exception
     */
    public static void addInclusiveDate(JSONArray dateJA, String label, String dateExpression) throws Exception {
        addInclusiveDate(dateJA, label, dateExpression, "", "");
    }

    /**
     * Method to add an inclusive date
     * @param dateJA
     * @param begin
     * @param end
     * @throws Exception
     */
    public static void addInclusiveDate(JSONArray dateJA, String label, String dateExpression, String begin, String end) throws Exception {
        addDate(dateJA, "inclusive", label, dateExpression, begin, end);
    }

    /**
     * Method to add date bulk date that just consist of date expression
     *
     * @param dateJA
     * @param label
     * @param dateExpression
     * @throws Exception
     */
    public static void addBulkDate(JSONArray dateJA, String label, String dateExpression) throws Exception {
        addBulkDate(dateJA, label, dateExpression, "", "");
    }

    /**
     * Method to add an bulk date
     * @param dateJA
     * @param begin
     * @param end
     * @throws Exception
     */
    public static void addBulkDate(JSONArray dateJA, String label, String dateExpression, String begin, String end) throws Exception {
        addDate(dateJA, "bulk", label, dateExpression, begin, end);
    }

    /**
     * Method to add a date object to the dates array
     * @param dateJA
     * @param begin
     * @param end
     * @throws Exception
     */
    public static void addDate(JSONArray dateJA, String dateType, String label, String dateExpression, String begin, String end) throws Exception {
        JSONObject dateJS = new JSONObject();
        dateJS.put("date_type", dateType);
        dateJS.put("label", label);
        dateJS.put("expression", dateExpression);
        dateJS.put("begin", begin);
        dateJS.put("end", end);
        dateJA.put(dateJS);
    }

    /**
     * Add an external document to the JSON object
     * @param externalDocumentsJA
     * @param title
     * @param location
     */
    public static void addExternalDocument(JSONArray externalDocumentsJA, String title, String location) throws Exception {
        JSONObject documentJS = new JSONObject();
        documentJS.put("title", title);
        documentJS.put("location", MapperUtil.fixUrl(location));
        externalDocumentsJA.put(documentJS);
    }

    /**
     * Add a collection management object
     *
     * @param collectionManagementJA
     * @param catalogedNote
     * @param processors
     * @throws Exception
     */
    public static void addCollectionManagement(JSONArray collectionManagementJA, String catalogedNote, String processors) throws Exception {
        JSONObject collectionManagementJS = new JSONObject();
        collectionManagementJS.put("cataloged_note", catalogedNote);
        collectionManagementJS.put("processors", processors);
        collectionManagementJA.put(collectionManagementJS);
    }

    /**
     * Method to add single part note
     * @param notesJA
     * @param noteType
     * @param noteLabel
     * @param noteContent
     * @throws Exception
     */
    public static void addSinglePartNote(JSONArray notesJA, String noteType, String noteLabel, String noteContent) throws Exception {
        if(noteContent.isEmpty()) return;

        JSONObject noteJS = new JSONObject();

        noteJS.put("jsonmodel_type", "note_singlepart");
        noteJS.put("type", noteType);
        noteJS.put("label", noteLabel);

        JSONArray contentJA = new JSONArray();
        contentJA.put(noteContent);
        noteJS.put("content", contentJA);

        notesJA.put(noteJS);
    }

    /**
     * Add a multipart note
     *
     * @param notesJA
     * @param noteType
     * @param noteLabel
     * @param noteContent
     * @throws Exception
     */
    public static void addMultipartNote(JSONArray notesJA, String noteType, String noteLabel, String noteContent) throws Exception {
        addMultipartNoteWithType(notesJA, "note_multipart", noteType, noteLabel, noteContent);;
    }

    /**
     * Add a multipart note
     *
     * @param notesJA
     * @param noteLabel
     * @param noteContent
     * @throws Exception
     */
    public static void addBiogHistNote(JSONArray notesJA, String noteLabel, String noteContent) throws Exception {
        addMultipartNoteWithType(notesJA, "note_bioghist", "", noteLabel, noteContent);
    }

    /**
     * Add a multipart note and specify the type
     *
     * @param notesJA
     * @param noteType
     * @param noteLabel
     * @param noteContent
     * @throws Exception
     */
    public static void addMultipartNoteWithType(JSONArray notesJA, String jsonModelType, String noteType, String noteLabel, String noteContent) throws Exception {
        if(noteContent.isEmpty()) return;

        JSONObject noteJS = new JSONObject();

        noteJS.put("jsonmodel_type", jsonModelType);
        noteJS.put("type", noteType);
        noteJS.put("label", noteLabel);

        JSONArray subnotesJA = new JSONArray();

        // add the default text note
        JSONObject textNoteJS = new JSONObject();
        addTextNote(textNoteJS, MapperUtil.fixEmptyString(noteContent, "multi-part note content"));
        subnotesJA.put(textNoteJS);

        noteJS.put("subnotes", subnotesJA);

        notesJA.put(noteJS);
    }

    /**
     * Method to add a text note
     *
     * @param noteJS
     * @param content
     * @throws Exception
     */
    public static void addTextNote(JSONObject noteJS, String content) throws Exception {
        noteJS.put("jsonmodel_type", "note_text");
        noteJS.put("content", content);
    }

    /**
     * Method to add digital object note
     *
     * @param notesJA
     * @param noteType
     * @param noteLabel
     * @param noteContent
     * @throws Exception
     */
    private void addDigitalObjectNote(JSONArray notesJA, String noteType, String noteLabel, String noteContent) throws Exception {
        if(noteContent.isEmpty()) return;

        JSONObject noteJS = new JSONObject();

        noteJS.put("jsonmodel_type", "note_digital_object");
        noteJS.put("type", noteType);
        noteJS.put("label", noteLabel);

        JSONArray contentJA = new JSONArray();
        contentJA.put(noteContent);
        noteJS.put("content", contentJA);

        notesJA.put(noteJS);
    }

    /**
     * Method to get an event object Accession processed info
     *
     *
     * @param accessionJS
     * @param accessionURI
     * @param agentURI
     * @return
     */
    public static ArrayList<JSONObject> getAccessionEvents(JSONObject accessionJS, String agentURI, String accessionURI) throws Exception {
        ArrayList<JSONObject> eventsList = new ArrayList<JSONObject>();
        JSONObject eventJS;

        if(accessionJS.has("processed_date")) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "processed");
            addEventDate(eventJS, accessionJS.get("processed_date") ,"single", "event");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accessionJS.has("acknowledgement_sent_date")) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "acknowledgement_sent");
            addEventDate(eventJS, accessionJS.get("acknowledgement_sent_date"), "single", "event");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accessionJS.has("agreement_signed_date")) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "agreement_signed");
            addEventDate(eventJS, accessionJS.get("agreement_signed_date"), "single", "event");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accessionJS.has("agreement_sent_date")) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "agreement_sent");
            addEventDate(eventJS, accessionJS.get("agreement_sent_date"), "single", "event");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accessionJS.has("cataloged_date")) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "cataloged");
            addEventDate(eventJS, accessionJS.get("cataloged_date"), "single", "event");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accessionJS.has("processing_started_date")) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "processing_started");
            addEventDate(eventJS, accessionJS.get("processing_started_date"), "single", "event");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accessionJS.has("copyright_transfer_date")) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "copyright_transfer");

            if(accessionJS.has("copyright_transfer_note")) {
                eventJS.put("outcome_note", accessionJS.get("copyright_transfer_note"));
            }

            addEventDate(eventJS, accessionJS.get("copyright_transfer_date"), "single", "event");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        return eventsList;
    }

    /**
     * Method to add a date object
     *
     * @param eventJS
     * @param date
     * @param dateType
     * @param dateLabel
     */
    public static void addEventDate(JSONObject eventJS, Object date, String dateType, String dateLabel) throws Exception {
        JSONObject dateJS = new JSONObject();
        dateJS.put("date_type", dateType);
        dateJS.put("label", dateLabel);
        dateJS.put("expression", date.toString());
        eventJS.put("date", dateJS);
    }

    /**
     * Method to add the event linked record
     *
     * @param uri
     * @param eventJS
     * @throws Exception
     */
    public static void addEventLinkedRecordAndAgent(JSONObject eventJS, String agentURI, String uri) throws Exception {
        // add a dummy linked agent so record can save
        JSONArray linkedAgentsJA = new JSONArray();
        JSONObject linkedAgentJS = new JSONObject();

        linkedAgentJS.put("role", "recipient");
        linkedAgentJS.put("ref", agentURI);
        linkedAgentsJA.put(linkedAgentJS);

        eventJS.put("linked_agents", linkedAgentsJA);

        // add the linked to the record
        JSONArray linkedRecordsJA = new JSONArray();
        JSONObject linkedRecordJS = new JSONObject();

        linkedRecordJS.put("role", "source");
        linkedRecordJS.put("ref", uri);
        linkedRecordsJA.put(linkedRecordJS);

        eventJS.put("linked_records", linkedRecordsJA);
    }

    /**
     * Method to create and link a single name record. It just calls the same method in
     * the ascopy object
     *
     * @param recordJS
     * @param role
     * @param nameType
     * @param primaryName
     * @param source
     */
    public static void addName(JSONObject recordJS, String role, String nameType, String primaryName, String source) throws Exception {
        aspaceCopy.createAndAddName(recordJS, role.toLowerCase(), nameType.toLowerCase(), primaryName.trim(), source.toLowerCase());
    }

    /**
     * Method to create and add a single subject. It just calls the same method in
     * the ascopy object
     *
     * @param recordJS
     * @param source
     * @param termType
     * @param terms
     * @throws Exception
     */
    public static void addSubject(JSONObject recordJS, String source, String termType, String terms) throws Exception {
        aspaceCopy.createAndAddSubject(recordJS, source.toLowerCase(), termType.toLowerCase(), terms.trim());
    }

    /**
     * Method to return the column number given a character
     *
     * @param column
     * @return
     */
    public static int getColumnNumber(char column) {
        return Character.getNumericValue(column) - 10;
    }

    /**
     * Method to return the column number given a string character like AA, or BF
     *
     * @param column
     * @return
     */
    public static int getColumnNumber(String column) {
        int num1 = Character.getNumericValue(column.charAt(0)) - 10;
        int num2 = Character.getNumericValue(column.charAt(1)) - 10;
        int index = (num1 + 1) * 26 + num2;
        //System.out.println("Columns " + num1 + ", " + num2 + "  => " + index);
        return index;
    }

    /**
     * Method to set a string that's empty to "unspecified"
     * @param text
     * @return
     */
    public static String fixEmptyString(String text) {
        return fixEmptyString(text, null);
    }

    /**
     * Method to set a string that empty to "not set"
     * @param text
     * @return
     */
    public static String fixEmptyString(String text, String useInstead) {
        if(text == null || text.trim().isEmpty()) {
            if(useInstead == null) {
                return "unspecified";
            } else {
                return useInstead;
            }
        } else {
            return text;
        }
    }

    /**
     * Method to prepend http:// to a url to prevent ASpace from complaining
     *
     * @param url
     * @return
     */
    public static String fixUrl(String url) {
        if(url.isEmpty()) return "http://url.unspecified";

        String lowercaseUrl = url.toLowerCase();

        // check to see if its a proper uri format
        if(lowercaseUrl.contains("://")) {
            return url;
        } else if(lowercaseUrl.startsWith("/") || lowercaseUrl.contains(":\\")) {
            url = "file://" + url;
            return url;
        } else {
            url = "http://" + url;
            return  url;
        }
    }

    /**
     * Method to normalize a enum value
     *
     * @param value
     * @return
     */
    public static String normalizeEnumValue(String value) {
        return value.toLowerCase().replace(" ", "_");
    }

    /**
     * Method to normalize, and add to the aspace enum list if necessary.
     *
     * @param enumName
     * @param value
     * @return
     */
    public static String updateEnumValue(String enumName, String value) {
        String enumValue = value.toLowerCase().replace(" ", "_");

        if (dynamicEnums != null) {
            JSONObject enumJS = dynamicEnums.get(enumName);

            if (enumJS != null) {
                try {
                    JSONArray valuesJA = enumJS.getJSONArray("values");

                    // Do string comparison to see if has the value or not.
                    // Not the most efficient way to do this, but it works
                    if (!valuesJA.toString().contains("\"" + enumValue + "\"")) {
                        valuesJA.put(enumValue);
                        aspaceCopy.updateDynamicEnum(enumJS);

                        // sleep for 5 seconds since ASpace seems to need sometime before the newly
                        // added enum propagates to all places, even though it returns the newly created record
                        // fine???
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return enumValue;
    }

    /**
     * Method to normalize, and add to the aspace enum list if necessary.
     *
     * @param enumName
     * @return
     */
    public static void updateEnumValues(String enumName, ArrayList<String> enumValues) {
        if (dynamicEnums != null) {
            JSONObject enumJS = dynamicEnums.get(enumName);

            if (enumJS != null) {
                try {
                    JSONArray valuesJA = enumJS.getJSONArray("values");

                    for(String value: enumValues) {
                        String enumValue = value.toLowerCase().replace(" ", "_");

                        // Do string comparison to see if has the value or not.
                        // Not the most efficient way to do this, but it works
                        if (!valuesJA.toString().contains("\"" + enumValue + "\"")) {
                            valuesJA.put(enumValue);
                        }
                    }

                    // update the enum now
                    aspaceCopy.updateDynamicEnum(enumJS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Main method for testing
     *
     * @param args
     */
    public static void main(String[] args) {
        getColumnNumber("AA");
        getColumnNumber("AB");
        getColumnNumber("FB");
    }
}
