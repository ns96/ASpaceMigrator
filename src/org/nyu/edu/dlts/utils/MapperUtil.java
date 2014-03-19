package org.nyu.edu.dlts.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nyu.edu.dlts.aspace.ASpaceClient;
import org.nyu.edu.dlts.aspace.ASpaceCopy;

import java.util.ArrayList;
import java.util.Date;

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

    // used to send errors to the UI;
    public static ASpaceCopy aspaceCopy;

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
        instanceJS.put("instance_type", instanceType);

        // add the container now
        JSONObject containerJS = new JSONObject();

        containerJS.put("type_1", type1);
        containerJS.put("indicator_1", indicator1);
        containerJS.put("barcode_1", barcode);

        if(!type2.isEmpty()) {
            containerJS.put("type_2", type2);
            containerJS.put("indicator_2", indicator2);
        }

        if(!type3.isEmpty()) {
            containerJS.put("type_3", type3);
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
    public JSONObject createAccessionInstance(String accessionId, String locationURI, String locationNote) throws Exception {
        JSONObject instanceJS = new JSONObject();

        // set the type
        instanceJS.put("instance_type", "accession");

        // add the container now
        JSONObject containerJS = new JSONObject();

        containerJS.put("type_1", "item");
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
    public static void addExtent(JSONArray extentJA, String portion, String type, String extent) throws Exception {
        JSONObject extentJS = new JSONObject();
        extentJS.put("portion", portion);
        extentJS.put("extent_type", type);
        extentJS.put("number", extent);
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
     * Method to add an inclusive date
     * @param dateJA
     * @param begin
     * @param end
     * @throws Exception
     */
    public static void addInclusiveDate(JSONArray dateJA, String label, String begin, String end) throws Exception {
        JSONObject dateJS = new JSONObject();
        dateJS.put("date_type", "inclusive");
        dateJS.put("label", label);
        dateJS.put("begin", begin);
        dateJS.put("end", end);
        dateJA.put(dateJS);
    }

    /**
     * Method to add a bulk date
     * @param dateJA
     * @param begin
     * @param end
     * @throws Exception
     */
    public static void addBulkDate(JSONArray dateJA, String label, String begin, String end) throws Exception {
        JSONObject dateJS = new JSONObject();
        dateJS.put("date_type", "bulk");
        dateJS.put("label", label);
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
        if(noteContent.isEmpty()) return;

        JSONObject noteJS = new JSONObject();

        noteJS.put("jsonmodel_type", "note_multipart");
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
     * Method to return the column number given a character
     *
     * @param column
     * @return
     */
    public static int getColumnNumber(char column) {
        return Character.getNumericValue(column) - 10;
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
     * Main method to test this class
     *
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("Character Number A: " + getColumnNumber('A'));
        System.out.println("Character Number Z: " + getColumnNumber('Z'));
    }
}
