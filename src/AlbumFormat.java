import java.io.*;
import java.util.Vector;
import javax.media.jai.*;
import titech.image.dsp.*;
import java.util.Vector;

/**
 *  Creates an HTML from a list of Files.
 *
 *  History:
 *  04/08/24 - Change resize to be relative to one dimension. Added new arrangement: COLUMN_BY_4;
 * @author     Owner
 * @created    2003/10/23
 */
public class AlbumFormat {

	/** Arrange 1 table with the middle column for comments */
	public static final int COLUMN_BY_2=1;
	/** Arrange 1 table with 4 columns for pictures and a column for comments */
	public static final int COLUMN_BY_4=2;
	/** Arrange 2 tables inside another table, separated by an empty column. */
	public static final int TABLES_2=3;
	
	boolean thumbs = false;
	String documento;
	Vector table = new Vector();
	File[] names;
	/** The way to arrange the images in the html file */
	int arrangement;

	/**
	 * Constructs an HTML document with a table of images from the File array
	 *
	 * @param  names  Description of the Parameter
	 */
	public AlbumFormat(File[] names) {
		this(names, false);
	}


	/**
	 *Constructor for the AlbumFormat object
	 *
	 * @param  names   Description of the Parameter
	 * @param  thumbs  Description of the Parameter
	 */
	public AlbumFormat(File[] names, boolean thumbs) {
		this.thumbs = thumbs;
		this.names = names;
		setArrangement(COLUMN_BY_2);
		for (int i = 0; i < names.length; i++) {
			String s = "";
			String webName = names[i].getName();
			// sino cambiamos espacios por %20, en netscape no va
			// no va en java 1.3!! (el replaceAll)
			// solo va en la consola de cygwin? --> JAI lo encuentra
			// el java del sistema, o del command.com esta en Program Files\Java !!
			// (el exe en windows\system32\java.exe
			webName = webName.replaceAll(" ", "%20");

			//if (thumbs) s=s+"<a href=\""+webName+"\">";
			s = s + "<a href=\"javascript:goTo(" + i + ");\">";

			s = s + "<img src=\"" + (thumbs ? "thumbs/" : "")
					 + webName + "\">\n";

			if (thumbs) {
				s = s + "</a>";
			}
			table.add(s);
		}
		// note that we should call generateThumbs() from outside.
	}


	public void setArrangement(int arr) {
		arrangement = arr;
	}
	
	/**
	 *  Description of the Method
	 *
	 * @param  fdest                      The html file with thumb pix.
	 * @param  index                      The main frameset.
	 * @param  navi                       The navigation frame.
	 * @param  script                     The Javascript file.
	 * @exception  FileNotFoundException  Description of the Exception
	 * @exception  IOException            Description of the Exception
	 */
	public void save(File fdest, File index, File navi, File script) throws FileNotFoundException, IOException {
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(fdest));
		DataOutputStream indexdos = new DataOutputStream(new FileOutputStream(index));
		DataOutputStream navidos = new DataOutputStream(new FileOutputStream(navi));
		DataOutputStream scriptdos = new DataOutputStream(new FileOutputStream(script));

		String header = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n"
				 + "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html;charset=sjis\">\n"
				 + "<title>HHH Album</title>\n"
				 + "<SCRIPT LANGUAGE=\"Javascript\" SRC=\""
				 + script.getName() + "\"></SCRIPT></head>\n"
				 + "<body bgcolor=\"#253b00\" text=\"#aafaaa\" link=\"#fff8dc\" vlink=\"#6b8e23\">\n"
				 + "<center><h2>HHH Album</h2></center>"
				 + "<table border=1 width="+
				 ((arrangement==COLUMN_BY_2)?75:100)+"% align=center>\n";
		String endDoc = "</table></body></html>\n";

		// Write the index file with thumbnails
		// ------------------------------------------------------------------

		// writeUTF generates wierd characters..
		dos.writeBytes(header);
		
		// now write the contents depending on the arranger
		switch (arrangement) {
			case COLUMN_BY_2:
			for (int i = 0; i < table.size(); i++) {
			dos.writeBytes(new String("<tr><td align=center bgcolor=\"#6b8e23\">\n"));
			dos.writeBytes((String) table.get(i));
			dos.writeBytes(new String("</td>\n<td bgcolor=\"#556b2f\">\n"
					 + "<p align=left>" + i + "<br>\n<p align=right>" + (i + 1) + "\n</td>\n"));
			i++;
			dos.writeBytes(new String("<td align=center bgcolor=\"#6b8e23\">\n"));
			if (i < table.size()) {
				dos.writeBytes((String) table.get(i));
			}
			dos.writeBytes(new String("</td></tr>\n"));
			}
			break;
			case COLUMN_BY_4:
			for (int i = 0; i < table.size(); ) {
			int pi = i;
			dos.writeBytes(new String("<tr>\n"));
			for (int j=0;j<4;j++) { 
				dos.writeBytes(new String("<td align=center bgcolor=\"#6b8e23\">\n"));
				if (i < table.size()) {
					dos.writeBytes((String) table.get(i++));
				}
				dos.writeBytes("</td>\n");
			}
			dos.writeBytes(new String("<td bgcolor=\"#556b2f\">\n"
					 + "<p align=left>" + pi + "<br>\n" + (pi + 1) + "<br>\n"
					 + (pi + 2) + "<br>\n" + (pi + 3)
					 + "\n</td>\n"));
			dos.writeBytes(new String("</tr>\n"));
			}
			break;			
			case TABLES_2:
				String leftTable="";
				String rightTable="";
				for (int i=0; i<table.size(); i++) {
					leftTable+="<tr><td align=center>"+((String)table.get(i))
						+"</td></tr>\n<tr><td class='comment'>"+i+"</td></tr>\n";
					i++;
					if (i<table.size()) {
						rightTable+="<tr><td align=center>"+((String)table.get(i))
						+"</td></tr>\n<tr><td class='comment'>"+i+"</td></tr>\n";
					}
				}
				dos.writeBytes("<tr>\n<td><table border=0 align=center>"
					+leftTable+"</table></td>\n");
				dos.writeBytes("<td><h2>Your Title</h2>\n<p>... comments ...</td>\n");
				dos.writeBytes("<td><table border=0 align=center>"
					+rightTable+"</table></td>\n");
				dos.writeBytes("</tr>");
				break;
				
			default:
				dos.writeBytes("Unknown arrangement!");
				break;
		}
			
		dos.writeBytes(endDoc);

		dos.close();

		// Write the frameset
		// ------------------------------------------------------------------
		indexdos.writeBytes("<html><head><title>HHH Album</title>\n");
		indexdos.writeBytes("<SCRIPT LANGUAGE=\"Javascript\" SRC=\"" + script.getName() + "\"></SCRIPT></head>\n");

		indexdos.writeBytes("<frameset rows=\"*,25\" FRAMEBORDER=NO BORDER=0>\n");
		indexdos.writeBytes("<frame src=\"" + fdest.getName() + "\" name=\"bodypic\">\n");
		indexdos.writeBytes("<frame src=\"" + navi.getName() + "\" name=\"navimenu\" MARGINHEIGHT=\"0\" SCROLLING=NO></frameset>\n");

		indexdos.writeBytes("<noframes><body bgcolor=\"white\"><center>\n");
		indexdos.writeBytes("No frames in your browser?</center></body></html>");

		indexdos.close();

		// Write navigation file
		// ------------------------------------------------------------------
		navidos.writeBytes("<html><head><title>NAVIMENU</title>\n");
		navidos.writeBytes("<SCRIPT LANGUAGE=\"Javascript\" SRC=\""+script.getName()+"\"></SCRIPT>\n");
		navidos.writeBytes("</head><body bgcolor=LIGHTGRAY><center>\n");
		navidos.writeBytes("<a href=\"javascript:left();\">	<img src=\"../../pix/prev.png\" ALT=\"previous\">\n");
		navidos.writeBytes("</a><a href=\"hindex.html\" target=\"bodypic\">\n");
		navidos.writeBytes("<img src=\"../../pix/loadp.png\" ALT=\"INDEX\">\n");
		navidos.writeBytes("</a><a href=\"javascript:right();\">\n");
		navidos.writeBytes("<img src=\"../../pix/next.png\" ALT=\"next\"></a>\n");
		navidos.writeBytes("</center></body></html>\n");

		navidos.close();

		// Write the script file
		// ------------------------------------------------------------------

		scriptdos.writeBytes("currentImage = 0;\nlastImage = " +
				(names.length - 1) + ";\nimArray = new Array();\n");

		for (int i = 0; i < names.length; i++) {
			scriptdos.writeBytes("imArray[" + i + "]=\"" + names[i].getName() + "\";\n");
		}


		scriptdos.writeBytes("function left() {\n");
		scriptdos.writeBytes("	if (parent.currentImage>0) {\n");
		scriptdos.writeBytes("		parent.currentImage--;\n");
		scriptdos.writeBytes("		parent.bodypic.location=imArray[parent.currentImage];\n");
		scriptdos.writeBytes("	}}\n");

		scriptdos.writeBytes("function right() {\n");
		scriptdos.writeBytes("	if (parent.currentImage<lastImage) {\n");
		scriptdos.writeBytes("		parent.currentImage++;\n");
		scriptdos.writeBytes("		parent.bodypic.location=imArray[parent.currentImage];\n");
		scriptdos.writeBytes("	}}\n");

		scriptdos.writeBytes("function goTo(pos) {\n");
		scriptdos.writeBytes("	if (pos>=0 && pos<=lastImage) {\n");
		scriptdos.writeBytes("		parent.currentImage=pos;\n");
		scriptdos.writeBytes("		parent.bodypic.location=imArray[parent.currentImage];\n");
		scriptdos.writeBytes("	}}\n");

		scriptdos.close();

	}


	/**
	 *  Description of the Method
	 *
	 * @param  names            Description of the Parameter
	 * @exception  IOException  Description of the Exception
	 */
	public void generateThumbs(File[] names) throws IOException {
		String path = names[0].getParent() + File.separator + "thumbs";

		File dire = new File(path);
		if (!dire.exists()) {
			dire.mkdir();
		}

		path += File.separator;
		for (int i = 0; i < names.length; i++) {
			//open image
			PlanarImage pimg = JAI.create("fileload", names[i].getAbsolutePath());
			//resize
			int w = pimg.getWidth();
			//resize
			int h = pimg.getHeight();
			int resultWidth = 100;
			int resultHeight = 100;
			if (w > h) {
				resultWidth = 160;
				//resultHeight = 120;
				resultHeight = (h*160)/w;
			} else {
				//resultWidth = 120;
				resultHeight = 160;
				resultWidth = (w*160)/h;
			}
			RenderedOp rop = COps.scale(pimg, resultWidth, resultHeight);
			//save it
			COps.saveAsJPG(rop, path + names[i].getName());
			System.out.println("... " + path + names[i].getName() + " saved.");
		}
	}


	/**
	 * OVERWRITES INPUTS!!!
	 *
	 * @param  names            Description of the Parameter
	 * @param  x                Description of the Parameter
	 * @param  y                Description of the Parameter
	 * @exception  IOException  Description of the Exception
	 */
	public void resizeImages(File[] names, int x, int y) throws IOException {

		for (int i = 0; i < names.length; i++) {
			//open image
			PlanarImage pimg = JAI.create("fileload", names[i].getAbsolutePath());
			//resize
			int w = pimg.getWidth();
			//resize
			int h = pimg.getHeight();
			int resultWidth = 100;
			int resultHeight = 100;
			if (w > h) {
				resultWidth = x;
				resultHeight = (h*x)/w;
			} else {
				resultWidth = (w*y)/h;
				resultHeight = y;
			}
			RenderedOp rop = COps.scale(pimg, resultWidth, resultHeight);
			//save it
			COps.saveAsJPG(rop, names[i].getAbsolutePath());
			System.out.println("... " + names[i].getAbsolutePath() + " saved.");
		}
	}


	/**
	 * OVERWRITES INPUTS!!!
	 *
	 * @param  names            Description of the Parameter
	 * @exception  IOException  Description of the Exception
	 */
	public void correctColor(File[] names) throws IOException {

		for (int i = 0; i < names.length; i++) {
			//open image
			PlanarImage pimg = JAI.create("fileload", names[i].getAbsolutePath());
			//correct color
			pimg = ColorManipulation.modWhiteInGray(pimg);
			//save it
			COps.saveAsJPG(pimg, names[i].getAbsolutePath());
			System.out.println("... " + names[i].getAbsolutePath() + " saved.");
		}
	}

}

