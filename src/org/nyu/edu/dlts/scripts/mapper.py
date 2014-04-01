# Demo Python script to demonstrate importing excel data programatically
# into an Archives Space Instance
#
# It is executed within the data migration tool
#
# @author Nathan Stevens
# @version 1.0 03/19/2014
#
# Specify the mapping functionality the script provides. change # to @ to 
# specify that a certain record type is supported
#
##location
##name
#@subject
##accession
##digitalobject
##resource

# import the Java classes we need now
from org.json import *
from org.apache.poi.xssf.usermodel import *
from org.nyu.edu.dlts.utils import MapperUtil
from java.text import SimpleDateFormat

# function that return a Java Data Object
def getDate(dateString):
	df = SimpleDateFormat("MM/dd/yyyy");
	return df.parse(dateString)
	
# function to get the data in a certain column 
def getCellDataByNumber(column):
	cell = record.getCell(column)
	return cell.toString()

# function to get the data in a certain column 
def getCellData(column):
	column_number = ord(column) - 65
	return getCellDataByNumber(column_number)	

# function to convert the subject record 
def convertSubject():
	# add the source
	recordJS.put("source", getCellData('C'))
	
	# add the terms now
	termsJA = JSONArray()
	termType = getCellData('B')
	
	for i in range(3,5):
		termJS = JSONObject()
		termJS.put("term", getCellDataByNumber(i))
		termJS.put("term_type",termType)
		termJS.put("vocabulary", recordJS.getString("vocabulary"))
		termsJA.put(termJS)
	
	recordJS.put("terms", termsJA)
	

# This is where code execution starts by first checking that the record 
# is not null. then it checks the type of record.
# All functions being called must be above this point.
if (record is not None):	
	if (recordType == "subject"):
    		convertSubject();
    	else:
    		# just set the result to true since this record is not supported
    		print("Record not supported ... " + recordType)
