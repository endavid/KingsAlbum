package titech.image;

import java.awt.*;
import java.awt.image.*;

/**
 * A Canvas used for showing images
 * @see titech.image.InteractiveImageDisplay if you want to use JAI.
 */
public class ImageCanvas extends Canvas {

	/** The images for Double Buffering. mImage is the layer being "painted" */
	private BufferedImage mImage = null;
		
	protected Color bgColor = Color.black;
	protected Color fgColor = Color.white;
	
	public ImageCanvas(int width, int height) {
		super();

		setSize(width, height);		
		setBackground(bgColor);
		setForeground(fgColor);
	}

	public void setSize(int width, int height) {
		super.setSize(width, height);
		
		mImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	}
	
	
	public void set(BufferedImage img) {
		setSize(img.getWidth(), img.getHeight());

		Graphics layer = mImage.getGraphics();
		
		layer.drawImage(img,0,0,this);
		
		repaint();
	}

	

    public void paint(Graphics g) {				
		g.drawImage(mImage,0,0,this);
    }	
	
	/** Redefine the method since we already clear in paint.
	* Et voila! No glitches! :D
	  */
	public void update(Graphics g) {
		paint(g);
	}
		
}

