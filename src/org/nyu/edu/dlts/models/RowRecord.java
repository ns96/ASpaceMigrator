package org.nyu.edu.dlts.models;

import org.apache.poi.xssf.usermodel.XSSFRow;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A an object to allow storing of excel rows into a db40 for increase performance
 * and reduce memory usage
 *
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 8/6/14
 * Time: 9:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class RowRecord {
    private String rowId;
    private String uniqueId;
    private String recordType;
    private String parentRowId;

    // an array list is used to store row records from excel
    private ArrayList<String> record;

    // hashmap used to store key value records from an XML file
    private HashMap<String, String> recordMap;

    /**
     * The main constructor which takes an array list to store the row record
     */
    public RowRecord(String recordType, String rowId, String parentRowId, ArrayList<String> record) {
        this.recordType = recordType;
        this.rowId = rowId;
        this.parentRowId = parentRowId;
        this.record = record;
    }

    /**
     * The main constructor which takes an hashmap to store the row record
     */
    public RowRecord(String recordType, String rowId, HashMap<String, String> recordMap) {
        this.recordType = recordType;
        this.rowId = rowId;
        this.recordMap = recordMap;
    }

    public String getRowId() {
        return rowId;
    }

    public void setRowId(String rowId) {
        this.rowId = rowId;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getParentRowId() {
        return parentRowId;
    }

    public void setParentRowId(String parentRowId) {
        this.parentRowId = parentRowId;
    }

    public ArrayList<String> getRecord() {
        return record;
    }

    public void setRecord(ArrayList<String> record) {
        this.record = record;
    }

    public HashMap<String, String> getRecordMap() {
        return recordMap;
    }

    public void setRecordMap(HashMap<String, String> recordMap) {
        this.recordMap = recordMap;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String get(int i) {
        return record.get(i);
    }

    public String get(String key) {
        return recordMap.get(key);
    }
}
