package org.nyu.edu.dlts.aspace;

import bsh.EvalError;
import bsh.Interpreter;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nyu.edu.dlts.utils.EnumUtil;
import org.nyu.edu.dlts.utils.MapperUtil;
import org.nyu.edu.dlts.utils.RandomString;
import org.python.util.PythonInterpreter;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 9/5/12
 * Time: 1:41 PM
 *
 * Class to map AT data model to ASPace JSON data model
 */
public class ASpaceMapper {
    // String used when mapping AT access class to groups
    public static final String ACCESS_CLASS_PREFIX = "_AccessClass_";

    // Used to specify the type of mapper scripts
    public static final String BEANSHELL_SCRIPT = "BeanShell";
    public static final String JRUBY_SCRIPT = "Jruby";
    public static final String JYTHON_SCRIPT = "Jython";
    public static final String JAVASCRIPT_SCRIPT = "JavaScript";

    // The utility class used to map to ASpace Enums
    private EnumUtil enumUtil = new EnumUtil();

    // required by some ASpace records
    public String vocabularyURI = "/vocabularies/1";

    // the type of script
    private String mapperScriptType;

    // the script mapper script
    private String mapperScript = null;

    // The script interpreters
    private Interpreter bsi = null;
    private PythonInterpreter pyi = null;
    private ScriptEngine jri = null;
    private ScriptEngine jsi = null;

    // some code used for testing
    private boolean makeUnique = false;

    // initialize the random string generators for use when unique ids are needed
    private RandomString randomString = new RandomString(4);

    // used to store errors
    private ASpaceCopy aspaceCopy;

    // used when generating errors
    private String currentResourceRecordIdentifier;

    // variable names in bean shell script that will indicate whether it can override
    // the default mapping operation with itself
    public static final String SUBJECT_MAPPER = "@subject";
    public static final String NAME_MAPPER = "@name";
    public static final String REPOSITORY_MAPPER = "@repository";
    public static final String LOCATION_MAPPER = "@location";
    public static final String USER_MAPPER = "@user";
    public static final String ACCESSION_MAPPER = "@accession";
    public static final String RESOURCE_MAPPER = "@resource";
    public static final String DIGITAL_OBJECT_MAPPER = "@digitalobject";
    public static final String NOTE_MAPPER = "@note";

    /**
     *  Main constructor
     */
    public ASpaceMapper() { }

    /**
     * Constructor that takes an aspace copy util object
     * @param aspaceCopy
     */
    public ASpaceMapper(ASpaceCopy aspaceCopy) {
        this.aspaceCopy = aspaceCopy;
    }

    /**
     * Set the mapper script
     */
    public void setMapperScript(String mapperScript) {
        this.mapperScript = mapperScript;
    }

    /*
     * Method to initialize the script interpreter
     */
    public void initializeScriptInterpreter(String type) {
        mapperScriptType = type;
        initializeScriptInterpreter();
    }

    /*
     * Method to initialize the script interpreter
     */
    public void initializeScriptInterpreter() {
        if(mapperScriptType.equals(BEANSHELL_SCRIPT)) {
            bsi = new Interpreter();
            jri = null;
            pyi = null;
            jsi = null;
        } else if(mapperScriptType.equals(JRUBY_SCRIPT)) {
            ScriptEngineManager manager = new ScriptEngineManager();
            jri = manager.getEngineByName("jruby");
            pyi = null;
            bsi = null;
            jsi = null;
        } else if(mapperScriptType.equals(JYTHON_SCRIPT)) {
            pyi = new PythonInterpreter();
            bsi = null;
            jri = null;
            jsi = null;
        } else {
            ScriptEngineManager manager = new ScriptEngineManager();
            jsi = manager.getEngineByName("javascript");
            bsi = null;
            jri = null;
            pyi = null;
        }
    }

    /**
     * Method to set the interpreters to null
     */
    public void destroyInterpreter() {
        bsi = null;
        jri = null;
        pyi = null;
        jsi = null;
    }

    /**
     * Used to to generate random ids
     *
     * @param makeUnique
     */
    public void setMakeUnique(boolean makeUnique) {
        this.makeUnique = makeUnique;
    }

    /**
     * Method to run the interpreter
     *
     * @param headerRow
     * @param record
     * @param recordJS
     * @return
     */
    private void runInterpreter(XSSFRow headerRow, XSSFRow record, JSONObject recordJS, String recordType) throws EvalError, ScriptException {
        if (bsi != null) {
            bsi.set("header", headerRow);
            bsi.set("record", record);
            bsi.set("recordJS", recordJS);
            bsi.set("recordType", recordType);
            bsi.eval(mapperScript);
        } else if(jri != null) {
            jri.put("header", headerRow);
            jri.put("record", record);
            jri.put("recordJS", recordJS);
            jri.put("recordType", recordType);
            jri.eval(mapperScript);
        } else if(pyi != null) {
            pyi.set("header", headerRow);
            pyi.set("record", record);
            pyi.set("recordJS", recordJS);
            pyi.set("recordType", recordType);
            pyi.exec(mapperScript);
        } else if(jsi != null) {
            jsi.put("header", headerRow);
            jsi.put("record", record);
            jsi.put("recordJS", recordJS);
            jsi.put("recordType", recordType);
            jsi.eval(mapperScript);
        }
    }

    /**
     * Method to get the corporate agent object from a repository
     *
     * @param repository
     * @return
     */
    public String getCorporateAgent(JSONObject repository) throws JSONException {
        // Main json object, agent_person.rb schema
        JSONObject agentJS = new JSONObject();
        agentJS.put("agent_type", "agent_corporate_entity");

        // hold name information
        JSONArray namesJA = new JSONArray();
        JSONObject namesJS = new JSONObject();

        //add the contact information
        JSONArray contactsJA = new JSONArray();
        JSONObject contactsJS = new JSONObject();

        contactsJS.put("name", repository.get("Name"));
        contactsJS.put("address_1", "Address 1");
        contactsJS.put("address_2", "Address 2");
        contactsJS.put("city", "City");

        // add the country and country code together
        String country = "Country Code";
        contactsJS.put("country", country);

        String postCode = "ZIP Code - ZIP Plus Four";
        contactsJS.put("post_code", postCode);

        String phone = "Phone - Phone Extension";
        contactsJS.put("telephone", phone);
        contactsJS.put("fax", "Fax");
        contactsJS.put("email", "Email");

        contactsJA.put(contactsJS);
        agentJS.put("agent_contacts", contactsJA);

        // add the names object
        String primaryName = repository.getString("Name");
        namesJS.put("source", "local");
        namesJS.put("primary_name", primaryName);
        namesJS.put("sort_name", primaryName);

        namesJA.put(namesJS);
        agentJS.put("names", namesJA);

        return agentJS.toString();
    }

    /**
     * Method to convert a repository record to
     *
     * @param record
     * @return
     * @throws Exception
     */
    public String convertRepository(JSONObject record, String agentURI) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the Archon database Id as an external ID
        MapperUtil.addExternalId("1", json, "repository");

        // get the repo code
        json.put("repo_code", record.get("ShortName"));
        json.put("name", MapperUtil.fixEmptyString(record.getString("Name")));
        json.put("org_code", record.get("Code"));
        json.put("url", MapperUtil.fixUrl(record.getString("URL")));

        if(agentURI != null) {
            json.put("agent_representation", MapperUtil.getReferenceObject(agentURI));
        }

        return json.toString();
    }

    /**
     * Method to convert a location record
     *
     *
     * @param record
     * @param header
     * @return
     * @throws Exception
     */
    public JSONObject convertLocation(XSSFRow header, XSSFRow record) throws Exception {
        // Main json object
        JSONObject recordJS = new JSONObject();

        // add the record Id as an external ID
        String recordId = record.getCell(0).toString().replace(".0", "");
        MapperUtil.addExternalId(recordId, recordJS, "location");

        runInterpreter(header, record, recordJS, "location");

        return recordJS;
    }

    /**
     * Method to convert a subject record
     *
     *
     * @param record
     * @param header
     * @return
     * @throws Exception
     */
    public JSONObject convertSubject(XSSFRow header, XSSFRow record) throws Exception {
        // Main json object
        JSONObject recordJS = new JSONObject();
        recordJS.put("vocabulary", vocabularyURI);

        // add the record Id as an external ID
        String recordId = record.getCell(0).toString().replace(".0", "");
        MapperUtil.addExternalId(recordId, recordJS, "subject");

        runInterpreter(header, record, recordJS, "subject");

        return recordJS;
    }

    /**
     * Method to convert a name record
     *
     * @param  header
     * @param record
     * @return
     * @throws Exception
     */
    public JSONObject convertName(XSSFRow header, XSSFRow record) throws Exception {
        // Main json object
        JSONObject recordJS = new JSONObject();

        // add the record Id as an external ID
        String recordId = record.getCell(0).toString().replace(".0", "");
        MapperUtil.addExternalId(recordId, recordJS, "name");

        runInterpreter(header, record, recordJS, "name");

        return recordJS;
    }

    /**
     * Method to convert an accession record
     *
     * @param  header
     * @param record
     * @return
     * @throws Exception
     */
    public JSONObject convertAccession(XSSFRow header, XSSFRow record) throws Exception {
        // Main json object
        JSONObject recordJS = new JSONObject();

        // add the record Id as an external ID
        String recordId = record.getCell(0).toString().replace(".0", "");
        MapperUtil.addExternalId(recordId, recordJS, "accession");

        runInterpreter(header, record, recordJS, "accession");

        if(makeUnique) {
            recordJS.put("id_0", randomString.nextString());
            recordJS.put("id_1", randomString.nextString());
            recordJS.put("id_2", randomString.nextString());
            recordJS.put("id_3", randomString.nextString());
        }

        return recordJS;
    }

    /**
     * Method to convert a digital object record
     *
     * @param header
     * @param record
     * @return
     */
    public JSONObject convertDigitalObject(XSSFRow header, XSSFRow record) throws Exception {
        // Main json object
        JSONObject recordJS = new JSONObject();

        // add the record Id as an external ID
        String recordId = record.getCell(0).toString().replace(".0", "");
        MapperUtil.addExternalId(recordId, recordJS, "digitalObject");

        runInterpreter(header, record, recordJS, "digitalObject");

        if(makeUnique) {
            recordJS.put("digital_object_id", "Digital Object ID ##" + randomString.nextString());
        }

        return recordJS;
    }

    /**
     * Method to convert a digital object record into a aspace digital object component
     *
     * @param record
     * @return
     */
    public JSONObject convertToDigitalObjectComponent(XSSFRow header, XSSFRow record) throws Exception {
        // Main json object
        JSONObject recordJS = new JSONObject();

        runInterpreter(header, record, recordJS, "digitalObjectComponent");

        if(makeUnique) {
            recordJS.put("component_id", "DO Component ID ##" + randomString.nextString());
        }

        return recordJS;

    }

    /**
     * Method to convert an resource record to json ASpace JSON
     *
     * @param header
     * @param record
     *
     * @return
     * @throws Exception
     */
    public JSONObject convertResource(XSSFRow header, XSSFRow record) throws Exception {
        // Main json object
        JSONObject recordJS = new JSONObject();

        // add the record Id as an external ID
        String recordId = record.getCell(0).toString().replace(".0", "");
        MapperUtil.addExternalId(recordId, recordJS, "resource");

        runInterpreter(header, record, recordJS, "resource");

        if(makeUnique) {
            recordJS.put("id_0", randomString.nextString());
            recordJS.put("id_1", randomString.nextString());
            recordJS.put("id_2", randomString.nextString());
            recordJS.put("id_3", randomString.nextString());
        }

        return recordJS;
    }

    /**
     * Method to convert an resource component record to json ASpace JSON
     *
     * @param header
     * @param record
     * @return
     * @throws Exception
     */
    public JSONObject convertResourceComponent(XSSFRow header, XSSFRow record) throws Exception {
        // Main json object
        JSONObject recordJS = new JSONObject();

        // add the record Id as an external ID
        String recordId = record.getCell(0).toString().replace(".0", "");
        MapperUtil.addExternalId(recordId, recordJS, "resourceComponent");

        runInterpreter(header, record, recordJS, "resourceComponent");

        if(makeUnique) {
            recordJS.put("component_id", "Component ID ##" + randomString.nextString());
        }

        return recordJS;
    }

    /**
     * Method to set the current resource record identifier. Usefull for error
     * message generation
     *
     * @param identifier
     */
    public void setCurrentResourceRecordIdentifier(String identifier) {
        this.currentResourceRecordIdentifier = identifier;
    }
}
