package titech.image.dsp;

import javax.media.jai.*;

/**
 * Default configurations for blob processing.
 */
public class BlobOps {

	/**
	 * This decides the size of the resulting image, thus, the resulting blobs.
	 * It should be the same size as for the ones in the Database (80x60)
	 */
	public final static int IMGLONG = 80, IMGSHORT = 60;

	/** Sigma of the Gaussian filter **/
	public final static double SCALE = 4;
	/** For the color categorization object */
	public final static double THRESHOLD = 0.5;
	
	/** Default minimum region size **/
	public final static double OBJ_SIZE = 0.005;

	/** Applies the Color Blobs segmentation with the default parameters
      * (includes color correction).
	  */
	public final static ObjectImage getBlobs(PlanarImage pimg,
		ColorCategorization colorCat) {
			return getBlobs(pimg, colorCat, OBJ_SIZE);
	}
	
	public final static ObjectImage getBlobs(PlanarImage pimg,
		ColorCategorization colorCat, double osize) {
		int resultWidth = IMGLONG, resultHeight=IMGLONG;
		
		if (pimg.getWidth()>pimg.getHeight()) {
			resultWidth = IMGLONG; resultHeight = IMGSHORT;
		} else {
			resultWidth = IMGSHORT; resultHeight = IMGLONG;
		}
	
		PlanarImage ccorrected = ColorManipulation.modWhiteInGray(pimg);
		
		RenderedOp rop = COps.scale(ccorrected,resultWidth,resultHeight);
		rop = COps.gaussianBlur(rop,SCALE);

		TiledImage reg = colorCat.categorize(rop);
		
		return new ObjectImage(reg,COps.scale(pimg,resultWidth,resultHeight),osize);
		
	}
}
