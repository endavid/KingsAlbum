import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;
import java.util.Vector;
import java.util.Collections;

/**
 * Manages files.
 There is no guarantee that the name strings in the resulting array will appear in any specific order.
They are not, in particular, guaranteed to appear in alphabetical order.
 @see http://javaboutique.internet.com/tutorials/Files_Directories2/
 */
public class NavigationBar extends JPanel implements ActionListener {

	public static final int ICON_WIDTH=28;
	public static final int ICON_HEIGHT=28;
	public static final String CATEGORIES_DIR=
		File.separator+".categories"+File.separator;
	
	public static final String[] arrangements=new String[] {
		"3 columns", "5 columns", "2 tables"};
	public static final int[] arrangeCodes=new int[] {
	AlbumFormat.COLUMN_BY_2, AlbumFormat.COLUMN_BY_4, AlbumFormat.TABLES_2 };	
	
	/**
	 * File dialog.
	 */
	final JFileChooser fc = new JFileChooser();
    final ImageFileFilter filter = new ImageFileFilter();
	
	/**
	 * Name of the selected path.
	 */
	String currentPath=".";
	
	
	File[] fileList = null;
	
	//ImagePanel imagePanel;
		
	JComboBox arrangeCombo;
	
	ToolBar toolBar = null;					
	JEditorPane documentPane = null;
	IconBrowser iconBrowser = null;
	
	
    public NavigationBar() {
	    // interface
	    
		setLayout(new BorderLayout()); // left to right

		JButton importB = new JButton(new ImageIcon(this.getClass().getResource("/resources/load.png")));
		importB.setPreferredSize(new Dimension(ICON_WIDTH,ICON_HEIGHT));		
		importB.setActionCommand("import");		
		JButton buildB = new JButton(new ImageIcon(this.getClass().getResource("/resources/loadp.png")));
		buildB.setPreferredSize(new Dimension(ICON_WIDTH,ICON_HEIGHT));		
		buildB.setActionCommand("build");
		JButton loadB = new JButton(new ImageIcon(this.getClass().getResource("/resources/loadd.png")));
		loadB.setPreferredSize(new Dimension(ICON_WIDTH,ICON_HEIGHT));		
		loadB.setActionCommand("open");

		JPanel openPanel = new JPanel(new FlowLayout());
		openPanel.add(importB);
		openPanel.add(buildB);
		openPanel.add(loadB);
		add(openPanel, BorderLayout.WEST);
		
		JPanel navPanel = new JPanel(new FlowLayout());
		arrangeCombo = new JComboBox(arrangements);
		navPanel.add(arrangeCombo);

		
		add(navPanel, BorderLayout.CENTER);
		
		JButton saveB = new JButton(new ImageIcon(this.getClass().getResource("/resources/save.png")));
		saveB.setPreferredSize(new Dimension(ICON_WIDTH,ICON_HEIGHT));
		saveB.setActionCommand("save");
		add(saveB, BorderLayout.EAST);
		
		
		
		importB.addActionListener(this);
		buildB.addActionListener(this);
		loadB.addActionListener(this);
		saveB.addActionListener(this);
		
		// others
   		filter.addExtension("jpg");
    	filter.addExtension("png");
    			
    	fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
    }

	public void setToolBar(ToolBar tb) {
		toolBar = tb;
	}
	
	public void setPane(JEditorPane pane) {
		documentPane = pane;
	}
	
	public void setBrowser(IconBrowser browser) {
		iconBrowser = browser;
	}

    public void actionPerformed(java.awt.event.ActionEvent e) {
		String s = e.getActionCommand();
		if (s.equals("build")) { // builds an album with an imported dir
			buildCallback();
		} else if (s.equals("import")) { // selects a directory of images
			importCallback();
		} else if (s.equals("open")) { // opens an HTML document
			openCallback();
		} else if (s.equals("save")) { // saves an HTML document
			saveDocument();
		}
    }
    
   	/**
	 * Opens a file-load requester to select a directory containing images.
	 * If preview is activated, it loads the thumbnails.
	 */
	void importCallback() {
		try {			
			int returnVal = fc.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File sf = fc.getSelectedFile();
				currentPath = sf.getAbsolutePath();

				// get the files and sort them
				fileList = sf.listFiles(filter);
				Vector fileVector = new Vector(fileList.length);
				for (int i = 0;i<fileList.length;i++) fileVector.add(fileList[i]);
				Collections.sort(fileVector);
				for (int i = 0;i<fileList.length;i++) fileList[i]=(File)fileVector.get(i);
				
				if (toolBar.preview()) {
					iconBrowser.populate(fileList);
				}
				
				System.out.println("Imported \"" + currentPath + "\"");
				// parent no es el window? quien es?
				//((Frame)getParent()).setTitle(currentPath);
			} else {
				System.out.println("Import command cancelled by user.");
			}
		} catch (Exception e) {
			documentPane.setText("build: " + e);
			//System.err.println("build: " + e );
		}
	}		
	
	/**
	 * Process the image files, makes the thumbnails, and makes an HTML document.
	 */
	void buildCallback() {
		try {			
				
				AlbumFormat af = new AlbumFormat(fileList, toolBar.thumbs());
				af.setArrangement(arrangeCodes[arrangeCombo.getSelectedIndex()]);
				//documentPane.setText("debugging 2..");
				if (toolBar.resize())
					af.resizeImages(fileList, toolBar.getWidth(), toolBar.getHeight());
				if (toolBar.ccorrect())
					af.correctColor(fileList);

				if (toolBar.thumbs()) af.generateThumbs(fileList);
				//documentPane.setText("debugging 3..");

				
				File findex = new File(currentPath+File.separator+"index.html");
				File fhindex = new File(currentPath+File.separator+"hindex.html");
				File fnavi = new File(currentPath+File.separator+"navimenu.html");
				File fscript = new File(currentPath+File.separator+"navifunctions.js");

				af.save(fhindex, findex, fnavi, fscript);
				//documentPane.setText("debugging 4..");

				
				// load documents in window
				documentPane.setPage(fhindex.toURL());
				
		} catch (Exception e) {
			documentPane.setText("build: " + e);
			//System.err.println("build: " + e );
		}
	} // end buildCallback

	
	public void openCallback() {
		try {
			JFileChooser fchooser = new JFileChooser();
			
			int returnVal = fchooser.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File sf = fchooser.getSelectedFile();
				currentPath = sf.getAbsolutePath();
				
				// load documents in window
				documentPane.setPage(sf.toURL());
				
				System.out.println("Loaded \"" + currentPath + "\"");
			} else {
				System.out.println("Open command cancelled by user.");
			}

		} catch(Exception e) {
			System.err.println("open: " + e);
		}
	} //end openCallback()
	

	public void saveDocument() {
		try {
			JFileChooser fchooser = new JFileChooser(currentPath);
	
			int returnVal = fchooser.showSaveDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File sf = fchooser.getSelectedFile();
				currentPath = sf.getAbsolutePath();
				
				java.io.FileWriter fw = new java.io.FileWriter(sf);
				
				// load documents in window
				documentPane.write(fw);
				
				fw.close();
				
				System.out.println("Saved \"" + currentPath + "\"");
			} else {
				System.out.println("Save command cancelled by user.");
			}

		} catch(Exception e) {
			System.err.println("save: " + e);
		}		
	} // saveDocument
	
	public String getFileNamesList(File[] fl) {
		if (fl == null) return "[null]";
		String all = "";
		for (int i=0;i<fl.length;i++) {
			all += fl[i].getAbsolutePath() + "\n";
		}
		return all;
	}
		
	
	public static final void printFileNames(File[] fl) {
		if (fl == null) return;
		for (int i=0;i<fl.length;i++) {
			System.out.println(fl[i].getAbsolutePath());
		}
	}
}
