package org.nyu.edu.dlts.models;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Nathan Stevens
 * Date: 3/14/14
 * Time: 10:42 AM
 *
 * A model class for storing rows of excel data which have a parent/child relationship. For example Digital Object
 * and Resource records.
 */
public class RelatedRowData {
    private XSSFSheet xssfSheet;

    private XSSFRow parentRow;

    private ArrayList<XSSFRow> childRowsList;

    private String parentRowId;

    /**
     * The main constructor
     *
     * @param parentRowId
     */
    public RelatedRowData(String parentRowId, XSSFSheet xssfSheet, XSSFRow parentRow) {
        this.parentRowId = parentRowId;
        this.xssfSheet = xssfSheet;
        this.parentRow = parentRow;

        childRowsList = new ArrayList<XSSFRow>();
    }

    public XSSFRow getParentRow() {
        return parentRow;
    }

    public void setParentRow(XSSFRow parentRow) {
        this.parentRow = parentRow;
    }

    public ArrayList<XSSFRow> getChildRowsList() {
        return childRowsList;
    }

    public void setChildRowsList(ArrayList<XSSFRow> childRowsList) {
        this.childRowsList = childRowsList;
    }

    public void addChildRow(XSSFRow xssfRow) {
        childRowsList.add(xssfRow);
    }

    public String getParentRowId() {
        return parentRowId;
    }

    public void setParentRowId(String parentRowId) {
        this.parentRowId = parentRowId;
    }

    public XSSFSheet getXssfSheet() {
        return xssfSheet;
    }

    public void setXssfSheet(XSSFSheet xssfSheet) {
        this.xssfSheet = xssfSheet;
    }
}
