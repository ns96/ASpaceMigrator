package org.nyu.edu.dlts.utils;

import bsh.EvalError;
import bsh.Interpreter;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.nyu.edu.dlts.aspace.ASpaceCopy;
import org.nyu.edu.dlts.aspace.ASpaceMapper;
import org.python.util.PythonInterpreter;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A utility class for doing basic clean-up operations on excel using
 * scripts
 *
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 10/28/14
 * Time: 12:51 PM
 */
public class ExcelUtils {
    // The ASpaceCopy object
    private ASpaceCopy aspaceCopy;

    // The Excel work book
    private XSSFWorkbook workBook;

    // the type of script
    private String mapperScriptType;

    // the script mapper script
    private String mapperScript = null;

    // The script interpreters
    private Interpreter bsi = null;
    private PythonInterpreter pyi = null;
    private ScriptEngine jri = null;
    private ScriptEngine jsi = null;

    // some predefine task
    public static final String TASK_PREPROCCESS = "preprocess";

    /**
     * The default constructor
     */
    public ExcelUtils() { }

    /**
     * Method to set the aspace copy object
     * @param aspaceCopy
     */
    public void setASpaceCopy(ASpaceCopy aspaceCopy) {
        this.aspaceCopy = aspaceCopy;
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
        if(mapperScriptType.equals(ASpaceMapper.BEANSHELL_SCRIPT)) {
            bsi = new Interpreter();
            jri = null;
            pyi = null;
            jsi = null;
        } else if(mapperScriptType.equals(ASpaceMapper.JRUBY_SCRIPT)) {
            ScriptEngineManager manager = new ScriptEngineManager();
            jri = manager.getEngineByName("jruby");
            pyi = null;
            bsi = null;
            jsi = null;
        } else if(mapperScriptType.equals(ASpaceMapper.JYTHON_SCRIPT)) {
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
     * Method to run the interpreter
     *
     * @param parentRow
     * @param record
     * @return
     */
    private void runInterpreter(Object parentRow, Object record, String recordType, String task) throws EvalError, ScriptException {
        if (bsi != null) {
            bsi.set("header", parentRow);
            bsi.set("record", record);
            bsi.set("recordType", recordType);
            bsi.set("task", task);
            bsi.eval(mapperScript);
        } else if(jri != null) {
            jri.put("header", parentRow);
            jri.put("record", record);
            jri.put("recordType", recordType);
            jri.put("task", task);
            jri.eval(mapperScript);
        } else if(pyi != null) {
            pyi.set("header", parentRow);
            pyi.set("record", record);
            pyi.set("recordType", recordType);
            pyi.set("task", task);
            pyi.exec(mapperScript);
        } else if(jsi != null) {
            jsi.put("parentRecord", parentRow);
            jsi.put("record", record);
            jsi.put("recordType", recordType);
            jsi.put("task", task);
            jsi.eval(mapperScript);
        }
    }

    /**
     * Method to merge and cleanup row data in a spreadsheet based on logic found
     * in the mapper script.
     *
     * @param sheetNumber
     * @return
     */
    public ArrayList<XSSFRow> cleanRowData(int sheetNumber, String recordType) throws ScriptException, EvalError {
        XSSFSheet xssfSheet = workBook.getSheetAt(sheetNumber);

        ArrayList<XSSFRow> rowList = new ArrayList<XSSFRow>();

        Iterator rowIterator = xssfSheet.rowIterator();

        // this hold the parent row
        XSSFRow parentRow = null;

        int rowNumber = 0;
        while (rowIterator.hasNext()) {
            rowNumber++;

            XSSFRow xssfRow = (XSSFRow) rowIterator.next();
            XSSFCell cell = xssfRow.getCell(0);

            // skip the header row
            if(rowNumber == 1) {
                continue;
            }

            String id = "-1";

            // this is a parent row
            if (cell != null && !cell.toString().trim().isEmpty()) {
                parentRow = xssfRow;
                runInterpreter(parentRow, null, recordType, TASK_PREPROCCESS);
                rowList.add(xssfRow);
                id = parentRow.getCell(0).toString().replace(".0", "");
            } else {
                runInterpreter(parentRow, xssfRow, recordType, TASK_PREPROCCESS);
            }

            print("Pre-Processed Row # " + rowNumber + "\tID: " + id);
        }

        return rowList;
    }

    /**
     * Method to merge and cleanup row data in a spreadsheet based on logic found
     * in the mapper script.
     *
     * @param record The row data to clean up
     * @param recordType The type of record we cleaning up
     *
     * @return
     */
    public void cleanRowData(XSSFRow record, String recordType) throws ScriptException, EvalError {
        runInterpreter(null, record, recordType, TASK_PREPROCCESS);
    }

    /**
     * Method to print out a message to the UI console to shell
     *
     * @param message
     */
    private void print(String message) {
        if(aspaceCopy != null) {
            aspaceCopy.print(message);
        } else {
            System.out.println(message);
        }
    }
}
