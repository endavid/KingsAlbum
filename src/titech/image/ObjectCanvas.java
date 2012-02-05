package titech.image;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import titech.image.dsp.*;
import javax.media.jai.*;
import javax.media.jai.iterator.*;


/**
 * A Canvas used for selecting regions from an ObjectImage.
 * @see titech.image.dsp.ObjectImage.
 */
public class ObjectCanvas extends Canvas implements MouseListener, MouseMotionListener {
	ObjectImage objectImage = null;
	private BufferedImage mImage = null;
	private RandomIter iter=null;

	protected Color bgColor = Color.black;
	protected double[][] features;
	protected int[] categories;
	
	int selectedBlob = 0;
	
	public ObjectCanvas(int width, int height) {
		super();

		setSize(width, height);		
		setBackground(bgColor);
		
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	
	public ObjectCanvas(ObjectImage obi) {
		super();
		set(obi);
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	
	public void set(ObjectImage obi) {
		set(obi, obi.meanColorPalette());
	}
	
	public void set(ObjectImage obi, byte[][] colormap) {
		objectImage = obi;
		setSize(obi.getWidth(), obi.getHeight());
		PlanarImage res = ImageObjects.Indexed2RGB(obi.getLabeledImage(),
			8, colormap);
		mImage = res.getAsBufferedImage();
		iter = RandomIterFactory.create(obi.getLabeledImage(), null);
		features = obi.getFeatures();
		categories = obi.getCategories();
		selectedBlob = 0;
		
		repaint();		
	}
	
	
	public void paint(Graphics g) {
		if (mImage != null) g.drawImage(mImage,0,0,this);
	}

	public double[][] getFeatures() {
		return features;
	}
	/** features[0] is from region 1 !!! (region 0 is the background)
	 */
	public double getCx(int i) {
		return features[i][0];
	}
	public double getCy(int i) {
		return features[i][1];
	}
	public double getVol(int i) {
		return features[i][2];
	}
	public double getRVol(int i) {
		return features[i][3];
	}
	public double getR(int i) {
		return features[i][4];
	}
	public double getG(int i) {
		return features[i][5];
	}
	public double getB(int i) {
		return features[i][6];
	}

	public double getDR(int i) {
		return features[i][7];
	}
	public double getDG(int i) {
		return features[i][8];
	}
	public double getDB(int i) {
		return features[i][9];
	}

	public double getSR(int i) {
		return features[i][10];
	}
	public double getSG(int i) {
		return features[i][11];
	}
	public double getSB(int i) {
		return features[i][12];
	}
	
	public int getColor(int i) {
		return categories[i];
	}
	
	/**
	 * Returns user's selected blob, or the center blob if no blob is selected.
	 */
	public int getSelectedBlob() {
		if (selectedBlob>0) return selectedBlob;
		return objectImage.getCentralBlob();
	}
	
    // mouse interface
    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
		
        Point p = e.getPoint();
        int mods = e.getModifiers();
		
		if (iter!=null) {
			int i=iter.getSample(p.x,p.y,0);
			if (i==0) return; // we can not select background			
			
			byte[][] pal = objectImage.meanColorPalette();
			
			// selected to red
			pal[0][i]=(byte)255;
			pal[1][i]=0;
			pal[2][i]=0;
			
			set(objectImage, pal);
			selectedBlob = i;
			/*
			System.out.println("ObjectCanvas: "+p+" "+i+"("
				+getCx(i-1)+", "+getCy(i-1)+") cat:"
				+getColor(i-1));
			*/
		}
    }

    public void mouseReleased(MouseEvent e) {

    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {

    }

    public void mouseDragged(MouseEvent e) {
		mousePressed(e);
    }	
		
}

