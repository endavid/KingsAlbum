package titech.image;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.util.Vector;

import javax.media.jai.*;
import javax.media.jai.iterator.*;

import javax.swing.*;


/**
 * This class allows to paint over an image at a different layer.
 *
 * It provides different paint tools.
 * The MouseListener is activated when calling getOdometer in ImageDisplay.
 *
 * @author David Gavilan Ruiz 
 * 2003 - May - 26
 */
public class InteractiveImageDisplay
    extends ImageDisplay {

    public static final int NONE = 0;
    public static final int DOTS = 1;
    public static final int POLY = 2;
    private boolean colorInfo = true;
    private boolean colorHSB = false;

    /** used to access the source image */
    private RandomIter iter = null;
    private JButton colorOdometer = new JButton(".");
    protected Vector pointArray = new Vector();
    protected int toolMode = POLY;
    protected Color brushColor = Color.red; // from jdk 1.4, also Color.RED
    protected Stroke stroke = new BasicStroke(1f);
    protected BufferedWriter bw;
    protected BufferedWriter bwsamples;

    /** We update linkedComponent at the same time as this.
      * Used for mirror painting.
      */
    protected Component linkedComponent = null;

    public InteractiveImageDisplay() {
        super();
    }

    public InteractiveImageDisplay(PlanarImage pimg) {
        super(pimg);
    }

    public InteractiveImageDisplay(int x, int y) {
        super(x, y);
    }

    /** Produces a Color Odometer Swing Component, i.e. provides color information. */
    public JComponent getColorOdometer() {

        return colorOdometer;
    }

    public void setLinkedComponent(Component lc) {
        this.linkedComponent = lc;
    }

    /** Changes the paint mode */
    public void setTool(int mode) {
        toolMode = mode;
    }

    public void setStroke(int size) {
        stroke = new BasicStroke((float)size, BasicStroke.CAP_ROUND, 
                                 BasicStroke.JOIN_ROUND);
    }

    /** Get the paint mode we are using */
    public int getTool() {

        return toolMode;
    }

    /** Activates the color odometer */
    public void setColorInfo() {
        colorInfo = true;

        // used to access the source image
        iter = RandomIterFactory.create(source, null);
    }

    /** Deactivates the color odometer */
    public void unsetColorInfo() {
        colorInfo = false;
        iter = null;
    }

    /** When the canvas changes, we should call this function to reset some values */
    public void resetState() {

        if (colorInfo)
            setColorInfo();
    }

    /** Return the collection of points we have painted */
    public Vector getVector() {

        return pointArray;
    }

    /** Links to a given array of points */
    public void setVector(Vector v) {
        pointArray = v;
    }

    /** Erases all points */
    public void clearVector() {
        pointArray.clear();
        System.out.println("vector size: " + pointArray.size());
    }

    /**
     * @see clearVector
     */
    public void resetTools() {
        clearVector();

        // other tools ...
    }

    public void setPaintColor(Color c) {
        brushColor = c;
		//brushColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), 128);
		
        if (getClass().isInstance(linkedComponent)) {
            ((InteractiveImageDisplay)linkedComponent).setPaintColor(c);
        }
    }

    /** Show the array of points in the standard output */
    public void showPoints() {

        if (pointArray.size() <= 0) {
            System.out.println("No elements");
        }

        for (int i = 0; i < pointArray.size(); i++) {

            Point p = (Point)pointArray.get(i);
            System.out.println("" + i + ": " + p);
        }
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

        Point p = absoluteCoords(e.getPoint());
        int mods = e.getModifiers();

        switch (toolMode) {

            case DOTS:
            case POLY:
                pointArray.add(p);
                repaint();

                if (linkedComponent != null)
                    linkedComponent.repaint();

                break;

            default:
        }

        if (odometer != null) {

            String output = " (" + p.x + ", " + p.y + ")";
            odometer.setText(output);
        }
    }

    public void mouseReleased(MouseEvent e) {

        Point p = absoluteCoords(e.getPoint());

        if (odometer != null) {

            String output = " (" + p.x + ", " + p.y + ")";
            odometer.setText(output);
        }
    }

    public void mouseClicked(MouseEvent e) {

        if (colorInfo) {
            colorHSB = colorHSB ? false : true;
        }
    }

    public void mouseMoved(MouseEvent e) {

        Point p = absoluteCoords(e.getPoint());

        if (odometer != null) {

            String output = " (" + p.x + ", " + p.y + ")";

            if (colorInfo) {

                try {

                    int red = iter.getSample(p.x, p.y, 0);
                    int green = iter.getSample(p.x, p.y, 1);
                    int blue = iter.getSample(p.x, p.y, 2);
                    Color c = new Color(red, green, blue);

                    if (colorHSB) {

                        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
                        output += " (" + hsb[0] + ", " + hsb[1] + ", " + hsb[2] + ")";
                    } else {
                        output += " (" + red + ", " + green + ", " + blue + ")";
                    }

                    colorOdometer.setBackground(c);
                } catch (ArrayIndexOutOfBoundsException obe) {

                    // out of bounds
                    colorOdometer.setBackground(getBackground());
                }
            }

            odometer.setText(output);
        }
    }

    public void mouseDragged(MouseEvent e) {
        mousePressed(e);
    }

    public synchronized void forcePaint() {

        Graphics g = getGraphics();
        paintComponent(g);
    }

    public synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawOn(g);
    }

    /**
     * Draws the result of used tools over the Graphics object g.
     * The object may refer to an image, for instance.
     */
    public void drawOn(Graphics g0) {

        Graphics2D g = (Graphics2D)g0;
        g.setColor(brushColor);
        g.setStroke(stroke);

        switch (toolMode) {

            case POLY:

                if (pointArray.size() <= 0)

                    break;

                int[] xPoints = new int[pointArray.size()];
                int[] yPoints = new int[pointArray.size()];

                for (int i = 0; i < pointArray.size(); i++) {

                    Point p = relativeCoords(new Point((Point)pointArray.get(i)));
                    xPoints[i] = p.x;
                    yPoints[i] = p.y;
                }

                g.drawPolyline(xPoints, yPoints, pointArray.size());

                break;

            case DOTS:

                if (pointArray.size() <= 0)

                    break;

                for (int i = 0; i < pointArray.size(); i++) {

                    Point p = relativeCoords(new Point((Point)pointArray.get(i)));
                    g.drawOval(p.x, p.y, 2, 2);
                }

                break;

            default:
        }
    }

    Point absoluteCoords(Point p) {

        // just in case the image is displaced
        p.x += originX;
        p.y += originY;

        return p;
    }

    /** Given an absolute coordinate (image coordinate), returns it in component coordinates */
    Point relativeCoords(Point p) {

        // just in case the image is displaced
        p.x -= originX;
        p.y -= originY;

        return p;
    }

    public void saveRegionColors(String colorname) {

        int[] xPoints = new int[pointArray.size()];
        int[] yPoints = new int[pointArray.size()];

        for (int i = 0; i < pointArray.size(); i++) {

            Point p = relativeCoords(new Point((Point)pointArray.get(i)));
            xPoints[i] = p.x;
            yPoints[i] = p.y;
        }

        Polygon pol = new Polygon(xPoints, yPoints, pointArray.size());
        Rectangle rect = pol.getBounds();

        try {

            for (double y = rect.getY();
                 y < (rect.getY() + rect.getHeight());
                 y++)

                for (double x = rect.getX();
                     x < (rect.getX() + rect.getWidth());
                     x++) {

                    if (pol.contains(x, y)) {

                        int r = iter.getSample((int)x, (int)y, 0);
                        int g = iter.getSample((int)x, (int)y, 1);
                        int b = iter.getSample((int)x, (int)y, 2);
                        String sample = "" + ((double)r / 255.) + " " + 
                                        ((double)g / 255.) + " " + 
                                        ((double)b / 255.);
                        bwsamples.write(sample, 0, sample.length());
                        bwsamples.newLine();
                        bw.write(colorname, 0, colorname.length());
                        bw.newLine();
                    }
                }
        } catch (Exception ee) {
            System.err.println("I couldn't save colors. " + ee);
        }
    }

    public void setBuffers(BufferedWriter bw, BufferedWriter bwsamples) {
        this.bw = bw;
        this.bwsamples = bwsamples;
    }

    /** Pastes current draw layer over the Image */
    public void brushLayer() {

        TiledImage tim = new TiledImage(source, false);

        if (tim.getClass().isInstance(source)) {
            tim = (TiledImage)source;
            Graphics2D gpim = tim.createGraphics();
            drawOn(gpim);
        } else {
            Graphics2D gpim = tim.createGraphics();
            drawOn(gpim);
            source.dispose();
            source = tim;
        }

        if (getClass().isInstance(linkedComponent)) {
            ((InteractiveImageDisplay)linkedComponent).brushLayer();
        }

        //updateView();
    }
	
	/** Clears the picture */
	public void setBackground(Color c) {
		super.setBackground(c);
		
        TiledImage tim = new TiledImage(source, true); //share buffers
		Graphics2D gpim = tim.createGraphics();
		
		gpim.setColor(c);
		gpim.fillRect(0,0,tim.getWidth(),tim.getHeight());
		
	}
}
