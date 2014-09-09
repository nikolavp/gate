/*
 * Copyright (c) 1995-2012, The University of Sheffield. See the file
 * COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 * Copyright (c) 2009, Ontotext AD.
 * 
 * This file is part of GATE (see http://gate.ac.uk/), and is free software,
 * licenced under the GNU Library General Public License, Version 2, June 1991
 * (in the distribution as file licence.html, and also available at
 * http://gate.ac.uk/gate/licence.html).
 * 
 * PluginManagerUI.java
 * 
 * Valentin Tablan, 21-Jul-2004
 * 
 * $Id: PluginManagerUI.java 13565 2011-03-26 23:03:34Z johann_p $
 */

package gate.gui.creole.manager;

import gate.Gate;
import gate.Gate.ResourceInfo;
import gate.gui.MainFrame;
import gate.resources.img.svg.AddIcon;
import gate.resources.img.svg.AvailableIcon;
import gate.resources.img.svg.GATEIcon;
import gate.resources.img.svg.InvalidIcon;
import gate.resources.img.svg.OpenFileIcon;
import gate.resources.img.svg.RemotePluginIcon;
import gate.resources.img.svg.RemoveIcon;
import gate.resources.img.svg.ResetIcon;
import gate.resources.img.svg.UserPluginIcon;
import gate.swing.CheckBoxTableCellRenderer;
import gate.swing.IconTableCellRenderer;
import gate.swing.XJFileChooser;
import gate.swing.XJTable;
import gate.util.GateRuntimeException;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class AvailablePlugins extends JPanel {

  private XJTable mainTable;

  /**
   * Contains the URLs from Gate.getKnownPlugins() that satisfy the filter
   * filterTextField for the plugin URL and the plugin resources names
   */
  private List<URL> visibleRows = new ArrayList<URL>();

  private JSplitPane mainSplit;

  private MainTableModel mainTableModel;

  private ResourcesListModel resourcesListModel;

  private JList<ResourceInfo> resourcesList;

  private JTextField filterTextField;

  /**
   * Map from URL to Boolean. Stores temporary values for the loadNow options.
   */
  private Map<URL, Boolean> loadNowByURL = new HashMap<URL, Boolean>();

  /**
   * Map from URL to Boolean. Stores temporary values for the loadAlways
   * options.
   */
  private Map<URL, Boolean> loadAlwaysByURL = new HashMap<URL, Boolean>();

  private static final int ICON_COLUMN = 0;

  private static final int NAME_COLUMN = 3;

  private static final int LOAD_NOW_COLUMN = 1;

  private static final int LOAD_ALWAYS_COLUMN = 2;

  public AvailablePlugins() {
    JToolBar tbPluginDirs = new JToolBar(JToolBar.HORIZONTAL);
    tbPluginDirs.setFloatable(false);
    tbPluginDirs.setLayout(new BoxLayout(tbPluginDirs, BoxLayout.X_AXIS));
    tbPluginDirs.add(new JButton(new AddCreoleRepositoryAction()));
    tbPluginDirs.add(new JButton(new DeleteCreoleRepositoryAction()));
    tbPluginDirs.add(Box.createHorizontalStrut(5));
    JLabel titleLabel = new JLabel("CREOLE Plugin Directories");
    titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 40));
    tbPluginDirs.add(titleLabel);
    tbPluginDirs.add(Box.createHorizontalGlue());
    tbPluginDirs.add(new JLabel("Filter:"));
    filterTextField = new JTextField();
    filterTextField.setToolTipText("Type some text to filter the table rows.");
    tbPluginDirs.add(filterTextField);
    JButton clearFilterButton =
            new JButton(new AbstractAction(null, new ResetIcon(24, 24)) {
              {
                this.putValue(MNEMONIC_KEY, KeyEvent.VK_BACK_SPACE);
                this.putValue(SHORT_DESCRIPTION, "Clear text field");
              }

              @Override
              public void actionPerformed(ActionEvent e) {
                filterTextField.setText("");
                filterTextField.requestFocusInWindow();
              }
            });

    tbPluginDirs.add(clearFilterButton);

    mainTableModel = new MainTableModel();
    mainTable = new XJTable(mainTableModel);
    mainTable.setTabSkipUneditableCell(true);
    mainTable.setSortedColumn(NAME_COLUMN);

    Collator collator = Collator.getInstance(Locale.ENGLISH);
    collator.setStrength(Collator.TERTIARY);
    mainTable.setComparator(NAME_COLUMN, collator);
    mainTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    mainTable.getColumnModel().getColumn(ICON_COLUMN)
            .setCellRenderer(new IconTableCellRenderer());
    CheckBoxTableCellRenderer cbCellRenderer = new CheckBoxTableCellRenderer();
    mainTable.getColumnModel().getColumn(LOAD_ALWAYS_COLUMN)
            .setCellRenderer(cbCellRenderer);
    mainTable.getColumnModel().getColumn(LOAD_NOW_COLUMN)
            .setCellRenderer(cbCellRenderer);

    resourcesListModel = new ResourcesListModel();
    resourcesList = new JList<ResourceInfo>(resourcesListModel);
    resourcesList.setCellRenderer(new ResourcesListCellRenderer());

    // this is needed because otherwise the list gets really narrow most of the
    // time. Strangely if we don't use a custom cell renderer it works fine so
    // that must be where the actual bug is
    ResourceInfo prototype =
            new ResourceInfo("A rather silly long resource name",
                    "java.lang.String", "this is a comment");
    resourcesList.setPrototypeCellValue(prototype);
    resourcesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    // enable tooltips
    ToolTipManager.sharedInstance().registerComponent(resourcesList);

    mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
    mainSplit.setResizeWeight(0.80);
    mainSplit.setContinuousLayout(true);
    JScrollPane scroller = new JScrollPane(mainTable);
    scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    mainSplit.setLeftComponent(scroller);

    scroller = new JScrollPane(resourcesList);
    scroller.setBorder(BorderFactory.createTitledBorder(scroller.getBorder(),
            "Resources in Plugin", TitledBorder.LEFT, TitledBorder.ABOVE_TOP));
    mainSplit.setRightComponent(scroller);

    setLayout(new BorderLayout());

    add(tbPluginDirs, BorderLayout.NORTH);
    add(mainSplit, BorderLayout.CENTER);

    mainTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
              @Override
              public void valueChanged(ListSelectionEvent e) {
                if(!e.getValueIsAdjusting()) {
                  resourcesListModel.dataChanged();
                }
              }
            });

    // when typing a character in the table, use it for filtering
    mainTable.addKeyListener(new KeyAdapter() {

      private Action a = new DeleteCreoleRepositoryAction();

      @Override
      public void keyTyped(KeyEvent e) {
        // if you are doing something other than Shift+ then you probably don't
        // want to use it for filtering
        if(e.getModifiers() > 1) return;

        // if the user presses delete then act as if they pressed the delete
        // button on the toolbar
        if(e.getKeyChar() == KeyEvent.VK_DELETE) {
          a.actionPerformed(null);
          return;
        }

        // these are used for table navigation and not filtering
        if(e.getKeyChar() == KeyEvent.VK_TAB
            || e.getKeyChar() == KeyEvent.VK_SPACE) return;

        // we want to filter so move the character to the filter text field
        filterTextField.requestFocusInWindow();
        filterTextField.setText(String.valueOf(e.getKeyChar()));

      }
    });

    // show only the rows containing the text from filterTextField
    filterTextField.getDocument().addDocumentListener(new DocumentListener() {
      private Timer timer = new Timer("Plugin manager table rows filter", true);

      private TimerTask timerTask;

      @Override
      public void changedUpdate(DocumentEvent e) {
        /* do nothing */
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        update();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        update();
      }

      private void update() {
        if(timerTask != null) {
          timerTask.cancel();
        }
        Date timeToRun = new Date(System.currentTimeMillis() + 300);
        timerTask = new TimerTask() {
          @Override
          public void run() {
            filterRows(filterTextField.getText());
          }
        };
        // add a delay
        timer.schedule(timerTask, timeToRun);
      }
    });

    // Up/Down key events in filterTextField are transferred to the table
    filterTextField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_UP
                || e.getKeyCode() == KeyEvent.VK_DOWN
                || e.getKeyCode() == KeyEvent.VK_PAGE_UP
                || e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
          mainTable.dispatchEvent(e);
        }
      }
    });

    // disable Enter key in the table so this key will confirm the dialog
    InputMap inputMap =
            mainTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    inputMap.put(enter, "none");

    reInit();
  }

  protected void reInit() {
    loadNowByURL.clear();
    loadAlwaysByURL.clear();
    visibleRows.clear();
    visibleRows.addAll(Gate.getKnownPlugins());
    if(mainTable.getRowCount() > 0) mainTable.setRowSelectionInterval(0, 0);
    filterRows("");
  }

  private void filterRows(String rowFilter) {
    final String filter = rowFilter.trim().toLowerCase();
    final String previousURL =
            mainTable.getSelectedRow() == -1 ? "" : (String)mainTable
                    .getValueAt(mainTable.getSelectedRow(),
                            mainTable.convertColumnIndexToView(NAME_COLUMN));
    if(filter.length() < 2) {
      // one character or less, don't filter rows
      visibleRows.clear();
      visibleRows.addAll(Gate.getKnownPlugins());
    } else {
      // filter rows case insensitively on each plugin URL and its resources
      visibleRows.clear();
      for(int i = 0; i < Gate.getKnownPlugins().size(); i++) {
        Gate.DirectoryInfo dInfo =
                Gate.getDirectoryInfo(Gate.getKnownPlugins().get(i));
        String url = dInfo.getUrl().toString();
        String resources = "";
        for(int j = 0; j < dInfo.getResourceInfoList().size(); j++) {
          resources +=
                  dInfo.getResourceInfoList().get(j).getResourceName() + " ";
        }
        if(url.toLowerCase().contains(filter)
                || resources.toLowerCase().contains(filter)) {
          visibleRows.add(Gate.getKnownPlugins().get(i));
        }
      }
    }

    mainTableModel.fireTableDataChanged();

    if(mainTable.getRowCount() > 0) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          mainTable.setRowSelectionInterval(0, 0);
          if(filter.length() < 2 && previousURL != null
                  && !previousURL.equals("")) {
            // reselect the last selected row based on its name and url values
            for(int row = 0; row < mainTable.getRowCount(); row++) {
              String url =
                      (String)mainTable.getValueAt(row,
                              mainTable.convertColumnIndexToView(NAME_COLUMN));
              if(url.contains(previousURL)) {
                mainTable.setRowSelectionInterval(row, row);
                mainTable.scrollRectToVisible(mainTable.getCellRect(row, 0,
                        true));
                break;
              }
            }
          }
        }
      });
    }
  }

  private Boolean getLoadNow(URL url) {
    Boolean res = loadNowByURL.get(url);
    if(res == null) {
      res = Gate.getCreoleRegister().getDirectories().contains(url);
      loadNowByURL.put(url, res);
    }
    return res;
  }

  private Boolean getLoadAlways(URL url) {
    Boolean res = loadAlwaysByURL.get(url);
    if(res == null) {
      res = Gate.getAutoloadPlugins().contains(url);
      loadAlwaysByURL.put(url, res);
    }
    return res;
  }

  private class MainTableModel extends AbstractTableModel {

    private Icon coreIcon, userIcon, remoteIcon, otherIcon, invalidIcon;

    public MainTableModel() {
      otherIcon = new OpenFileIcon(32, 32);
      coreIcon = new GATEIcon(32, 32);
      userIcon = new UserPluginIcon(32, 32);
      remoteIcon = new RemotePluginIcon(32, 32);
      invalidIcon = new InvalidIcon(32, 32);
    }

    @Override
    public int getRowCount() {
      return visibleRows.size();
    }

    @Override
    public int getColumnCount() {
      return 4;
    }

    @Override
    public String getColumnName(int column) {
      switch(column){
        case NAME_COLUMN:
          return "<html><body style='padding: 2px; text-align: center;'>Plugin Name</body></html>";
        case ICON_COLUMN:
          return null;
        case LOAD_NOW_COLUMN:
          return "<html><body style='padding: 2px; text-align: center;'>Load<br>Now</body></html>";
        case LOAD_ALWAYS_COLUMN:
          return "<html><body style='padding: 2px; text-align: center;'>Load<br>Always</body></html>";
        default:
          return "?";
      }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      switch(columnIndex){
        case NAME_COLUMN:
          return String.class;
        case ICON_COLUMN:
          return Icon.class;
        case LOAD_NOW_COLUMN:
        case LOAD_ALWAYS_COLUMN:
          return Boolean.class;
        default:
          return Object.class;
      }
    }

    @Override
    public Object getValueAt(int row, int column) {
      Gate.DirectoryInfo dInfo = Gate.getDirectoryInfo(visibleRows.get(row));
      if(dInfo == null) { return null; }
      switch(column){
        case NAME_COLUMN:
          return dInfo.toHTMLString();
        case ICON_COLUMN:
          if(!dInfo.isValid()) return invalidIcon;
          if(dInfo.isRemotePlugin()) return remoteIcon;
          if(dInfo.isCorePlugin()) return coreIcon;
          if(dInfo.isUserPlugin()) return userIcon;
          return otherIcon;
        case LOAD_NOW_COLUMN:
          return getLoadNow(dInfo.getUrl());
        case LOAD_ALWAYS_COLUMN:
          return getLoadAlways(dInfo.getUrl());
        default:
          return null;
      }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
      Gate.DirectoryInfo dInfo = Gate.getDirectoryInfo(visibleRows.get(row));
      return dInfo.isValid()
              && (column == LOAD_NOW_COLUMN || column == LOAD_ALWAYS_COLUMN);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      Boolean valueBoolean = (Boolean)aValue;
      Gate.DirectoryInfo dInfo =
              Gate.getDirectoryInfo(visibleRows.get(rowIndex));
      if(dInfo == null) { return; }

      switch(columnIndex){
        case LOAD_NOW_COLUMN:
          loadNowByURL.put(dInfo.getUrl(), valueBoolean);
          // for some reason the focus is sometime lost after editing
          // however it is needed for Enter key to execute OkAction
          mainTable.requestFocusInWindow();
          break;
        case LOAD_ALWAYS_COLUMN:
          loadAlwaysByURL.put(dInfo.getUrl(), valueBoolean);
          mainTable.requestFocusInWindow();
          break;
      }
    }
  }

  private class ResourcesListModel extends AbstractListModel<ResourceInfo> {

    @Override
    public ResourceInfo getElementAt(int index) {
      int row = mainTable.getSelectedRow();
      if(row == -1) return null;
      row = mainTable.rowViewToModel(row);
      Gate.DirectoryInfo dInfo = Gate.getDirectoryInfo(visibleRows.get(row));
      return dInfo.getResourceInfoList().get(index);
    }

    @Override
    public int getSize() {
      int row = mainTable.getSelectedRow();
      if(row == -1) return 0;
      row = mainTable.rowViewToModel(row);
      Gate.DirectoryInfo dInfo = Gate.getDirectoryInfo(visibleRows.get(row));
      if(dInfo == null) { return 0; }
      return dInfo.getResourceInfoList().size();
    }

    public void dataChanged() {
      fireContentsChanged(this, 0, getSize() - 1);
    }
  }

  private class ResourcesListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
      Gate.ResourceInfo rInfo = (Gate.ResourceInfo)value;
      // prepare the renderer
      String filter = filterTextField.getText().trim().toLowerCase();
      if(filter.length() > 1
              && rInfo.getResourceName().toLowerCase().contains(filter)) {
        isSelected = true; // select resource if matching table row filter
      }
      super.getListCellRendererComponent(list, rInfo.getResourceName(), index,
              isSelected, cellHasFocus);
      // add tooltip text
      setToolTipText(rInfo.getResourceComment());
      return this;
    }
  }

  protected boolean unsavedChanges() {

    Set<URL> creoleDirectories = Gate.getCreoleRegister().getDirectories();

    Iterator<URL> pluginIter = loadNowByURL.keySet().iterator();
    while(pluginIter.hasNext()) {
      URL aPluginURL = pluginIter.next();
      boolean load = loadNowByURL.get(aPluginURL);
      boolean loaded = creoleDirectories.contains(aPluginURL);
      if(load && !loaded) { return true; }
      if(!load && loaded) { return true; }
    }

    pluginIter = loadAlwaysByURL.keySet().iterator();
    while(pluginIter.hasNext()) {
      URL aPluginURL = pluginIter.next();
      boolean load = loadAlwaysByURL.get(aPluginURL);
      boolean loaded = Gate.getAutoloadPlugins().contains(aPluginURL);
      if(load && !loaded) { return true; }
      if(!load && loaded) { return true; }
    }

    return false;
  }

  protected Set<URL> updateAvailablePlugins() {

    Set<URL> creoleDirectories = Gate.getCreoleRegister().getDirectories();

    // update the data structures to reflect the user's choices
    Iterator<URL> pluginIter = loadNowByURL.keySet().iterator();
    
    Set<URL> toLoad = new HashSet<URL>();
    while(pluginIter.hasNext()) {
      URL aPluginURL = pluginIter.next();
      boolean load = loadNowByURL.get(aPluginURL);
      boolean loaded = creoleDirectories.contains(aPluginURL);
      if(load && !loaded) {
        // remember that we need to load this plugin
        toLoad.add(aPluginURL);
      }
      if(!load && loaded) {
        // remove the directory
        Gate.getCreoleRegister().removeDirectory(aPluginURL);
      }
    }

    pluginIter = loadAlwaysByURL.keySet().iterator();
    while(pluginIter.hasNext()) {
      URL aPluginURL = pluginIter.next();
      boolean load = loadAlwaysByURL.get(aPluginURL);
      boolean loaded = Gate.getAutoloadPlugins().contains(aPluginURL);
      if(load && !loaded) {
        // set autoload to true
        Gate.addAutoloadPlugin(aPluginURL);
      }
      if(!load && loaded) {
        // set autoload to false
        Gate.removeAutoloadPlugin(aPluginURL);
      }
    }
    
    while(!toLoad.isEmpty()) {
      //lets finally try loading all the plugings
      int numToLoad = toLoad.size();
      List<Throwable> errors = new ArrayList<Throwable>();
      
      pluginIter = toLoad.iterator();
      while(pluginIter.hasNext()) {
        URL aPluginURL = pluginIter.next();
        
        // load the directory
        try {
          Gate.getCreoleRegister().registerDirectories(aPluginURL);
          pluginIter.remove();
        } catch(Throwable ge) {
          //TODO suppress the errors unless we are going to break out of the loop
          //ge.printStackTrace();
          errors.add(ge);
        }
      }
      
      if (numToLoad == toLoad.size()) {
        //we tried loading all the plugins and yet
        //we didn't actually achieve anything
        for (Throwable t : errors) {
          t.printStackTrace();
        }        
        
        break;
      }
    }
    
    loadNowByURL.clear();
    loadAlwaysByURL.clear();
    
    return toLoad;
  }

  private class DeleteCreoleRepositoryAction extends AbstractAction {
    public DeleteCreoleRepositoryAction() {
      super(null, new RemoveIcon(24, 24));
      putValue(SHORT_DESCRIPTION, "Unregister selected CREOLE directory");
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
      int[] rows = mainTable.getSelectedRows();

      for(int row : rows) {
        int rowModel = mainTable.rowViewToModel(row);

        URL toDelete = visibleRows.get(rowModel);

        Gate.DirectoryInfo dInfo = Gate.getDirectoryInfo(toDelete);

        Gate.getCreoleRegister().removeDirectory(toDelete);

        if(!dInfo.isCorePlugin() && !dInfo.isUserPlugin()) {
          Gate.removeKnownPlugin(toDelete);
          loadAlwaysByURL.remove(toDelete);
          loadNowByURL.remove(toDelete);
        }
      }

      // redisplay the table with the current filter
      filterRows(filterTextField.getText());
    }
  }

  private class AddCreoleRepositoryAction extends AbstractAction {
    public AddCreoleRepositoryAction() {
      super(null, new AddIcon(24, 24));
      putValue(SHORT_DESCRIPTION, "Register a new CREOLE directory");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      JTextField urlTextField = new JTextField(20);

      class URLfromFileAction extends AbstractAction {
        URLfromFileAction(JTextField textField) {
          super(null, new OpenFileIcon(24, 24));
          putValue(SHORT_DESCRIPTION, "Click to select a directory");
          this.textField = textField;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
          XJFileChooser fileChooser = MainFrame.getFileChooser();
          fileChooser.setMultiSelectionEnabled(false);
          fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
          fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
          fileChooser.setResource("gate.CreoleRegister");
          int result = fileChooser.showOpenDialog(AvailablePlugins.this);
          if(result == JFileChooser.APPROVE_OPTION) {
            try {
              textField.setText(fileChooser.getSelectedFile().toURI().toURL()
                      .toExternalForm());
            } catch(MalformedURLException mue) {
              throw new GateRuntimeException(mue.toString());
            }
          }
        }

        JTextField textField;
      }

      JButton fileBtn = new JButton(new URLfromFileAction(urlTextField));

      JPanel message = new JPanel();
      GroupLayout msgLayout = new GroupLayout(message);
      message.setLayout(msgLayout);

      msgLayout.setAutoCreateContainerGaps(true);
      msgLayout.setAutoCreateGaps(true);

      JLabel lblURL = new JLabel("Type a URL");
      JLabel lblDir = new JLabel("Select a Directory");
      JLabel lblOR = new JLabel("or");

      msgLayout
              .setHorizontalGroup(msgLayout
                      .createSequentialGroup()
                      .addGroup(
                              msgLayout.createParallelGroup()
                                      .addComponent(lblURL)
                                      .addComponent(urlTextField))
                      .addComponent(lblOR)
                      .addGroup(
                              msgLayout
                                      .createParallelGroup(
                                              GroupLayout.Alignment.CENTER)
                                      .addComponent(lblDir)
                                      .addComponent(fileBtn)));

      msgLayout
              .setVerticalGroup(msgLayout
                      .createSequentialGroup()
                      .addGroup(
                              msgLayout.createParallelGroup()
                                      .addComponent(lblURL)
                                      .addComponent(lblDir))

                      .addGroup(
                              msgLayout
                                      .createParallelGroup(
                                              GroupLayout.Alignment.CENTER)
                                      .addComponent(urlTextField)
                                      .addComponent(lblOR)
                                      .addComponent(fileBtn)));

      if(JOptionPane.showConfirmDialog(AvailablePlugins.this, message,
              "Register a new CREOLE directory", JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE, new AvailableIcon(48, 48)) != JOptionPane.OK_OPTION)
        return;

      try {
        final URL creoleURL = new URL(urlTextField.getText());
        Gate.addKnownPlugin(creoleURL);
        mainTable.clearSelection();
        // redisplay the table without filtering
        filterRows("");
        // clear the filter text field
        filterTextField.setText("");
        // select the new plugin row
        SwingUtilities.invokeLater(new Runnable() {

          @Override
          public void run() {
            for(int row = 0; row < mainTable.getRowCount(); row++) {
              String url =
                      (String)mainTable.getValueAt(row,
                              mainTable.convertColumnIndexToView(NAME_COLUMN));
              if(url.contains(creoleURL.toString())) {
                mainTable.setRowSelectionInterval(row, row);
                mainTable.scrollRectToVisible(mainTable.getCellRect(row, 0,
                        true));
                break;
              }
            }
          }
        });
        mainTable.requestFocusInWindow();
      } catch(Exception ex) {

        JOptionPane
                .showMessageDialog(
                        AvailablePlugins.this,
                        "<html><body style='width: 350px;'><b>Unable to register CREOLE directory!</b><br><br>"
                                + "The URL you specified is not valid. Please check the URL and try again.</body></html>",
                        "CREOLE Plugin Manager", JOptionPane.ERROR_MESSAGE);
      }
    }
  }
}
