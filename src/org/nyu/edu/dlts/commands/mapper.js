/**
Demo Javascript script to demonstrate importing excel data programatically
into an Archives Space Instance

It is executed within the data migration tool

@author Nathan Stevens
@version 1.0 03/20/2014

Specify the mapping functionality the script provides. change # to @ to 
specify that a certain record type is supported

@name
@subject
@accession
@digitalobject
@resource
*/

// import Java classes we need
importPackage(org.json);
importPackage(org.apache.poi.xssf.usermodel);
importPackage(java.text);
importPackage(org.nyu.edu.dlts.utils);

// Method to return a Java date object
function getDate(dateString) {
	df = new SimpleDateFormat("MM/dd/yyyy");
	return df.parse(dateString);
}


// Method to get the cell data by providing a column number
function getCellDataByNumber(column) {
	cell = record.getCell(column);
	
	if(cell != null) {
		return cell.toString();
	} else {
		return "";
	}	
}

// Method to get the data in a certain column 
function getCellData(column) {
	getCellDataByNumber(MapperUtil.getColumnNumber(column));	
}

// Method to convert the subject record
function convertSubject() {
	// add the source
	recordJS.put("source", getCellData('C'));
	
	// add the terms now
	termsJA = new JSONArray();
	
	termType = getCellData('B');

	for(i = 3; i <= 5; i++) {
		termJS = new JSONObject();	
		termJS.put("term", getCellDataByNumber(i));
    		termJS.put("term_type",termType);
    		termJS.put("vocabulary", recordJS.getString("vocabulary"));
    		termsJA.put(termJS);
    }
    	
	recordJS.put("terms", termsJA);
}

/*
 * This is where code execution starts by first checking that the record 
 * is not null. then it checks the type of record.
 * All functions being called must be above this point.
 */
if(record != null) {	
    if (recordType.equals("subject")) {
    		print("\nConverting Subject ...\n");
    		convertSubject();
    } else {
    		// print error message to say that the record type is not supported
    		print("Record not supported ... " + recordType);
    }
}
