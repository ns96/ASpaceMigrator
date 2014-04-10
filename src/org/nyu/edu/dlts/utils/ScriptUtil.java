package org.nyu.edu.dlts.utils;

import org.nyu.edu.dlts.dbCopyFrame;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 3/11/14
 * Time: 1:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScriptUtil {
    public static final String BEANSHELL_SCRIPT = "mapper.bsh";
    public static final String JRUBY_SCRIPT = "mapper.rb";
    public static final String JYTHON_SCRIPT = "mapper.py";
    public static final String JAVASCRIPT_SCRIPT = "mapper.js";

    /**
     * Method to return the String of a script that is stored in the classpath. This just calls the
     * default package where scripts are stored relative to the dbCopyFrame class
     *
     * @return
     */
    public static String getTextForBeanShellScript() {
        return getTextForBuiltInScript("scripts/", BEANSHELL_SCRIPT);
    }

    /**
     * Method to return the String of a script that is stored in the classpath. This just calls the
     * default package where scripts are stored relative to the dbCopyFrame class
     *
     * @return
     */
    public static String getTextForJRubyScript() {
        return getTextForBuiltInScript("scripts/", JRUBY_SCRIPT);
    }

    /**
     * Method to return the String of a script that is stored in the classpath. This just calls the
     * default package where scripts are stored relative to the dbCopyFrame class
     *
     * @return
     */
    public static String getTextForJythonScript() {
        return getTextForBuiltInScript("scripts/", JYTHON_SCRIPT);
    }

    /**
     * Method to return the String of a script that is stored in the classpath. This just calls the
     * default package where scripts are stored relative to the dbCopyFrame class
     *
     * @return
     */
    public static String getTextForJavascriptScript() {
        return getTextForBuiltInScript("scripts/", JAVASCRIPT_SCRIPT);
    }

    /**
     * Method to read in a text file to a string from the classpath
     *
     * @param scriptName
     * @return
     */
    public static String getTextForBuiltInScript(String packageName, String scriptName) {
        try {
            InputStream is = dbCopyFrame.class.getResourceAsStream(packageName + scriptName);

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }

            is.close();

            return sb.toString();
        } catch(Exception e) {
            e.printStackTrace();
            return "print (\"Error Loading Built In Script\")";
        }
    }
}
