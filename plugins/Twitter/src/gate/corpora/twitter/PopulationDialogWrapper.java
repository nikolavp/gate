/*
 *  Copyright (c) 1995-2014, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *  
 *  $Id$
 */
package gate.corpora.twitter;

import gate.gui.ListEditorDialog;
import gate.gui.MainFrame;
import gate.swing.XJFileChooser;
import gate.util.ExtensionFileFilter;
import gate.util.Strings;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;


public class PopulationDialogWrapper  {
  protected JDialog dialog;
  protected PopulationConfig config;
  private JTextField encodingField;
  private JCheckBox checkbox;
  private JFileChooser chooser;
  private List<URL> fileUrls;
  private ListEditor featureKeysEditor, contentKeysEditor;

  public static final String RESOURCE_CODE = "twitter.population";
  private static final Logger logger = Logger.getLogger(PopulationDialogWrapper.class.getName());

  
  public PopulationDialogWrapper() {
    config = new PopulationConfig();
    
    dialog = new JDialog(MainFrame.getInstance(), "Populate from Twitter JSON", true);
    MainFrame.getGuiRoots().add(dialog);
    dialog.getContentPane().setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
    dialog.add(Box.createVerticalStrut(3));
    
    Box encodingBox = Box.createHorizontalBox();
    JLabel encodingLabel = new JLabel("Encoding:");
    encodingField = new JTextField(config.getEncoding());
    encodingBox.add(encodingLabel);
    encodingBox.add(encodingField);
    dialog.add(encodingBox);
    dialog.add(Box.createVerticalStrut(4));

    // Default is now 1 tweet per document; changed in PopulationConfig's
    // default constructor.
    Box checkboxBox = Box.createHorizontalBox();
    checkboxBox.setToolTipText("If unchecked, one document per file");
    JLabel checkboxLabel = new JLabel("One document per tweet");
    checkbox = new JCheckBox();
    checkbox.setSelected(config.getOneDocCheckbox());
    checkboxBox.add(checkboxLabel);
    checkboxBox.add(Box.createHorizontalGlue());
    checkboxBox.add(checkbox);
    dialog.add(checkboxBox);
    dialog.add(Box.createVerticalStrut(4));
    
    contentKeysEditor = new ListEditor("Content keys: ", config.getContentKeys());
    contentKeysEditor.setToolTipText("JSON key paths to be turned into DocumentContent");
    dialog.add(contentKeysEditor);
    dialog.add(Box.createVerticalStrut(4));
    
    featureKeysEditor = new ListEditor("Feature keys: ", config.getFeatureKeys());
    featureKeysEditor.setToolTipText("JSON key paths to be turned into Tweet annotation features");
    dialog.add(featureKeysEditor);
    dialog.add(Box.createVerticalStrut(6));
    
    Box configPersistenceBox = Box.createHorizontalBox();
    configPersistenceBox.add(Box.createHorizontalGlue());
    JButton loadConfigButton = new JButton("Load configuration");
    loadConfigButton.setToolTipText("Replace the configuration above with a previously saved one");
    loadConfigButton.addActionListener(new LoadConfigListener(this));
    configPersistenceBox.add(loadConfigButton);
    configPersistenceBox.add(Box.createHorizontalGlue());
    JButton saveConfigButton = new JButton("Save configuration");
    saveConfigButton.setToolTipText("Save the configuration above for re-use");
    saveConfigButton.addActionListener(new SaveConfigListener(this));
    configPersistenceBox.add(saveConfigButton);
    configPersistenceBox.add(Box.createHorizontalGlue());
    
    dialog.add(configPersistenceBox);
    dialog.add(Box.createVerticalStrut(5));
    
    dialog.add(new JSeparator(SwingConstants.HORIZONTAL));
    dialog.add(Box.createVerticalStrut(2));
    
    //chooser = MainFrame.getFileChooser();
    chooser = new JFileChooser();
    // TODO Fix this to get GATE to remember last location.
    //chooser.setResource(RESOURCE_CODE);
    chooser.setFileSelectionMode(XJFileChooser.FILES_ONLY);
    chooser.setMultiSelectionEnabled(true);
    chooser.setDialogTitle("Select a Twitter JSON file");
    chooser.resetChoosableFileFilters();
    chooser.setAcceptAllFileFilterUsed(false);
    ExtensionFileFilter filter = new ExtensionFileFilter("Twitter JSON files (*.json)", "json");
    chooser.addChoosableFileFilter(filter);
    chooser.setFileFilter(filter);
    chooser.setApproveButtonText("Populate");
    chooser.addActionListener(new PopulationDialogListener(this));

    dialog.add(chooser);
    dialog.pack();
    dialog.setLocationRelativeTo(dialog.getOwner());
    dialog.setVisible(true);
  }
  
  
  public String getEncoding() {
    return this.config.getEncoding();
  }
  
  public List<URL> getFileUrls() throws MalformedURLException {
    return this.fileUrls;
  }

  public int getTweetsPerDoc() {
    return this.config.getTweetsPerDoc();
  }
  
  public List<String> getContentKeys() {
    return this.config.getContentKeys();
  }
  
  public List<String> getFeatureKeys() {
    return this.config.getFeatureKeys();
  }
  
  
  protected void setNewConfig(PopulationConfig newConfig) {
    this.config = newConfig;
    this.updateGui();
  }
  
  protected void updateConfig() {
    this.config.setTweetsPerDoc(this.checkbox.isSelected() ? 1 : 0);
    this.config.setContentKeys(this.contentKeysEditor.getValues());
    this.config.setFeatureKeys(this.featureKeysEditor.getValues());
    this.config.setEncoding(this.encodingField.getText());
  }
  
  
  protected void updateGui() {
    this.encodingField.setText(config.getEncoding());
    this.contentKeysEditor.setValues(config.getContentKeys());
    this.featureKeysEditor.setValues(config.getFeatureKeys());
    this.checkbox.setSelected(config.getOneDocCheckbox());
  }
  
  
  protected void loadFile()  {
    updateConfig();

    try {
      this.fileUrls = new ArrayList<URL>();
      for (File file : this.chooser.getSelectedFiles()) {
        this.fileUrls.add(file.toURI().toURL());
      }
    }
    catch (MalformedURLException e) {
      logger.warn("Error loading file", e);
    }
    finally {
      this.dialog.dispose();
    }
  }

  
  protected void cancel() {
    this.dialog.dispose();
  }
  
}


class PopulationDialogListener implements ActionListener {

  private PopulationDialogWrapper dialog;
  
  public PopulationDialogListener(PopulationDialogWrapper dialog) {
    this.dialog = dialog;
  }

  
  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getActionCommand().equals(XJFileChooser.APPROVE_SELECTION)){
      this.dialog.loadFile();
    }
    else {
      this.dialog.cancel();
    }
  }
  
}


class ListEditor extends JPanel {
  private static final long serialVersionUID = -1578463259277343578L;

  private JButton listButton;
  private ListEditorDialog listEditor;
  private List<String> values;
  private JLabel label;
  private JTextField field;
  
  @Override
  public void setToolTipText(String text) {
    super.setToolTipText(text);
    label.setToolTipText(text);
    field.setToolTipText(text);
  }
  
  
  public ListEditor(String labelString, List<String> initialValues) {
    label = new JLabel(labelString);
    field = new JTextField();
    values = initialValues;
    field.setText(Strings.toString(initialValues));
    field.setEditable(false);
        
    listEditor = new ListEditorDialog(SwingUtilities.getAncestorOfClass(
        Window.class, this), values, List.class, String.class.getName());

    listButton = new JButton(MainFrame.getIcon("edit-list"));
    listButton.setToolTipText("Edit the list");
    
    listButton.addActionListener(new ActionListener() {
      @SuppressWarnings("unchecked")
      public void actionPerformed(ActionEvent e) {
        List<?> returnedList = listEditor.showDialog();
        if(returnedList != null) {
          values = (List<String>) returnedList;
          field.setText(Strings.toString(returnedList));
        }
      }
    });
    
    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    this.add(label);
    this.add(field);
    this.add(listButton);
  }
  
  
  public List<String> getValues() {
    return this.values;
  }
  
  public void setValues(List<String> values) {
    this.values = values;
    this.field.setText(Strings.toString(values));
  }
  
}
