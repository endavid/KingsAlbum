package titech.image.dsp;

import java.awt.image.*;
import java.awt.image.renderable.*;
import javax.media.jai.*;
import java.awt.Transparency;
import javax.media.jai.iterator.*;
import titech.image.math.*;

/**
 * @author     Owner
 * @created    2003/12/26
 * @see        titech.image.dsp.Canny
 */
public class Difference {


	/*
	 *  public static PlanarImage differenciateX(PlanarImage source) {
	 *  float matrix[] =  {-1/2f, 0, 1/2f};
	 *  KernelJAI kernel = new KernelJAI(3,1,matrix);
	 *  return JAI.create("convolve", source, kernel);
	 *  }
	 *  public static PlanarImage differenciateY(PlanarImage source) {
	 *  float matrix[] =  {-1/2f, 0, 1/2f};
	 *  KernelJAI kernel = new KernelJAI(1,3,matrix);
	 *  return JAI.create("convolve", source, kernel);
	 *  }
	 */
	/**
	 *  Description of the Method
	 *
	 * @param  image  Description of the Parameter
	 * @return        Description of the Return Value
	 */
	public static RenderedOp differenciateX(PlanarImage image) {
		if (image.getSampleModel().getDataType() != DataBuffer.TYPE_FLOAT) {
			image = COps.reformat(image, DataBuffer.TYPE_FLOAT);
		}

		ParameterBlock pb = new ParameterBlock();
		pb.addSource(image);
		pb.add(DerivativeOpImage.X);

		return JAI.create("Derivative", pb);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  image  Description of the Parameter
	 * @return        Description of the Return Value
	 */
	public static RenderedOp differenciateY(PlanarImage image) {
		if (image.getSampleModel().getDataType() != DataBuffer.TYPE_FLOAT) {
			image = COps.reformat(image, DataBuffer.TYPE_FLOAT);
		}

		ParameterBlock pb = new ParameterBlock();
		pb.addSource(image);
		pb.add(DerivativeOpImage.Y);

		return JAI.create("Derivative", pb);
	}


	/**
	 * Differenciates an image by the X coordinate (columns)
	 * using Central Difference Approximation.
	 * The resulting PlanarImage contains Float values.
	 *
	 * @param  source  the source image
	 * @return         Description of the Return Value
	 */
	public static PlanarImage old_differenciateX(PlanarImage source) {
		TiledImage dest;

		int bands = source.getSampleModel().getNumBands();
		int height = source.getHeight();
		int width = source.getWidth();
		int tileHeight = source.getTileHeight();
		int tileWidth = source.getTileWidth();

		int offsets[] = new int[bands];
		for (int i = 0; i < bands; i++) {
			offsets[i] = i;
		}
		ComponentSampleModelJAI csm =
				new ComponentSampleModelJAI(
				DataBuffer.TYPE_FLOAT, tileWidth, tileHeight,
				tileWidth * bands, bands, offsets);
		FloatDoubleColorModel ccm =
				new FloatDoubleColorModel(
				COps.colorSpaceFromBands(bands),
				false, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);

		dest = new TiledImage(source.getMinX(), source.getMinY(), width,
				height, source.getMinX(), source.getMinY(), csm, ccm);

		// used to access the source image
		RandomIter iter = RandomIterFactory.create(source, null);

		float ant;

		float sig;
		for (int band = 0; band < bands; band++) {
			for (int line = 0; line < height; line++) {

				// left extreme
				ant = (float) iter.getSample(0, line, band);
				sig = (float) iter.getSample(1, line, band);
				dest.setSample(0, line, band, (sig - ant) / 2.0);

				for (int samp = 1; samp < width - 1; samp++) {

					ant = (float) iter.getSample(samp - 1, line, band);
					sig = (float) iter.getSample(samp + 1, line, band);
					dest.setSample(samp, line, band, (sig - ant) / 2.0);
				}

				// right extreme
				ant = (float) iter.getSample(width - 2, line, band);
				sig = (float) iter.getSample(width - 1, line, band);
				dest.setSample(width - 1, line, band, (sig - ant) / 2.0);
			}
		}

		return dest;
	}


	/**
	 * Differenciates an image by the Y coordinate (rows)
	 * using Central Difference Approximation.
	 *
	 * @param  source  the source image
	 * @return         Description of the Return Value
	 */
	public static PlanarImage old_differenciateY(PlanarImage source) {
		TiledImage dest;

		int bands = source.getSampleModel().getNumBands();
		int height = source.getHeight();
		int width = source.getWidth();
		int tileHeight = source.getTileHeight();
		int tileWidth = source.getTileWidth();

		int offsets[] = new int[bands];
		for (int i = 0; i < bands; i++) {
			offsets[i] = i;
		}
		ComponentSampleModelJAI csm =
				new ComponentSampleModelJAI(
				DataBuffer.TYPE_FLOAT, tileWidth, tileHeight,
				tileWidth * bands, bands, offsets);
		FloatDoubleColorModel ccm =
				new FloatDoubleColorModel(
				COps.colorSpaceFromBands(bands),
				false, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);

		dest = new TiledImage(source.getMinX(), source.getMinY(), width,
				height, source.getMinX(), source.getMinY(), csm, ccm);

		// used to access the source image
		RandomIter iter = RandomIterFactory.create(source, null);

		float ant;

		float sig;
		for (int band = 0; band < bands; band++) {
			for (int samp = 0; samp < width; samp++) {

				// upper border
				ant = (float) iter.getSample(samp, 0, band);
				sig = (float) iter.getSample(samp, 1, band);
				dest.setSample(samp, 0, band, (sig - ant) / 2.0);

				for (int line = 1; line < height - 1; line++) {
					ant = (float) iter.getSample(samp, line - 1, band);
					sig = (float) iter.getSample(samp, line + 1, band);
					dest.setSample(samp, line, band, (sig - ant) / 2.0);
				}

				// bottom border
				ant = (float) iter.getSample(samp, height - 2, band);
				sig = (float) iter.getSample(samp, height - 1, band);
				dest.setSample(samp, height - 1, band, (sig - ant) / 2.0);
			}
		}

		return dest;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  source  Description of the Parameter
	 * @return         Description of the Return Value
	 */
	public static PlanarImage[] gradientVector(PlanarImage source) {
		PlanarImage[] vector = new PlanarImage[2];

		vector[0] = differenciateX(source);
		vector[1] = differenciateY(source);

		return vector;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  gradVector  Description of the Parameter
	 * @return             Description of the Return Value
	 */
	public static PlanarImage gradientDirection(PlanarImage[] gradVector) {
		//PlanarImage gradVector[] = gradientVector(source);
		PlanarImage source = gradVector[0];

		TiledImage dest;
		dest = new TiledImage(source.getMinX(), source.getMinY(), source.getWidth(),
				source.getHeight(), source.getMinX(), source.getMinY(),
				gradVector[0].getSampleModel(),
				gradVector[0].getColorModel());

		int bands = source.getSampleModel().getNumBands();
		int height = source.getHeight();
		int width = source.getWidth();

		// used to access the source image
		RandomIter iterX = RandomIterFactory.create(gradVector[0], null);
		RandomIter iterY = RandomIterFactory.create(gradVector[1], null);

		for (int samp = 0; samp < width; samp++) {
			for (int line = 0; line < height; line++) {
				for (int band = 0; band < bands; band++) {
					dest.setSample(samp, line, band,
							AMath.atangent(
							iterX.getSample(samp, line, band),
							iterY.getSample(samp, line, band)));
				}
			}
		}

		return dest;
	}


	/**
	 * Returns the gradient magnitude of the image
	 *
	 * @param  gradVector  Description of the Parameter
	 * @return             Description of the Return Value
	 */
	public static PlanarImage gradientMagnitude(PlanarImage[] gradVector) {
		//PlanarImage gradVector[] = gradientVector(source);
		PlanarImage source = gradVector[0];

		TiledImage dest;
		dest = new TiledImage(source.getMinX(), source.getMinY(), source.getWidth(),
				source.getHeight(), source.getMinX(), source.getMinY(),
				gradVector[0].getSampleModel(),
				gradVector[0].getColorModel());

		int bands = source.getSampleModel().getNumBands();
		int height = source.getHeight();
		int width = source.getWidth();

		// used to access the source image
		RandomIter iterX = RandomIterFactory.create(gradVector[0], null);
		RandomIter iterY = RandomIterFactory.create(gradVector[1], null);

		for (int samp = 0; samp < width; samp++) {
			for (int line = 0; line < height; line++) {
				for (int band = 0; band < bands; band++) {
					dest.setSample(samp, line, band,
							Math.sqrt(
							Math.pow(iterX.getSample(samp, line, band), 2) +
							Math.pow(iterY.getSample(samp, line, band), 2)));
				}
			}
		}

		return dest;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  x  Description of the Parameter
	 * @param  s  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	public static double gauss(double x, double s) {
		return Math.exp((-x * x) / (2. * s * s)) / s / Math.sqrt(2. * Math.PI);
	}

	
	/**
	 * 2D Gaussian
	 *
	 * @param  x  Description of the Parameter
	 * @param  y  Description of the Parameter
	 * @param  s  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	public static double gauss(double x, double y, double s) {
		return Math.exp(-(x * x + y * y) / (2. * s * s)) / (2. * Math.PI * s * s);
	}

	/** Just the exponential */
	public static double gaussPI(double x, double y, double s) {
		return Math.exp(-(x * x + y * y) / (2. * s * s));
	}


	/**
	 * Gaussian derivative
	 *
	 * @param  x  Description of the Parameter
	 * @param  s  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	public static double gaussD(double x, double s) {
		return -x / Math.sqrt(2. * Math.PI) / Math.pow(s, 3) * Math.exp((-x * x) / (2. * s * s));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  x  Description of the Parameter
	 * @param  y  Description of the Parameter
	 * @param  s  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	public static double gaussDX(double x, double y, double s) {
		return -x / Math.sqrt(2. * Math.PI) / Math.pow(s, 3) * Math.exp(-(x * x + y * y) / (2. * s * s));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  x  Description of the Parameter
	 * @param  y  Description of the Parameter
	 * @param  s  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	public static double gaussDY(double x, double y, double s) {
		return -y / Math.sqrt(2. * Math.PI) / Math.pow(s, 3) * Math.exp(-(x * x + y * y) / (2. * s * s));
	}


	/**
	 * (Computer Vision: A Modern Approach (pag. 209))
	 *
	 * @param  size  Description of the Parameter
	 * @param  s     Description of the Parameter
	 * @return       Description of the Return Value
	 */
	public static double[][] gaussian2Dkernel(int size, double s) {
		if (size < 3) {
			return null;
		}
		if (size % 2 == 0) {
			return null;
		}
		// only odd numbers

		int half = size >> 1;
		double[][] kernel = new double[size][size];
		for (int i = -half; i <= half; i++) {
			for (int j = -half; j <= half; j++) {
				//kernel[i + half][j + half] = gauss(i, j, s);
				// save some computations. We are gonna divide by sum anyway.
				kernel[i + half][j + half] = gaussPI(i, j, s);
			}
		}

		return AMath.mult(kernel, 1. / AMath.sum(kernel));

	}


	/**
	 * Laplacian of the gaussian (Matlab 'fspecial' -> 'log')
	 * The Matlab implementation does adjust the filter coefficients to make sure they sum
	 * to zero as follows:
	 * <pre>
	 *     h = h - sum(h(:))/prod(size(h))
	 * </pre>
	 * The default size: n = ceil(sigma*3) * 2 + 1
	 *
	 * Ref. http://www.cs.ubc.ca/~woodham/cpsc505/examples/log.html
	 *
	 * @param  size  Size of the kernel
	 * @param  s     Sigma
	 * @return       A Laplacian of Gaussian filter.
	 */

	public static double[][] laplacianGaussianKernel(int size, double s) {
		if (size < 3) {
			return null;
		}
		if (size % 2 == 0) {
			return null;
		}
		// only odd numbers

		int half = size >> 1;
		double[][] kernel = new double[size][size];
		for (int i = -half; i <= half; i++) {
			for (int j = -half; j <= half; j++) {
				kernel[i + half][j + half] = gaussPI(i, j, s);
			}
		}
	
		AMath.mult(kernel, 1. / AMath.sum(kernel));		

		for (int i = -half; i <= half; i++) {
			for (int j = -half; j <= half; j++) {
				//double x=(double)i/(double)size;
				//double y=(double)j/(double)size;
				kernel[i + half][j + half] *= (i*i+j*j-2.*s*s)/(s*s*s*s);
			}
		}
		
		AMath.add(kernel,-AMath.sum(kernel)/(double)(size*size));
		
		return kernel;
	}


	/**
	 * @param  s  sigma
	 * @return    Description of the Return Value
	 */
	public static double[][] gaussianKernels(double s) {
		double[] g = new double[20];
		double[] dg = new double[20];
		int i;
		int filterWidth;
		// calculate kernel (s is standard deviation)
		for (i = 0; i < 20; i++) {
			double a = gauss((double) i, s);
			if (a > 0.001 || i < 2) {
				g[i] = a;
				dg[i] = gaussD((double) i, s);
			} else {
				break;
			}
		}
		filterWidth = i;
		double[][] kernels = new double[2][filterWidth];
		for (i = 0; i < filterWidth; i++) {
			kernels[0][i] = g[i];
			kernels[1][i] = dg[i];
		}
		return kernels;
	}

}

