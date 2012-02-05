package titech.image.texture;

import javax.media.jai.*;
import javax.media.jai.iterator.*;
import titech.image.dsp.*;

/**
  * An implementation of quilting algorithm.
  * <p> Ref. Image Quilting for Texture Synthesis and Transfer, A.A. Efros and W.T.Freeman, Siggraph 2001.
  *
  */
public class Quilt {
	
	PlanarImage input;
	RandomIter src;	
	TiledImage output;
	double dmax;
	
	/**
	 * @param input Input Texture
	 */
	public Quilt(PlanarImage input) {
		this.input = input;
		src = RandomIterFactory.create(input, null);		
	}
	
	/**
	 * @param w Block width
	 * @param h Block height
	 * @param nw Width in blocks of the output image
	 * @param nh Width in blocks of the output image
	 * @param ov Percentage of overlaping (tipically 1/6)
	 * @param tol Error tolerance (tipically 0.1)
	 */
	public TiledImage quilting(int w, int h, int nw, int nh, double ov, double tol) {
		
		int ovw = (int)(ov*w), ovh = (int)(ov*h);
		output = ImageObjects.createRGBImage(nw*w-(nw-1)*ovw, nh*h-(nh-1)*ovh);
		
		// Go through the image to be synthesized in raster scan order in steps of
		// one block (minus the overlap)
		for (int i=0,x=0; i<nw; i++, x+=w-ovw)
		for (int j=0,y=0; j<nh; j++, y+=h-ovh) {
			// For every location, search the input texture for a set of blocks that
			// satisfy the overlap constraints (above and left) within some error
			// tolerance. Randomly pick one such block.
			//for (int x=0; input.getWidth()-w; x++)
			//for (int y=0; input.getHeight()-h; y++) {
			//	double d = bdistance(x+w-ovw, y, ovw, 1, i, j);
			//}
			// this should be done in bdistance
			//for (int x=0; input.getWidth()-w; x++)
			//for (int y=0; input.getHeight()-h; y++) {
			//	double d = bdistance(x, y+h-ovh, 1, ovh, i, j);
			//}

		}
		
		return output;
	}
	
	/**
	 * Implement this using Dynamic Programming, to avoid repeating calculations already done!
	 */
	double bdistance(int ex, int ey, int ew, int eh, int ox, int oy) {
		
		double d=0;
		for (int x=ex;x<ex+ew;x++) 
		for (int y=ey;y<ey+eh;y++) {
			for (int c=0;c<input.getSampleModel().getNumBands();c++) {
				double p = (double) src.getSample(x, y, c);
				p -= output.getSample(ox+ex, oy+ey, c);
				p *= p;
				d+=p;
			}
		}
		// divide by the total area
		d /= (double) ew*eh;
		d = Math.sqrt(d);
		
		return d;
	}
}
