package titech.image;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.Vector;

/**
 * A Canvas used for painting in Applets, using Double Buffering.
 * <p>
 * History: <ul>
 * <li>03/12/14: Added brushes (for replacing strokes) and grid mode.
 * </ul>
 * @see titech.image.InteractiveImageDisplay if you want to use JAI.
 */
public class PaintCanvas extends Canvas implements MouseListener, MouseMotionListener {
    public static final int NONE = 0;
    public static final int DOTS = 1;
    public static final int POLY = 2;

	/** The images for Double Buffering. mImage is the layer being "painted" */
	private BufferedImage mImage = null;
	/** Background Layer */
	private BufferedImage mLayer = null;
	/** When a brush is selected, this image is used instead of a stroke */
	private BufferedImage mBrush = null;
	 
	protected Vector pointArray = new Vector();
	
	protected Color bgColor = Color.black;
	protected Color fgColor = Color.white;
	protected Stroke stroke = new BasicStroke(1f);
    protected int toolMode = POLY;	
	
	/** The size of the grid to which drawing is limited. 1x1 is maximum resolution. */
	private int gridWidth = 1;
	private int gridHeight = 1;
		
	public BufferedImage getImage() {
		brushLayer();
		resetTools();
		return mImage;
	}
	
	public PaintCanvas(int width, int height) {
		super();
		
		setSize(width, height);
		setBackground(bgColor);
		setForeground(fgColor);
		
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	
	public PaintCanvas(int width, int height, Color bg) {
		super();
		
		bgColor = bg;
		setBackground(bgColor);
		setForeground(fgColor);
		setSize(width, height);
		
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void setSize(int width, int height) {
		super.setSize(width, height);
		
		mImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics gi = mImage.getGraphics();
		gi.setColor(bgColor);
		gi.fillRect(0,0,width,height);
		mLayer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);		
	}
	
	public void setStroke(int size) {
        stroke = new BasicStroke((float)size, BasicStroke.CAP_ROUND, 
                                 BasicStroke.JOIN_ROUND);
		mBrush = null;
		repaint();
    }
	
	public void setBrush(BufferedImage buf) {
		mBrush = buf;
		toolMode = DOTS;
	}
	
	public void setMode(int mode) {
		if (mode==POLY) {
			mBrush = null;
		}
		toolMode=mode;
	}

	public void setGrid(int width, int height) {
		gridWidth = width; gridHeight = height;
	}
	
    public void setPaintColor(Color c) {
		fgColor = c;
		repaint();
	}
	
	public Color getPaintColor() {
		return fgColor;
	}
	
	/** This is not the background color of the component, but the image
	* inside this component.
	*/
	public void setBackColor(Color c) {		
		bgColor = c;
	}
	public Color getBackColor() {
		return bgColor;
	}
	

	/** Sets a foreground image */
	public void set(BufferedImage img) {
		setSize(img.getWidth(), img.getHeight());

		Graphics layer = mImage.getGraphics();
		
		layer.drawImage(img,0,0,this);
		
		repaint();
	}
		
	 /**
     * @see clearVector
     */
    public void resetTools() {
        clearVector();

        // other tools ...
    }

    /** Erases all points */
    public void clearVector() {
        pointArray.clear();
    }	
    /**
     * Draws the result of used tools over the Graphics object g.
     * The object may refer to an image, for instance.
     */
    public void drawOn(Graphics g0) {

        Graphics2D g = (Graphics2D)g0;
        g.setColor(fgColor);
        g.setStroke(stroke);

        switch (toolMode) {

            case POLY:

                if (pointArray.size() <= 0)

                    break;

                int[] xPoints = new int[pointArray.size()];
                int[] yPoints = new int[pointArray.size()];

                for (int i = 0; i < pointArray.size(); i++) {

                    Point p = (Point)pointArray.get(i);
                    xPoints[i] = p.x;
                    yPoints[i] = p.y;
                }

                g.drawPolyline(xPoints, yPoints, pointArray.size());

                break;

            case DOTS:

                if (pointArray.size() <= 0)

                    break;

                for (int i = 0; i < pointArray.size(); i++) {

                    Point p = (Point)pointArray.get(i);
					if (mBrush!=null) {
						g.drawImage(mBrush, p.x, p.y, this);
					} else {
						g.drawOval(p.x, p.y, 2, 2);
					}
                }

                break;

            default:
        }
    }
	

    public void paint(Graphics g) {
		Graphics layer = mLayer.getGraphics();

		layer.drawImage(mImage,0,0,this);
        drawOn(layer);

		g.drawImage(mLayer,0,0,this);
    }	
	
	/** Redefine the method since we already clear in paint.
	* Et voila! No glitches! :D
	  */
	public void update(Graphics g) {
		paint(g);
	}
	
	/** Pastes the current drawing with the background */
	public void brushLayer() {
		Graphics layer = mLayer.getGraphics();
		Graphics remnant = mImage.getGraphics();
		
		layer.drawImage(mImage,0,0,this);
        drawOn(layer);
		
		remnant.drawImage(mLayer,0,0,this);
	}

	
    // mouse interface
    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            //System.out.println("BUTTON3: " + MouseEvent.BUTTON3);
            brushLayer();
			resetTools();
            return;
        }		
		
        Point p = e.getPoint();
        int mods = e.getModifiers();
		// discretize to grid
		if (gridWidth>1) { p.x /= gridWidth; p.x *= gridWidth;}
		if (gridHeight>1) {p.y /= gridHeight; p.y *=gridHeight;}
		
        switch (toolMode) {

            case DOTS:
            case POLY:
                pointArray.add(p);
                repaint();

                break;

            default:
        }
		
		if (mBrush != null) { // alway brush "brushes"
			brushLayer();
			resetTools();
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

