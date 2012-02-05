package titech.image.dsp;

import java.awt.image.*;
import javax.media.jai.*;
import javax.media.jai.iterator.*;

import titech.image.math.*;

/**
 * TOps = Testing Operations
 *
 * @author     David Gavilan
 * @version    03/12/27
 */
public class TOps {
	/**
	 * Constructs the Scale-Space (SS) representation of a given row of an image.
	 *
	 * @param  pim  Input PlanarImage
	 * @param  ns   Number of scales to compute
	 * @param  row  Which row
	 * @return      An image showing the Scale-Scape from scale 1 to ns of a choosen row.
	 */
	public static TiledImage buildSSrow(PlanarImage pim, int ns, int row, double step) {
		int bands = pim.getSampleModel().getNumBands();
		int height = pim.getHeight();
		int tileHeight = pim.getTileHeight();
		int width = pim.getWidth();
		int tileWidth = pim.getTileWidth();

		SampleModel sm = pim.getSampleModel();
		ColorModel cm = pim.getColorModel();

		TiledImage outImage = new TiledImage(0,0,width,ns,0,0,sm,cm);

		PlanarImage ss=pim;
		
		double sigma=1.;
		if (step<sigma) sigma=step;
		for (int s = 1; s <= ns; s++) {

			// used to access the source image
			RandomIter iter = RandomIterFactory.create(ss, null);

			for (int i=0;i<width;i++)
			for (int band=0;band<bands;band++) {
				outImage.setSample(i, s-1, band, (byte) iter.getSampleFloat(i, row, band));
			}

			//pim = COps.gaussianBlur(pim,5,1f);
			ss=COps.gaussianBlur(pim, sigma);
			sigma+=step;
		}

		return outImage;
	}


	/**
	 * Constructs the Scale-Space (SS) representation of a given column of an image.
	 *
	 * @param  pim  Input PlanarImage
	 * @param  ns   Number of scales to compute
	 * @param  row  Which column
	 * @return      An image showing the Scale-Scape from scale 1 to ns of a choosen column.
	 */
	public static TiledImage buildSScol(PlanarImage pim, int ns, int col) {
		int bands = pim.getSampleModel().getNumBands();
		int height = pim.getHeight();
		int tileHeight = pim.getTileHeight();
		int width = pim.getWidth();
		int tileWidth = pim.getTileWidth();

		SampleModel sm = pim.getSampleModel();
		ColorModel cm = pim.getColorModel();

		TiledImage outImage = new TiledImage(0,0,ns+1,height,0,0,sm,cm);
		
		PlanarImage aux = pim;
		int s = 0;		
		for (float sigma = 0; sigma < (float)ns; sigma+=1f, s++) {

			// used to access the source image
			RandomIter iter = RandomIterFactory.create(aux, null);

			for (int j=0;j<height;j++)
			for (int band=0;band<bands;band++) {
				outImage.setSample(s,j, band, iter.getSample(col, j, band));
			}

			aux = COps.gaussianBlur(pim,21,sigma);
		}

		return outImage;
	}

	/** Creates a Gaussian Color Blob */
	public static TiledImage gaussianBlob(int size, double[] sigmas) {
		return gaussianBlob(size, sigmas, new double[]{255, 255, 255});
	}
	
	/** Creates a Gaussian Color Blob.
	 * @param size The size of the gaussian blob in pixels.
	 * @param sigmas The standard deviation for each band.
	 * @param mean The average (mean color in the center of the gaussian).
	 */
	public static TiledImage gaussianBlob(int size, double[] sigmas, double[] mean) {
		int mid = size >> 1;
		
		int[][][] img = new int[3][][];
		
		for (int b=0; b<3; b++) {
			double[][] kernel = Difference.gaussian2Dkernel(size, sigmas[b]);
			AMath.mult(kernel, 1.0/kernel[mid][mid]);
			img[b] = AMath.toInt(AMath.mult(kernel,mean[b]));
			//System.err.println(AMath.showMatrix(img[b]));
		}
		
		TiledImage res = ImageObjects.createRGBImage(size, size);
		for (int b=0; b<3; b++)
			for (int i=0;i<size;i++)
				for (int j=0;j<size;j++)
					res.setSample(i,j,b,img[b][i][j]);
		
		return res;
	}
	/**
	public static void main(String args[]) {
		int size = Integer.parseInt(args[0]);
		double[] sigmas = {
			Double.parseDouble(args[1]),
			Double.parseDouble(args[2]),
			Double.parseDouble(args[3])};
		
		gaussianBlob(size, sigmas);
	}*/
}
// -- end class TOps

