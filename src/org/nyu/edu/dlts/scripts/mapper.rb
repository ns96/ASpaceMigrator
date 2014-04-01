# Demo jruby script to demonstrate importing excel data programatically
# into an Archives Space Instance
#
# It is executed within the data migration tool
#
# @author Nathan Stevens
# @version 1.0 04/01/2014
#
# Specify the mapping functionality the script provides. change # to @ to 
# specify that a certain record type is supported
# 
# @location
# @subject
# @name
# @accession
# @digitalobject
# @resource
#

# import Java classes we need
java_import org.json.JSONObject
java_import org.json.JSONArray
java_import org.apache.poi.xssf.usermodel.XSSFRow
java_import java.text.SimpleDateFormat
java_import org.nyu.edu.dlts.utils.MapperUtil

# Method to return a Java date object
def getDate(dateString)
	df = SimpleDateFormat.new("MM/dd/yyyy")
	return df.parse(dateString)
end


# Method to get the cell data by providing a column number
def getCellDataByNumber(column)
	cell = $record.getCell(column)
	
	if (cell != nil)
		return cell.toString()
	else
		return "";
	end	
end

# Method to get the data in a certain column 
def getCellData(column)
	# need column number where A = 0, so subtract 65 from acii number
	column_num = column.ord - 65
	return getCellDataByNumber(column_num)	
end

# Method to convert a location record
def convertLocation()
	$recordJS.put("building", getCellData('B'));
	$recordJS.put("floor", getCellData('C'));
	$recordJS.put("room", getCellData('D'));
	$recordJS.put("barcode", getCellData('E'));
	$recordJS.put("coordinate_1_label", getCellData('F'));
	$recordJS.put("coordinate_1_indicator", getCellData('G'));
end

# Method to convert the subject record
def convertSubject()
	# add the source
	$recordJS.put("source", getCellData('C'))
	
	# add the terms now
	termsJA = JSONArray.new
	
	termType = getCellData('B')

	for i in 3..5
		termJS = JSONObject.new	
		termJS.put("term", getCellDataByNumber(i))
    		termJS.put("term_type",termType)
    		termJS.put("vocabulary", $recordJS.getString("vocabulary"))
    		termsJA.put(termJS)
    	end
    	
	$recordJS.put("terms", termsJA)
end

# Method to convert the subject record
def convertName()
	# holds name information
	namesJA = JSONArray.new
	namesJS = JSONObject.new

	# add the contact information
    contactsJA = JSONArray.new
    contactsJS = JSONObject.new
    
    info = getCellData('H').split(/\s*,\s*/)
    
    contactsJS.put("address_1", info[0])
    contactsJS.put("city", info[1])
    contactsJS.put("region", info[2])
    contactsJS.put("country", "USA")
    contactsJS.put("post_code", info[3])
    contactsJA.put(contactsJS)
    $recordJS.put("agent_contacts", contactsJA)
    
    # add information for the name type
    nameSource = getCellData('C')
    nameRule = getCellData('D')

    # set values for abstract_name.rb schema
    namesJS.put("dates", getCellData('G'))
    namesJS.put("source", nameSource)
    namesJS.put("rules", nameRule)
    	
    # get the agent type
	type = getCellData('B')
	
    if (type == "person")
    		# set the agent type
    		$recordJS.put("agent_type", "agent_person")
    	
    		primaryName = getCellData('E')
    		
    		# set values for name_person.rb schema
    		namesJS.put("primary_name", primaryName)
    		namesJS.put("name_order", "direct")
    		namesJS.put("sort_name", primaryName)
    		
    		# set the name value for the contact information
    		contactsJS.put("name", primaryName)
    elsif (type == "family")
    		# set the agent type
    		$recordJS.put("agent_type", "agent_family")
    		
    		# set values for name_family.rb schema
    		familyName = getCellData('E')
    		
    		namesJS.put("family_name", familyName)
    		namesJS.put("sort_name", familyName)
    		
    		# set the contact name
    		contactsJS.put("name", familyName);
    	else
    		# set the agent type
    		$recordJS.put("agent_type", "agent_corporate_entity")
    	
    		primaryName = getCellData('E')
    	
    		# set values for name_corporate_entity.rb schema
    		namesJS.put("primary_name", primaryName)
    		namesJS.put("sort_name", primaryName)
    	
    		# set the contact name
    		contactsJS.put("name", primaryName)
    
    	end
    	
    # add the names array and names json objects to main record
    namesJA.put(namesJS)
    $recordJS.put("names", namesJA)	
end

# Method to convert an accession record
def convertAccession()
	$recordJS.put("title", getCellData('C'))
	
	date = getDate(getCellData('D'))
	$recordJS.put("accession_date", date)
	
	# get the ids and make them unique if we in DEBUG mode
	ids = getCellData('B').split(/\./)
	
	$recordJS.put("id_0", ids[0])
	$recordJS.put("id_1", ids[1])
	$recordJS.put("id_2", ids[2])
	$recordJS.put("id_3", ids[3])
	
	# add the linked subjects and names data. This is not used
	# by aspace, just by the data migrator to create the links
	$recordJS.put("linked_subjects", getCellData('F'))
	$recordJS.put("linked_names", getCellData('G'))
	
	# add the processed and right transfer date
	date = getDate(getCellData('H'))
	$recordJS.put("processed_date", date)
	
	date = getDate(getCellData('I'));
	$recordJS.put("copyright_transfer_date", date)
	$recordJS.put("copyright_transfer_note", getCellData('J'))
	
	# add the location id and location note
	$recordJS.put("location_id", getCellData('K'))
	$recordJS.put("location_note", getCellData('L'))
	
	# TODO add a general note
end

# Method to convert a digital object record
def convertDigitalObject()
	$recordJS.put("digital_object_id", getCellData('C'))
	$recordJS.put("title", getCellData('D'))
	
	MapperUtil.addDate($recordJS, getCellData('F'), "digitized")
		
	fileVersionsJA = JSONArray.new
	MapperUtil.addFileVersion(fileVersionsJA, getCellData('G'), getCellData('H'), "none", "none")
	$recordJS.put("file_versions", fileVersionsJA)
	
	# set the digital object type
	$recordJS.put("digital_object_type", "mixed_materials")
	
	# set the restrictions apply
	$recordJS.put("publish", true)
	
	# add the linked subjects and names data. This is not used
	# by aspace, just by the data migrator to create the links
	$recordJS.put("linked_subjects", getCellData('I'))
	$recordJS.put("linked_names", getCellData('J'))
	
	# TODO Add Notes
end

# Method to convert a digital object component
def convertDigitalObjectComponent()
	$recordJS.put("component_id", getCellData('C'))
	$recordJS.put("title", getCellData('D'))
	
	fileVersionsJA = JSONArray.new
	MapperUtil.addFileVersion(fileVersionsJA, getCellData('G'), getCellData('H'), "none", "none")
	$recordJS.put("file_versions", fileVersionsJA)
	
	# add the linked subjects and names data. This is not used
	# by aspace, just by the data migrator to create the links
	$recordJS.put("linked_subjects", getCellData('I'))
	$recordJS.put("linked_names", getCellData('J'))
end

# Method to convert a resource record
def convertResource()
	# check to make sure we have a title
	title = getCellData('E')
	$recordJS.put("title", title)
	
	# add the language code
	$recordJS.put("language", getCellData('F'))
	
	# add the extent array containing
	extentJA = JSONArray.new
	MapperUtil.addExtent(extentJA, "whole", getCellData('G'), getCellData('H'))
	$recordJS.put("extents", extentJA);
	
	# add the date array containing the dates json objects
	dateJA = JSONArray.new
	MapperUtil.addDateExpression(dateJA, getCellData('I'))
	$recordJS.put("dates", dateJA)
	
	# get the ids and make them unique if we in DEBUG mode
	ids = getCellData('C').split(/\./)
	
	$recordJS.put("id_0", ids[0])
	$recordJS.put("id_1", ids[1])
	$recordJS.put("id_2", ids[2])
	$recordJS.put("id_3", ids[3])
	
	# set the level to collection
	$recordJS.put("level", "collection")
	
	# set the type to papers
	$recordJS.put("resource_type", "papers")
	
	# set the publish flag to true
	$recordJS.put("publish", true)
	
	# add the linked subjects, names, and accession data. This is not used
	# by aspace, just by the data migrator to create the links
	$recordJS.put("linked_subjects", getCellData('J'))
	$recordJS.put("linked_names", getCellData('K'))
	$recordJS.put("linked_accessions", getCellData('L'))
	
	# add the instance now
	$recordJS.put("analog_instances", getCellData('M'))
	$recordJS.put("digital_instances", getCellData('N'))
	
	# TO-DO add the notes
end

# Method to convert a resource record
def convertResourceComponent()
	# check to make sure we have a title
	title = getCellData('E')
	$recordJS.put("title", title)
	
	# add the language code
	$recordJS.put("language", getCellData('F'))
	
	# add the extent array if needed
	if (!getCellData('G').empty? && !getCellData('H').empty?)
		extentJA = JSONArray.new
		MapperUtil.addExtent(extentJA, "whole", getCellData('G'), getCellData('H'))
		$recordJS.put("extents", extentJA)
	end
	
	# add the date array if needed
	if (!getCellData('I').empty?)
		dateJA = JSONArray.new
		MapperUtil.addDateExpression(dateJA, getCellData('I'))
		$recordJS.put("dates", dateJA)
	end
	
	# add the level
	$recordJS.put("level", getCellData('D'))
	
	# add the component id
	$recordJS.put("component_id", getCellData('C'))
	
	# set the publish flag to true
	$recordJS.put("publish", true)
	
	# add the linked subjects, names, and accession data. This is not used
	# by aspace, just by the data migrator to create the links
	$recordJS.put("linked_subjects", getCellData('J'))
	$recordJS.put("linked_names", getCellData('K'))
	$recordJS.put("linked_accessions", getCellData('L'))
	
	# add the instance now
	$recordJS.put("analog_instances", getCellData('M'))
	$recordJS.put("digital_instances", getCellData('N'))
end

# This is where code execution starts by first checking that the record 
# is not null. then it checks the type of record.
# All functions being called must be above this point.
if ($record != nil)
	if ($recordType == "location")
    		print("\nConverting Location ...\n")
    		convertLocation()
    	elsif ($recordType == "subject")
    		print("\nConverting Subject ...\n")
    		convertSubject()
    	elsif ($recordType == "name")
    		print("\nConverting Name ...\n")
    		convertName()
    	elsif ($recordType == "accession")
    		print("\nConverting Accession ...\n")
    		convertAccession()
    	elsif ($recordType == "digitalObject")
    		print("\nConverting Digital Object ...\n")
    		convertDigitalObject()
    elsif ($recordType == "digitalObjectComponent")
    		print("\nConverting Digital Object Component ...\n")
    		convertDigitalObjectComponent()
    	elsif ($recordType == "resource")
    		print("\nConverting Resource ...\n")
    		convertResource()
    elsif($recordType == "resourceComponent")
    		print("\nConverting Resource Component...\n")
    		convertResourceComponent()
    	else
    		#print error message to say that the record type is not supported
    		print("Record not supported ... " + $recordType)
    	end    	
end
