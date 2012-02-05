import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.media.jai.*;
import javax.media.jai.iterator.*;

/**
 * Application to generate albums in HTML/XML.<p>
 * La noche de reyes es un buen dia para empezar...
 * TODO: rotations, script processing (---, "custom1", "custom2", "custom3")
 * <p>
 * History:
 * <pre>
 * v. 0.4  (04/08/24) Change resize to be relative to one dimension. Added new arrangement: COLUMN_BY_4;
 * v. 0.37 Sort the files (not sorted by default in JDK).
 * v. 0.36 Relative aspect ratio for thumbs.
 * v. 0.35 Added an additional arrangement.
 * v. 0.3 (03/10/23) Added navigation bar (javascript).
 * v. 0.2 Shows the generated HTML file, and becomes editable (WYSWYG).
 * v. 0.1 Generates HTML files from a directory of images.
 * </pre>
 *
 * @author     David Gavilan
 * @created    2003/01/05
 */
public class KingsAlbum extends JFrame implements ActionListener {
	static String version="v0.41";
	
	
	public KingsAlbum() {
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		
		JEditorPane htmlViewer = new JEditorPane();
		ToolBar toolBar = new ToolBar();
		IconBrowser iconBrowser = new IconBrowser();
		NavigationBar naviBar = new NavigationBar();
		naviBar.setToolBar(toolBar);
		naviBar.setPane(htmlViewer);
		naviBar.setBrowser(iconBrowser);
		
		JScrollPane view = new JScrollPane(htmlViewer);
		
		// SOUTH: images/brushes to select
		JScrollPane scroller = new JScrollPane(iconBrowser,
			JScrollPane.VERTICAL_SCROLLBAR_NEVER,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setPreferredSize(new Dimension(100,IconBrowser.THUMB_LONG*2));
		
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(toolBar, BorderLayout.NORTH);
		southPanel.add(scroller, BorderLayout.CENTER);
		
		contentPane.add(naviBar, BorderLayout.NORTH);
		contentPane.add(view,BorderLayout.CENTER);
		contentPane.add(southPanel, BorderLayout.SOUTH);

	}
	
	/**
	 * Implementacion del ActionListener
	 *
	 * @param  e  Description of the Parameter
	 */
	public void actionPerformed(ActionEvent e) {
		JComponent src = (JComponent) (e.getSource());
	}

	/**
	 * Creamos la aplicacion
	 *
	 * @param  s  Description of the Parameter
	 */
	public static void main(String s[]) {
		KingsAlbum window = new KingsAlbum();
		window.addWindowListener(
			new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					System.exit(0);
				}
			});

		String ver = System.getProperty("java.version");
		//String vmver = System.getProperty("java.vm.version");
		window.setTitle("3 Kings Album "+version+" [JRE: "+ver+"]");
		window.pack();
		window.setVisible(true);
	}

}
