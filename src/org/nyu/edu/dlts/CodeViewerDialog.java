/*
 * Created by JFormDesigner on Thu Jan 03 10:26:44 EST 2013
 */

package org.nyu.edu.dlts;

import java.awt.event.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import org.json.JSONException;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.json.JSONObject;
import org.nyu.edu.dlts.aspace.ASpaceClient;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Simple dialog for viewing or editing source code with syntax highlighting
 *
 * @author Nathan Stevens
 */
public class CodeViewerDialog extends JDialog {
    private RSyntaxTextArea textArea;
    private boolean editable = false;


    private ASpaceClient aspaceClient;

    private dbCopyFrame dbcopyFrame;

    /**
     * Constructor which code is past in
     *
     * @param dbcopyFrame
     * @param code
     * @param syntaxStyle
     */
    public CodeViewerDialog(dbCopyFrame dbcopyFrame, String syntaxStyle,  String code, boolean editable, boolean allowRecordTest) {
        super(dbcopyFrame);
        initComponents();

        this.dbcopyFrame = dbcopyFrame;

        this.editable = editable;

        // add the syntax area now
        textArea = new RSyntaxTextArea(30, 100);
        textArea.setSyntaxEditingStyle(syntaxStyle);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setEditable(editable);
        textArea.setText(code);

        RTextScrollPane sp = new RTextScrollPane(textArea);
        sp.setFoldIndicatorEnabled(true);

        contentPanel.add(sp, BorderLayout.CENTER);

        // Make the components for unit testing a json using the RecordTestServlet
        if(allowRecordTest) {
            scrollPane1.setVisible(true);
            recordTestPanel.setVisible(true);
        }

        // make sure we open this window somewhere that make sense
        setLocation(dbcopyFrame.getLocationOnScreen());
    }



    /**
     * Method to set the script that is displayed
     *
     * @param script
     */
    public void setCurrentScript(String script) {
        textArea.setText(script);
    }

    /**
     * Method to return the current script, for example after it been edited
     *
     * @return The script
     */
    public String getCurrentScript() {
        return textArea.getText();
    }

    /**
     * Close the dialog when the window is closed
     */
    private void okButtonActionPerformed() {
        setVisible(false);

        if(!editable) {
            dispose();
        }
    }

    /**
     * Method to store a json record to test against
     */
    private void postButtonActionPerformed() {
        String testHost = "http";
        String jsonText = textArea.getText();

        try {
            String message = "";

            message = aspaceClient.post(testHost, jsonText, null, "Test Record");

            messageTextArea.append(message + "\n");
        } catch(Exception e) {
            if(e instanceof JSONException) {
                messageTextArea.setText("Invalid JSON Record");
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to delete a record on the ASpace backend.  This is really for development purposes
     */
    private void deleteButtonActionPerformed() {
        try {
            JSONObject jsonObject = new JSONObject(textArea.getText());
            String uri = jsonObject.getString("uri");
            String message = aspaceClient.deleteRecord(uri);
            messageTextArea.setText(message);
        } catch (JSONException e) {
            messageTextArea.setText("Invalid JSON record");
        } catch (Exception e) {
            messageTextArea.setText("Error deleting JSON record");
            e.printStackTrace();
        }
    }

    /**
     * Updated the script in the main program
     */
    private void updateButtonActionPerformed() {
        dbcopyFrame.updateMapperScript(textArea.getText());
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        scrollPane1 = new JScrollPane();
        messageTextArea = new JTextArea();
        buttonBar = new JPanel();
        recordTestPanel = new JPanel();
        openButton = new JButton();
        saveButton = new JButton();
        updateButton = new JButton();
        postToASpaceButton = new JButton();
        okButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Code Viewer");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout());

                //======== scrollPane1 ========
                {

                    //---- messageTextArea ----
                    messageTextArea.setRows(4);
                    messageTextArea.setEditable(false);
                    scrollPane1.setViewportView(messageTextArea);
                }
                contentPanel.add(scrollPane1, BorderLayout.SOUTH);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0};

                //======== recordTestPanel ========
                {
                    recordTestPanel.setLayout(new FormLayout(
                        new ColumnSpec[] {
                            FormFactory.DEFAULT_COLSPEC,
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC,
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC,
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC
                        },
                        RowSpec.decodeSpecs("default")));

                    //---- openButton ----
                    openButton.setText("Open");
                    recordTestPanel.add(openButton, cc.xy(1, 1));

                    //---- saveButton ----
                    saveButton.setText("Save");
                    recordTestPanel.add(saveButton, cc.xy(3, 1));

                    //---- updateButton ----
                    updateButton.setText("Update");
                    updateButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            updateButtonActionPerformed();
                        }
                    });
                    recordTestPanel.add(updateButton, cc.xy(5, 1));

                    //---- postToASpaceButton ----
                    postToASpaceButton.setText("Post Record");
                    recordTestPanel.add(postToASpaceButton, cc.xy(9, 1));
                }
                buttonBar.add(recordTestPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- okButton ----
                okButton.setText("Close");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        okButtonActionPerformed();
                    }
                });
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JScrollPane scrollPane1;
    private JTextArea messageTextArea;
    private JPanel buttonBar;
    private JPanel recordTestPanel;
    private JButton openButton;
    private JButton saveButton;
    private JButton updateButton;
    private JButton postToASpaceButton;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables


    /**
     * Method to setup the ASpace client
     *
     * @param aspaceClient
     */
    public void setASpaceClient(ASpaceClient aspaceClient) {
        this.aspaceClient = aspaceClient;
    }
}
