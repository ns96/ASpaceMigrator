/*
 * Created by JFormDesigner on Tue Jul 31 10:12:49 EDT 2012
 */

package org.nyu.edu.dlts;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONException;
import org.json.JSONObject;
import org.nyu.edu.dlts.aspace.ASpaceClient;
import org.nyu.edu.dlts.aspace.ASpaceCopy;
import org.nyu.edu.dlts.aspace.ASpaceMapper;
import org.nyu.edu.dlts.utils.*;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * The main GUI class for the ASpace Data Migration project
 *
 * @author Nathan Stevens
 */
public class dbCopyFrame extends JFrame {
    // used for viewing the mapper scripts
    private CodeViewerDialog codeViewerDialogBeanshell;
    private CodeViewerDialog codeViewerDialogJython;
    private CodeViewerDialog codeViewerDialogJavascript;

    // stores any migration errors
    private String migrationErrors = "";

    // the database copy util for AT to archives space
    private ASpaceCopy ascopy;

    // used to connect connect to apace backend for testing
    private ASpaceClient aspaceClient;

    private boolean copyStopped = false;

    private String mapperScript = "";

    private String defaultMapperScript = "";

    private File scriptFile = null;

    // used for loading mapper scripts and excel file
    final JFileChooser fc = new JFileChooser();

    /**
     * The main constructor
     */
    public dbCopyFrame() {
        initComponents();
        setSampleDataFilename();
    }

    /**
     * Assume we running in the same directory as the jar file
     */

    private void setSampleDataFilename() {
        String currentDirectory  = System.getProperty("user.dir");
        String excelFilename = currentDirectory +"/sample_data/Sample Ingest Data.xlsx";
        excelTextField.setText(excelFilename);
    }

    /**
     * Close this window, and only exit if we are running in stand alone mode
     */
    private void okButtonActionPerformed() {
        setVisible(false);
        System.exit(0);
    }

    /**
     * Method to copy data from AT to archive space. NO longer Used
     */
    private void CopyToASpaceButtonActionPerformed() {
        // reset the error count and error messages
        errorCountLabel.setText("N/A");
        migrationErrors = "";

        // try loading the excel file here
        try {
            String fileName = excelTextField.getText();

            FileInputStream fileInputStream = new FileInputStream(fileName);

            // try loading the work book
            XSSFWorkbook workBook = new XSSFWorkbook(fileInputStream);

            consoleTextArea.append("Loading excel file " + fileName + "\n\n");

            // now call the method to that will actually start the copy process
            startASpaceCopyProcess(workBook);
        } catch (Exception e) {
            consoleTextArea.append("Error loading excel file\n\n");
            e.printStackTrace();
        }

    }

    /**
     * Method to start the a thread that actually copied ASpace records
     *
     */
    private void startASpaceCopyProcess(final XSSFWorkbook workBook) {
        Thread performer = new Thread(new Runnable() {
            public void run() {
                // first disable/enable the relevant buttons
                copyToASpaceButton.setEnabled(false);
                //errorLogButton.setEnabled(false);
                stopButton.setEnabled(true);

                // clear text area and show progress bar
                consoleTextArea.setText("");
                copyProgressBar.setStringPainted(true);
                copyProgressBar.setString("Copying Records ...");
                copyProgressBar.setIndeterminate(true);

                try {
                    // print the connection message
                    consoleTextArea.append("Excel File Opened ...");

                    String host = hostTextField.getText().trim();
                    String admin = adminTextField.getText();
                    String adminPassword = adminPasswordTextField.getText();

                    boolean simulateRESTCalls = simulateCheckBox.isSelected();
                    boolean developerMode = developerModeCheckBox.isSelected();

                    ascopy = new ASpaceCopy(host, admin, adminPassword);
                    ascopy.setMapperScriptType(getMapperScriptType());

                    if(mapperScript.isEmpty()) {
                        ascopy.setMapperScript(defaultMapperScript);
                        indicateSupportedRecords(defaultMapperScript);
                        mapperScriptTextField.setText("Default mapper script loaded ...");
                    } else {
                        ascopy.setMapperScript(mapperScript);
                    }

                    ascopy.setWorkbook(workBook);

                    ascopy.setSimulateRESTCalls(simulateRESTCalls);
                    ascopy.setDeveloperMode(developerMode);

                    // set the reset password, and output console and progress bar
                    ascopy.setOutputConsole(consoleTextArea);
                    ascopy.setProgressIndicators(copyProgressBar, errorCountLabel);
                    ascopy.setCopying(true);

                    // try getting the session and only continue if a valid session is return;
                    if(!simulateRESTCalls && !ascopy.getSession()) {
                        consoleTextArea.append("No session, nothing to do ...\n");
                        reEnableCopyButtons();
                        return;
                    } else {
                        consoleTextArea.append("Administrator authenticated ...\n");
                    }

                    // set the progress bar from doing it's thing since the ascopy class is going to take over
                    copyProgressBar.setIndeterminate(false);

                    boolean globalRecordsExists = false;
                    if(developerMode && ascopy.uriMapFileExist()) {
                        globalRecordsExists = ascopy.loadURIMaps();
                    }

                    // see whether to create a repository record or use the one entered by user
                    String repositoryURI = repositoryURITextField.getText();

                    if(createRepositoryCheckBox.isSelected()) {
                        JSONObject repository = createRepositoryRecord();
                        repositoryURI = ascopy.copyRepositoryRecord(repository);
                        repositoryURITextField.setText(repositoryURI);
                    }

                    if(repositoryURI.isEmpty()) {
                        consoleTextArea.append("No target repository, unable to copy ...\n");
                        reEnableCopyButtons();
                        return;
                    } else {
                        ascopy.setRepositoryURI(repositoryURI);
                    }

                    int locationsSheet = Integer.parseInt(locationsTextField.getText()) - 1;
                    int subjectsSheet = Integer.parseInt(subjectsTextField.getText()) - 1;
                    int namesSheet = Integer.parseInt(namesTextField.getText()) - 1;
                    int accessionSheet = Integer.parseInt(accessionsTextField.getText()) - 1;
                    int digitalObjectSheet = Integer.parseInt(digitalObjectsTextField.getText()) - 1;

                    // now check if we in developer mode, in which case we not going to save
                    // the locations, subjects, names records since they should already be in the
                    // database
                    if(!developerMode || !globalRecordsExists) {
                        if(!copyStopped && locationsSheet >= 0) ascopy.copyLocationRecords(locationsSheet);
                        if(!copyStopped && subjectsSheet >= 0) ascopy.copySubjectRecords(subjectsSheet);
                        if(!copyStopped && namesSheet >= 0) ascopy.copyNameRecords(namesSheet);
                    }

                    if(!copyStopped && accessionSheet > 0) ascopy.copyAccessionRecords(accessionSheet);
                    if(!copyStopped && digitalObjectSheet > 0) ascopy.copyDigitalObjectRecords(digitalObjectSheet);

                    // save the record maps for possible future use
                    ascopy.saveURIMaps();

                    String resourcesSheets = resourcesTextField.getText().trim();
                    if(!copyStopped && !resourcesSheets.isEmpty()) {
                        ascopy.copyResourceRecords(resourcesSheets);
                    }

                    ascopy.cleanUp();

                    // set the number of errors and message now
                    String errorCount = "" + ascopy.getSaveErrorCount();
                    errorCountLabel.setText(errorCount);
                    migrationErrors = ascopy.getSaveErrorMessages() + "\n\nTotal errors: " + errorCount;
                } catch (Exception e) {
                    consoleTextArea.setText("Unrecoverable exception, migration stopped ...\n\n");
                    consoleTextArea.append(ascopy.getCurrentRecordInfo() + "\n\n");
                    consoleTextArea.append(getStackTrace(e));
                    //e.printStackTrace();
                }

                reEnableCopyButtons();
            }
        });

        performer.start();
    }

    /**
     * Method to re-enable the copy buttons
     */
    private void reEnableCopyButtons() {
        // re-enable the buttons the relevant buttons
        copyToASpaceButton.setEnabled(true);
        copyProgressBar.setValue(0);

        if (copyStopped) {
            copyStopped = false;
            copyProgressBar.setString("Cancelled Copy Process ...");
        } else {
            copyProgressBar.setString("Done");
        }
    }

    /**
     * Method to display the error log dialog
     */
    private void errorLogButtonActionPerformed() {
        ImportExportLogDialog logDialog;

        if(ascopy != null && ascopy.isCopying()) {
            logDialog = new ImportExportLogDialog(this, ascopy.getCurrentProgressMessage());
            logDialog.setTitle("Current Data Transfer Errors");
        } else {
            logDialog = new ImportExportLogDialog(this, migrationErrors);
            logDialog.setTitle("Data Transfer Errors");
        }

        logDialog.showDialog();
    }

    /**
     * Method to stop the copy process. Only works when resource are being copied
     */
    private void stopButtonActionPerformed() {
        if(ascopy != null) {
            ascopy.stopCopy();
        }

        copyStopped = true;
        stopButton.setEnabled(false);
    }

    /**
     * A convenient method for view the ASpace json records. It meant to be used for development purposes only
     */
    private void viewRecordButtonActionPerformed() {
        String uri = recordURIComboBox.getSelectedItem().toString();
        String recordJSON = "";

        try {
            if(aspaceClient == null) {
                String host = hostTextField.getText().trim();
                String admin = adminTextField.getText();
                String adminPassword = adminPasswordTextField.getText();

                aspaceClient = new ASpaceClient(host, admin, adminPassword);
                aspaceClient.getSession();
            }

            recordJSON = aspaceClient.getRecordAsJSON(uri, paramsTextField.getText());

            if(recordJSON == null || recordJSON.isEmpty()) {
                recordJSON = aspaceClient.getErrorMessages();
            }
        } catch (Exception e) {
            recordJSON = e.toString();
        }


        CodeViewerDialog codeViewerDialog = new CodeViewerDialog(this, SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT, recordJSON, true, true);
        codeViewerDialog.setTitle("REST ENDPOINT URI: " + uri);
        codeViewerDialog.pack();
        codeViewerDialog.setVisible(true);
    }

    /**
     * Method to set the mapper script file name
     */
    private void loadMapperScriptButtonActionPerformed() {
        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            mapperScriptTextField.setText(file.getAbsolutePath());
            loadMapperScript();
        }
    }

    /**
     * Method to check to see what kind of imports are supported by the mapper script
     */
    private void loadMapperScript() {
        String fileName = mapperScriptTextField.getText();
        File file = new File(fileName);

        if(file.exists()) {
            mapperScript = FileManager.readTextData(file);
            indicateSupportedRecords(null);
            scriptFile = file;
        }
    }

    /**
     * Method to indicate which type of records are supported by a certain script
     */
    private void indicateSupportedRecords(String script) {
        if(script == null) {
            script = mapperScript;
        }

        // now indicate what's supported by this mapper script
        if (script.contains(ASpaceMapper.LOCATION_MAPPER)) {
            locationsLabel.setText("supported");
        } else {
            locationsLabel.setText("not supported");
        }

        if (script.contains(ASpaceMapper.SUBJECT_MAPPER)) {
            subjectsLabel.setText("supported");
        } else {
            subjectsLabel.setText("not supported");
        }

        if (script.contains(ASpaceMapper.NAME_MAPPER)) {
            namesLabel.setText("supported");
        } else {
            namesLabel.setText("not supported");
        }

        if (script.contains(ASpaceMapper.ACCESSION_MAPPER)) {
            accessionsLabel.setText("supported");
        } else {
            accessionsLabel.setText("not supported");
        }

        if (script.contains(ASpaceMapper.DIGITAL_OBJECT_MAPPER)) {
            digitalObjectLabel.setText("supported");
        } else {
            digitalObjectLabel.setText("not supported");
        }

        if (script.contains(ASpaceMapper.RESOURCE_MAPPER)) {
            resourcesLabel.setText("supported");
        } else {
            resourcesLabel.setText("not supported");
        }
    }

    /**
     * Method to updated the mapper script
     *
     * @param text
     */
    public void updateMapperScript(String text) {
        mapperScript = text;
        mapperScriptTextField.setText("Script Loaded From Editor ...");
        indicateSupportedRecords(null);
    }

    /**
     * Method to load the excel file into memory
     */
    private void loadExcelFileButtonActionPerformed() {
        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            excelTextField.setText(file.getAbsolutePath());
        }
    }

    /**
     * Method to open up the code viewer dialog
     */
    private void editScriptButtonActionPerformed() {
        if(beanShellRadioButton.isSelected()) {
            if(mapperScript.isEmpty()) {
                mapperScript = ScriptUtil.getTextForBeanShellScript();
            }

            if (codeViewerDialogBeanshell == null) {
                codeViewerDialogBeanshell = new CodeViewerDialog(this, SyntaxConstants.SYNTAX_STYLE_JAVA, mapperScript, true, false);
            } else {
                codeViewerDialogBeanshell.setCurrentScript(mapperScript);
            }

            codeViewerDialogBeanshell.setScriptFile(scriptFile);

            codeViewerDialogBeanshell.setTitle("BeanShell Mapper Script Editor");
            codeViewerDialogBeanshell.pack();
            codeViewerDialogBeanshell.setVisible(true);
        } else if (pythonRadioButton.isSelected()) {
            if(mapperScript.isEmpty()) {
                mapperScript = ScriptUtil.getTextForJythonScript();
            }

            // must be a python script
            if (codeViewerDialogJython == null) {
                codeViewerDialogJython = new CodeViewerDialog(this, SyntaxConstants.SYNTAX_STYLE_PYTHON, mapperScript, true, false);
            } else {
                codeViewerDialogJython.setCurrentScript(mapperScript);
            }

            codeViewerDialogJython.setScriptFile(scriptFile);

            codeViewerDialogJython.setTitle("Jython Mapper Script Editor");
            codeViewerDialogJython.pack();
            codeViewerDialogJython.setVisible(true);
        } else {
            if(mapperScript.isEmpty()) {
                mapperScript = ScriptUtil.getTextForJavascriptScript();
            }

            // must be a python script
            if (codeViewerDialogJavascript == null) {
                codeViewerDialogJavascript = new CodeViewerDialog(this, SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT, mapperScript, true, false);
            } else {
                codeViewerDialogJavascript.setCurrentScript(mapperScript);
            }

            codeViewerDialogJavascript.setScriptFile(scriptFile);

            codeViewerDialogJavascript.setTitle("Javascript Mapper Script Editor");
            codeViewerDialogJavascript.pack();
            codeViewerDialogJavascript.setVisible(true);
        }
    }

    /**
     * Method to create a repository record
     *
     * @return
     */
    private JSONObject createRepositoryRecord() {
        JSONObject repository = new JSONObject();

        try {
            repository.put("ShortName", repoShortNameTextField.getText());
            repository.put("Name", repoNameTextField.getText());
            repository.put("Code", repoCodeTextField.getText());
            repository.put("URL", repoURLTextField.getText());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return repository;
    }

    /**
     * Method to clear the mapper script
     */
    private void clearMapperScript() {
        mapperScript = "";
        scriptFile = null;
    }

    /**
     * Method to get the mapper script type and load the default mapper script
     *
     * @return
     */
    private String getMapperScriptType() {
        if(beanShellRadioButton.isSelected()) {
            defaultMapperScript = ScriptUtil.getTextForBeanShellScript();
            return ASpaceMapper.BEANSHELL_SCRIPT;
        } else if(pythonRadioButton.isSelected()) {
            defaultMapperScript = ScriptUtil.getTextForJythonScript();
            return ASpaceMapper.JYTHON_SCRIPT;
        } else {
            defaultMapperScript = ScriptUtil.getTextForJavascriptScript();
            return ASpaceMapper.JAVASCRIPT_SCRIPT;
        }
    }

    /**
     * Method to get the string from a stack trace
     *
     * @param throwable The exception
     * @return the string representation of the stack trace
     */
    public static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        apiLabel = new JLabel();
        panel4 = new JPanel();
        label9 = new JLabel();
        beanShellRadioButton = new JRadioButton();
        pythonRadioButton = new JRadioButton();
        javascriptRadioButton = new JRadioButton();
        loadExcelFileButton = new JButton();
        excelTextField = new JTextField();
        loadMapperScriptButton = new JButton();
        mapperScriptTextField = new JTextField();
        editScriptButton = new JButton();
        panel5 = new JPanel();
        createRepositoryCheckBox = new JCheckBox();
        repoShortNameTextField = new JTextField();
        repoNameTextField = new JTextField();
        repoCodeTextField = new JTextField();
        repoURLTextField = new JTextField();
        panel2 = new JPanel();
        label1 = new JLabel();
        label3 = new JLabel();
        label10 = new JLabel();
        locationsTextField = new JTextField();
        locationsLabel = new JLabel();
        label5 = new JLabel();
        subjectsTextField = new JTextField();
        subjectsLabel = new JLabel();
        label4 = new JLabel();
        namesTextField = new JTextField();
        namesLabel = new JLabel();
        label6 = new JLabel();
        accessionsTextField = new JTextField();
        accessionsLabel = new JLabel();
        label7 = new JLabel();
        digitalObjectsTextField = new JTextField();
        digitalObjectLabel = new JLabel();
        label8 = new JLabel();
        resourcesTextField = new JTextField();
        resourcesLabel = new JLabel();
        copyToASpaceButton = new JButton();
        hostLabel = new JLabel();
        hostTextField = new JTextField();
        simulateCheckBox = new JCheckBox();
        adminLabel = new JLabel();
        adminTextField = new JTextField();
        adminPasswordLabel = new JLabel();
        adminPasswordTextField = new JTextField();
        label2 = new JLabel();
        repositoryURITextField = new JTextField();
        developerModeCheckBox = new JCheckBox();
        outputConsoleLabel = new JLabel();
        copyProgressBar = new JProgressBar();
        scrollPane1 = new JScrollPane();
        consoleTextArea = new JTextArea();
        recordURIComboBox = new JComboBox();
        panel1 = new JPanel();
        paramsLabel = new JLabel();
        paramsTextField = new JTextField();
        viewRecordButton = new JButton();
        buttonBar = new JPanel();
        errorLogButton = new JButton();
        saveErrorsLabel = new JLabel();
        errorCountLabel = new JLabel();
        stopButton = new JButton();
        okButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Archives Space Excel Migrator v0.1 (03-21-2014)");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new FormLayout(
                    new ColumnSpec[] {
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC
                    },
                    new RowSpec[] {
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        new RowSpec(RowSpec.TOP, Sizes.DEFAULT, FormSpec.NO_GROW),
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC
                    }));

                //---- apiLabel ----
                apiLabel.setText("  Archives Space Version: v1.0.7");
                apiLabel.setHorizontalTextPosition(SwingConstants.CENTER);
                apiLabel.setFont(new Font("Lucida Grande", Font.BOLD, 14));
                contentPanel.add(apiLabel, cc.xy(1, 1));

                //======== panel4 ========
                {
                    panel4.setLayout(new FormLayout(
                        new ColumnSpec[] {
                            FormFactory.DEFAULT_COLSPEC,
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                        },
                        RowSpec.decodeSpecs("default")));

                    //---- label9 ----
                    label9.setText("Select Mapper Script Type");
                    panel4.add(label9, cc.xy(1, 1));

                    //---- beanShellRadioButton ----
                    beanShellRadioButton.setText("Beanshell");
                    beanShellRadioButton.setSelected(true);
                    beanShellRadioButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            clearMapperScript();
                        }
                    });
                    panel4.add(beanShellRadioButton, cc.xy(3, 1));

                    //---- pythonRadioButton ----
                    pythonRadioButton.setText("Jython");
                    pythonRadioButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            clearMapperScript();
                        }
                    });
                    panel4.add(pythonRadioButton, cc.xy(5, 1));

                    //---- javascriptRadioButton ----
                    javascriptRadioButton.setText("Javascript");
                    javascriptRadioButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            clearMapperScript();
                        }
                    });
                    panel4.add(javascriptRadioButton, cc.xy(7, 1));
                }
                contentPanel.add(panel4, cc.xywh(3, 1, 7, 1));

                //---- loadExcelFileButton ----
                loadExcelFileButton.setText("Load Excel File");
                loadExcelFileButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        loadExcelFileButtonActionPerformed();
                    }
                });
                contentPanel.add(loadExcelFileButton, cc.xy(1, 3));
                contentPanel.add(excelTextField, cc.xywh(3, 3, 5, 1));

                //---- loadMapperScriptButton ----
                loadMapperScriptButton.setText("Load Mapper Script");
                loadMapperScriptButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        loadMapperScriptButtonActionPerformed();
                    }
                });
                contentPanel.add(loadMapperScriptButton, cc.xy(1, 5));

                //---- mapperScriptTextField ----
                mapperScriptTextField.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        loadMapperScript();
                    }
                });
                contentPanel.add(mapperScriptTextField, cc.xywh(3, 5, 5, 1));

                //---- editScriptButton ----
                editScriptButton.setText("Edit");
                editScriptButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        editScriptButtonActionPerformed();
                    }
                });
                contentPanel.add(editScriptButton, cc.xy(9, 5));

                //======== panel5 ========
                {
                    panel5.setLayout(new FormLayout(
                        ColumnSpec.decodeSpecs("default:grow"),
                        new RowSpec[] {
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC
                        }));

                    //---- createRepositoryCheckBox ----
                    createRepositoryCheckBox.setText("Create Repository");
                    panel5.add(createRepositoryCheckBox, cc.xy(1, 1));

                    //---- repoShortNameTextField ----
                    repoShortNameTextField.setText("Repo Short Name 1");
                    panel5.add(repoShortNameTextField, cc.xy(1, 3));

                    //---- repoNameTextField ----
                    repoNameTextField.setText("Repo Name 1");
                    panel5.add(repoNameTextField, cc.xy(1, 5));

                    //---- repoCodeTextField ----
                    repoCodeTextField.setText("Organization Code 1");
                    panel5.add(repoCodeTextField, cc.xy(1, 7));

                    //---- repoURLTextField ----
                    repoURLTextField.setText("http://repository.url.org");
                    panel5.add(repoURLTextField, cc.xy(1, 9));
                }
                contentPanel.add(panel5, cc.xy(1, 7));

                //======== panel2 ========
                {
                    panel2.setLayout(new FormLayout(
                        "default, default:grow, right:default",
                        "default, default, default, fill:default:grow, fill:default:grow, default, fill:default:grow"));

                    //---- label1 ----
                    label1.setText("Record Type");
                    panel2.add(label1, cc.xy(1, 1));

                    //---- label3 ----
                    label3.setText("Spreadsheet Number (starting at 1)");
                    panel2.add(label3, cc.xy(2, 1));

                    //---- label10 ----
                    label10.setText("Locations");
                    panel2.add(label10, cc.xy(1, 2));

                    //---- locationsTextField ----
                    locationsTextField.setText("1");
                    panel2.add(locationsTextField, cc.xy(2, 2));

                    //---- locationsLabel ----
                    locationsLabel.setText("not supported");
                    panel2.add(locationsLabel, cc.xy(3, 2));

                    //---- label5 ----
                    label5.setText("Subjects");
                    panel2.add(label5, cc.xy(1, 3));

                    //---- subjectsTextField ----
                    subjectsTextField.setColumns(2);
                    subjectsTextField.setText("2");
                    panel2.add(subjectsTextField, cc.xy(2, 3));

                    //---- subjectsLabel ----
                    subjectsLabel.setText("not supported");
                    panel2.add(subjectsLabel, cc.xy(3, 3));

                    //---- label4 ----
                    label4.setText("Names");
                    panel2.add(label4, cc.xy(1, 4));

                    //---- namesTextField ----
                    namesTextField.setColumns(12);
                    namesTextField.setText("3");
                    panel2.add(namesTextField, cc.xy(2, 4));

                    //---- namesLabel ----
                    namesLabel.setText("not supported");
                    panel2.add(namesLabel, cc.xy(3, 4));

                    //---- label6 ----
                    label6.setText("Accessions");
                    panel2.add(label6, cc.xy(1, 5));

                    //---- accessionsTextField ----
                    accessionsTextField.setColumns(2);
                    accessionsTextField.setText("4");
                    panel2.add(accessionsTextField, cc.xy(2, 5));

                    //---- accessionsLabel ----
                    accessionsLabel.setText("not supported");
                    panel2.add(accessionsLabel, cc.xy(3, 5));

                    //---- label7 ----
                    label7.setText("Digital Objects");
                    panel2.add(label7, cc.xy(1, 6));

                    //---- digitalObjectsTextField ----
                    digitalObjectsTextField.setColumns(2);
                    digitalObjectsTextField.setText("5");
                    panel2.add(digitalObjectsTextField, cc.xy(2, 6));

                    //---- digitalObjectLabel ----
                    digitalObjectLabel.setText("not supported");
                    panel2.add(digitalObjectLabel, cc.xy(3, 6));

                    //---- label8 ----
                    label8.setText("Resources");
                    panel2.add(label8, cc.xy(1, 7));

                    //---- resourcesTextField ----
                    resourcesTextField.setText("6, 7");
                    resourcesTextField.setColumns(2);
                    panel2.add(resourcesTextField, cc.xy(2, 7));

                    //---- resourcesLabel ----
                    resourcesLabel.setText("not supported");
                    panel2.add(resourcesLabel, cc.xy(3, 7));
                }
                contentPanel.add(panel2, cc.xywh(3, 7, 7, 1));

                //---- copyToASpaceButton ----
                copyToASpaceButton.setText("Copy To Archives Space");
                copyToASpaceButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        CopyToASpaceButtonActionPerformed();
                    }
                });
                contentPanel.add(copyToASpaceButton, cc.xy(1, 9));

                //---- hostLabel ----
                hostLabel.setText("Archives Space Host");
                contentPanel.add(hostLabel, cc.xywh(3, 9, 2, 1));

                //---- hostTextField ----
                hostTextField.setText("http://localhost:8089");
                contentPanel.add(hostTextField, cc.xywh(5, 9, 5, 1));

                //---- simulateCheckBox ----
                simulateCheckBox.setText("Simulate REST Calls");
                simulateCheckBox.setSelected(true);
                contentPanel.add(simulateCheckBox, cc.xy(1, 11));

                //---- adminLabel ----
                adminLabel.setText("Administrator User ID");
                contentPanel.add(adminLabel, cc.xy(3, 11));

                //---- adminTextField ----
                adminTextField.setText("admin");
                contentPanel.add(adminTextField, cc.xywh(5, 11, 2, 1));

                //---- adminPasswordLabel ----
                adminPasswordLabel.setText("Password");
                contentPanel.add(adminPasswordLabel, cc.xy(7, 11));

                //---- adminPasswordTextField ----
                adminPasswordTextField.setText("admin");
                contentPanel.add(adminPasswordTextField, cc.xy(9, 11));

                //---- label2 ----
                label2.setText("Target Repository URI");
                contentPanel.add(label2, cc.xy(3, 13));

                //---- repositoryURITextField ----
                repositoryURITextField.setText("/repositories/2");
                contentPanel.add(repositoryURITextField, cc.xywh(5, 13, 5, 1));

                //---- developerModeCheckBox ----
                developerModeCheckBox.setText("Developer Mode (location/names/subjects records are copied only once, and IDs are randomized)");
                contentPanel.add(developerModeCheckBox, cc.xywh(1, 15, 9, 1));

                //---- outputConsoleLabel ----
                outputConsoleLabel.setText("Output Console:");
                contentPanel.add(outputConsoleLabel, cc.xy(1, 17));
                contentPanel.add(copyProgressBar, cc.xywh(3, 17, 7, 1));

                //======== scrollPane1 ========
                {

                    //---- consoleTextArea ----
                    consoleTextArea.setRows(12);
                    scrollPane1.setViewportView(consoleTextArea);
                }
                contentPanel.add(scrollPane1, cc.xywh(1, 19, 9, 1));

                //---- recordURIComboBox ----
                recordURIComboBox.setModel(new DefaultComboBoxModel(new String[] {
                    "/repositories",
                    "/users",
                    "/subjects",
                    "/agents/families/1",
                    "/agents/people/1",
                    "/agents/corporate_entities/1",
                    "/repositories/2/accessions/1",
                    "/repositories/2/resources/1",
                    "/repositories/2/archival_objects/1",
                    "/config/enumerations"
                }));
                recordURIComboBox.setEditable(true);
                contentPanel.add(recordURIComboBox, cc.xy(1, 21));

                //======== panel1 ========
                {
                    panel1.setLayout(new FlowLayout(FlowLayout.LEFT));

                    //---- paramsLabel ----
                    paramsLabel.setText("Params");
                    panel1.add(paramsLabel);

                    //---- paramsTextField ----
                    paramsTextField.setColumns(20);
                    panel1.add(paramsTextField);
                }
                contentPanel.add(panel1, cc.xywh(3, 21, 3, 1));

                //---- viewRecordButton ----
                viewRecordButton.setText("View");
                viewRecordButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        viewRecordButtonActionPerformed();
                    }
                });
                contentPanel.add(viewRecordButton, cc.xywh(7, 21, 3, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setLayout(new FormLayout(
                    new ColumnSpec[] {
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.GLUE_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.BUTTON_COLSPEC
                    },
                    RowSpec.decodeSpecs("pref")));

                //---- errorLogButton ----
                errorLogButton.setText("View Error Log");
                errorLogButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        errorLogButtonActionPerformed();
                    }
                });
                buttonBar.add(errorLogButton, cc.xy(2, 1));

                //---- saveErrorsLabel ----
                saveErrorsLabel.setText(" Errors: ");
                buttonBar.add(saveErrorsLabel, cc.xy(4, 1));

                //---- errorCountLabel ----
                errorCountLabel.setText("N/A ");
                errorCountLabel.setForeground(Color.red);
                errorCountLabel.setFont(new Font("Lucida Grande", Font.BOLD, 13));
                buttonBar.add(errorCountLabel, cc.xy(6, 1));

                //---- stopButton ----
                stopButton.setText("Cancel Copy");
                stopButton.setEnabled(false);
                stopButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        stopButtonActionPerformed();
                        stopButtonActionPerformed();
                    }
                });
                buttonBar.add(stopButton, cc.xy(9, 1));

                //---- okButton ----
                okButton.setText("Close");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        okButtonActionPerformed();
                    }
                });
                buttonBar.add(okButton, cc.xy(12, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //---- buttonGroup1 ----
        ButtonGroup buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(beanShellRadioButton);
        buttonGroup1.add(pythonRadioButton);
        buttonGroup1.add(javascriptRadioButton);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }


    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel apiLabel;
    private JPanel panel4;
    private JLabel label9;
    private JRadioButton beanShellRadioButton;
    private JRadioButton pythonRadioButton;
    private JRadioButton javascriptRadioButton;
    private JButton loadExcelFileButton;
    private JTextField excelTextField;
    private JButton loadMapperScriptButton;
    private JTextField mapperScriptTextField;
    private JButton editScriptButton;
    private JPanel panel5;
    private JCheckBox createRepositoryCheckBox;
    private JTextField repoShortNameTextField;
    private JTextField repoNameTextField;
    private JTextField repoCodeTextField;
    private JTextField repoURLTextField;
    private JPanel panel2;
    private JLabel label1;
    private JLabel label3;
    private JLabel label10;
    private JTextField locationsTextField;
    private JLabel locationsLabel;
    private JLabel label5;
    private JTextField subjectsTextField;
    private JLabel subjectsLabel;
    private JLabel label4;
    private JTextField namesTextField;
    private JLabel namesLabel;
    private JLabel label6;
    private JTextField accessionsTextField;
    private JLabel accessionsLabel;
    private JLabel label7;
    private JTextField digitalObjectsTextField;
    private JLabel digitalObjectLabel;
    private JLabel label8;
    private JTextField resourcesTextField;
    private JLabel resourcesLabel;
    private JButton copyToASpaceButton;
    private JLabel hostLabel;
    private JTextField hostTextField;
    private JCheckBox simulateCheckBox;
    private JLabel adminLabel;
    private JTextField adminTextField;
    private JLabel adminPasswordLabel;
    private JTextField adminPasswordTextField;
    private JLabel label2;
    private JTextField repositoryURITextField;
    private JCheckBox developerModeCheckBox;
    private JLabel outputConsoleLabel;
    private JProgressBar copyProgressBar;
    private JScrollPane scrollPane1;
    private JTextArea consoleTextArea;
    private JComboBox recordURIComboBox;
    private JPanel panel1;
    private JLabel paramsLabel;
    private JTextField paramsTextField;
    private JButton viewRecordButton;
    private JPanel buttonBar;
    private JButton errorLogButton;
    private JLabel saveErrorsLabel;
    private JLabel errorCountLabel;
    private JButton stopButton;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    /**
     * Main method for testing in stand alone mode
     */
    public static void main(String[] args) {
        dbCopyFrame frame = new dbCopyFrame();
        frame.pack();
        frame.setVisible(true);
    }
}
