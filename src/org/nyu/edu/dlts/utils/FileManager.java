package org.nyu.edu.dlts.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

/**
 * This utility class defines methods for saving script data to the database
 * or to the file system
 *
 * <p/>
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: Oct 8, 2011
 * Time: 8:35:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileManager {
    /**
     * Method to get script data that was saved to a binary file
     * 
     * @param file
     * @return
     */
    public static Object getUriMapData(File file) {
        FileInputStream fis = null;
		ObjectInputStream in = null;

        try {
			fis = new FileInputStream(file);
			in = new ObjectInputStream(fis);
			Object object = in.readObject();
			in.close();

            return object;
		} catch (IOException ex) {
            System.out.println("Error opening file " + file.getName());
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}

        // we got here so just return null
        return null;
    }

    /**
     * Method to save script data to a binary file
     * 
     * @param file
     * @param scriptData
     * @throws Exception
     */
    public static void saveUriMapData(File file, Object scriptData) throws Exception {
        FileOutputStream fos = null;
		ObjectOutputStream out = null;

	    fos = new FileOutputStream(file);
		out = new ObjectOutputStream(fos);
		out.writeObject(scriptData);
		out.close();
    }

    /**
     * Method to save json text to a file
     *
     * @param file
     * @param textData
     */
    public static void saveTextData(File file, String textData) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(textData);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) { }
        }
    }

    /**
     * Method to read a text file
     *
     * @param file
     */
    public static String readTextData(File file) {
        BufferedReader br = null;
        String textData = null;

        try {
            br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append('\n');
                line = br.readLine();
            }

            textData = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return textData;
    }

    /**
     * Method to return a json object
     *
     * @param file
     * @return
     */
    public static JSONObject getJSONObject(File file) {
        String jsonText = readTextData(file);
        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(jsonText);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }
}
