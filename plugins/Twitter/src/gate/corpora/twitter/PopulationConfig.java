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


import gate.Gate;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFileChooser;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;


public class PopulationConfig   {
  private String encoding;
  private List<String> featureKeys, contentKeys;
  private int tweetsPerDoc;
  
  public boolean getOneDocCheckbox() {
    return this.tweetsPerDoc == 1;
  }
  
  public int getTweetsPerDoc() {
    return this.tweetsPerDoc;
  }

  public void setTweetsPerDoc(int tpd) {
    this.tweetsPerDoc = tpd;
  }
  
  public String getEncoding() {
    return this.encoding;
  }
  
  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }
  
  public List<String> getFeatureKeys() {
    return this.featureKeys;
  }
  
  public void setFeatureKeys(List<String> keys) {
    this.featureKeys = keys;
  }

  public List<String> getContentKeys() {
    return this.contentKeys;
  }
  
  public void setContentKeys(List<String> keys) {
    this.contentKeys = keys;
  }

  /** 
   * Constructor with defaults.
   */
  public PopulationConfig() {
    this.tweetsPerDoc = 1;
    // Default changed from 0 (all in one doc) to 1 as agreed.
    this.encoding = TweetUtils.DEFAULT_ENCODING;
    this.contentKeys = Arrays.asList(TweetUtils.DEFAULT_CONTENT_KEYS);
    this.featureKeys = Arrays.asList(TweetUtils.DEFAULT_FEATURE_KEYS);
  }
  

  /**
   * Constructor with all options.
   * @param tpd
   * @param encoding
   * @param cks
   * @param fks
   */
  public PopulationConfig(int tpd, String encoding, List<String> cks, List<String> fks) {
    this.tweetsPerDoc = tpd;
    this.encoding = encoding;
    this.contentKeys = cks;
    this.featureKeys = fks;
  }
  
  
  public void reload(File file) {
    PopulationConfig source = PopulationConfig.load(file);
    this.tweetsPerDoc = source.tweetsPerDoc;
    this.encoding = source.encoding;
    this.contentKeys = source.contentKeys;
    this.featureKeys = source.featureKeys;
  }
  
  public void reload(URL url) {
    PopulationConfig source = PopulationConfig.load(url);
    this.tweetsPerDoc = source.tweetsPerDoc;
    this.encoding = source.encoding;
    this.contentKeys = source.contentKeys;
    this.featureKeys = source.featureKeys;
  }
  
  
  public static PopulationConfig load(File file) {
    XStream xstream = new XStream(new StaxDriver());
    // setClassLoader needed so XStream finds plugin classes
    xstream.setClassLoader(Gate.getClassLoader());
    return (PopulationConfig) xstream.fromXML(file);
  }

  public static PopulationConfig load(URL url) {
    XStream xstream = new XStream(new StaxDriver());
    // setClassLoader needed so XStream finds plugin classes
    xstream.setClassLoader(Gate.getClassLoader());
    return (PopulationConfig) xstream.fromXML(url);
  }

  
  public void saveXML(File file) throws IOException {
    XStream xstream = new XStream(new StaxDriver());
    //TODO slap an XML prolog on the front
    PrettyPrintWriter ppw = new PrettyPrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
    xstream.marshal(this, ppw);
    ppw.close();
  }
  
}


class LoadConfigListener implements ActionListener {
  PopulationDialogWrapper wrapper;
  
  public LoadConfigListener(PopulationDialogWrapper wrapper) {
    this.wrapper = wrapper;
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Load XML configuration");
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    int chosen = chooser.showOpenDialog(this.wrapper.dialog);
    if (chosen == JFileChooser.APPROVE_OPTION) {
      wrapper.setNewConfig(PopulationConfig.load(chooser.getSelectedFile()));
    }
  }
}


class SaveConfigListener implements ActionListener {
  PopulationDialogWrapper wrapper;
  
  public SaveConfigListener(PopulationDialogWrapper wrapper) {
    this.wrapper = wrapper;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Save configuration as XML");
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    int chosen = chooser.showSaveDialog(this.wrapper.dialog);
    if (chosen == JFileChooser.APPROVE_OPTION) {
      try {
        wrapper.updateConfig();
        wrapper.config.saveXML(chooser.getSelectedFile());
      } 
      catch(IOException e1) {
        e1.printStackTrace();
      }
    }
  }
  
  
}


