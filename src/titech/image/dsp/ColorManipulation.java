package titech.image.dsp;

import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import javax.media.jai.*;
import javax.media.jai.iterator.*;
import java.io.*;

import java.awt.image.renderable.ParameterBlock;

import titech.image.math.*; 

/** This class provides with static methods for Color Manipulation, including Color Correction
  * and Color Labeling.
  */
public class ColorManipulation {

	public static final float GW_THRESHOLD = 90f;
	public static final float WW_THRESHOLD = 0.01f;
	/** default bin sizes for color histograms used when comparing images. */
	public static final int RED_BINS=6, GREEN_BINS=6, BLUE_BINS=5;
	/** These values are from VisualSeek */
	public static final int HUE_BINS=18,SAT_BINS=3, VAL_BINS=3, GRAY_BINS=4;	
	/** White Point */
	public static float[] white={ 246f, 252f, 230f };

	
	/**
	  * Applies the modified white world assumption to solve the color constancy problem.
	  * In this approach, I leave untouched those images that don't fall onto the
	  * gray world assumption (very dark images).
	  */
	public static PlanarImage modWhiteInGray(PlanarImage img) {
		double media = 0f;

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img); // The source image

        //pb.add(roi);        // The region of the image to scan
        pb.add(null); // the entire image
        pb.add(1); // The horizontal sampling rate
        pb.add(1); // The vertical sampling rate

        // Perform the mean operation on the source image
        RenderedOp op = JAI.create("mean", pb);

        // Retrieve the mean pixel value at each channel
        double[] mean = (double[])op.getProperty("mean");
		
		for (int i=0;i<mean.length;i++) {
			media += mean[i];
			// OK! :)
			//System.out.println("mean "+mean[i]);
		}
		media /= mean.length;
		
		if (media <= GW_THRESHOLD) return img;
		
		return modWhite(img);
	}
	
	/**
	  * Corrects image color under the modified White World Assumption.
	  * @see javax.media.jai.operator.HistogramDescriptor;
	  * @see javax.media.jai.Histogram;
	  */
	public static RenderedOp modWhite(PlanarImage img) {
        int bands = img.getSampleModel().getNumBands();
        double[] cons = new double[bands];

		ParameterBlock pb = new ParameterBlock();
        pb.addSource(img); // The source image

        //pb.add(roi);        // The region of the image to scan
        pb.add(null); // the entire image
        pb.add(1); // The horizontal sampling rate
        pb.add(1); // The vertical sampling rate
		// those are the default values, so it's not necessary
        //pb.add(new int[] {256, 256, 256}); // numBins
        //pb.add(new double[] {0, 0, 0}); // lowValue
		//pb.add(new double[] {256, 256, 256}); // highValue

		
		RenderedOp op = JAI.create("histogram",pb);
		
		Histogram histo = (Histogram)op.getProperty("histogram");
		
		int[][] bins = histo.getBins();
		double npix = img.getWidth()*img.getHeight();
		
		double maxw[]={255, 255, 255};
		for (int i = 0; i < bands; i++) {
			double suma = 0;
			while(maxw[i]>0 && suma<WW_THRESHOLD) {
				suma += ((double)bins[i][(int)maxw[i]])/npix;
				maxw[i]--;
			}
		}
				
		for (int i = 0; i < bands; i++) {
            cons[i] = white[i]/maxw[i];
			//System.out.println("gain "+cons[i]);
		}
		
        pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(cons);

        return JAI.create("MultiplyConst", pb);

	}
	
	
	/** This considers each band apart (independent probabilities), so the resulting
	 * vector is just of size <code>RED_BINS + GREEN_BINS + BLUE_BINS</code>.
	 */
	public static Histogram getHistogram(PlanarImage src) {
		return getHistogram(src, new int[] {RED_BINS, GREEN_BINS, BLUE_BINS});
	}
	
	public static Histogram getHistogram(PlanarImage src, int[] bins) {
		ParameterBlock pb = new ParameterBlock();
        pb.addSource(src); // The source image

        //pb.add(roi);        // The region of the image to scan
        pb.add(null); // the entire image
        pb.add(1); // The horizontal sampling rate
        pb.add(1); // The vertical sampling rate
        pb.add(bins); // numBins
		// those are the default values, so it's not necessary
        //pb.add(new double[] {0, 0, 0}); // lowValue
		//pb.add(new double[] {256, 256, 256}); // highValue
		
		RenderedOp op = JAI.create("histogram",pb);
		
		return (Histogram)op.getProperty("histogram");
	}
	
	/**
	 * Gives the euclidean distance between two images by comparing their histograms.
	 */
	public static double histogramDistance(PlanarImage a, PlanarImage b, 
		int[] bins) {
			int[] numBins;
			if (bins==null) {
				numBins = new int[] {RED_BINS, GREEN_BINS, BLUE_BINS};
			} else if (bins.length!=3) {
				System.err.println("ColorManipulation: using default bin sizes (6,6,5)");
				numBins = new int[] {RED_BINS, GREEN_BINS, BLUE_BINS};
			} else {
				numBins = bins;
			}
			
			Histogram histoa=getHistogram(a, numBins);
			Histogram histob=getHistogram(b, numBins);

			int[][] ha = histoa.getBins();
			double na = a.getWidth()*a.getHeight();
			int[][] hb = histob.getBins();
			double nb = b.getWidth()*b.getHeight();
			
			double sum = 0;
			for (int band=0;band<3;band++)
			for (int bin=0;bin<numBins[band];bin++) {
				double f=((double)ha[band][bin]/na-(double)hb[band][bin]/nb);
				sum+=f*f;
			}
			return Math.sqrt(sum);
	}
	
	
	/**
	 * Resulting size is <code>RED_BINS * GREEN_BINS * BLUE_BINS</code>.
	 */
	public static Histogram getColorHistogram(PlanarImage src) {
		byte[][] palette = ImageObjects.paletteRGBPartition(RED_BINS, GREEN_BINS, BLUE_BINS);
		
		int nbins = RED_BINS * GREEN_BINS * BLUE_BINS;
		
		PlanarImage quantized = ImageObjects.RGB2Indexed(src, 8, palette);
		
		ParameterBlock pb = new ParameterBlock();
        pb.addSource(quantized); // The source image

        //pb.add(roi);        // The region of the image to scan
        pb.add(null); // the entire image
        pb.add(1); // The horizontal sampling rate
        pb.add(1); // The vertical sampling rate
        pb.add(new int[] {nbins}); // numBins
        pb.add(new double[] {0}); // lowValue
		pb.add(new double[] {nbins}); // highValue
		
		RenderedOp op = JAI.create("histogram",pb);
		
		return (Histogram)op.getProperty("histogram");		
	}
	/**
	 * The input image is supposed to be a RGB image, and this function converts it to HSV color space.
	 * <p>
	 * Resulting size is <code>HUE_BINS * SAT_BINS * VAL_BINS + GRAY_BINS</code>.
	 */
	public static Histogram getHSVColorHistogram(PlanarImage src) {
		byte[][] palette = ImageObjects.paletteHSVPartition(HUE_BINS, SAT_BINS, VAL_BINS, GRAY_BINS);
		
		int nbins =HUE_BINS * SAT_BINS * VAL_BINS + GRAY_BINS;
		
		RenderedOp rop = COps.colorConvert(src, IHSColorSpace.getInstance());
		PlanarImage quantized = ImageObjects.RGB2Indexed(rop, 8, palette);
		
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(quantized); // The source image

		//pb.add(roi);        // The region of the image to scan
		pb.add(null); // the entire image
		pb.add(1); // The horizontal sampling rate
		pb.add(1); // The vertical sampling rate
		pb.add(new int[] {nbins}); // numBins
		pb.add(new double[] {0}); // lowValue
		pb.add(new double[] {nbins}); // highValue
		
		RenderedOp op = JAI.create("histogram",pb);
		
		return (Histogram)op.getProperty("histogram");		
	}
	/**
	 * Gives the euclidean distance between two images by comparing their histograms.
	 */
	public static double colorHistogramDistance(PlanarImage a, PlanarImage b) {
			
			Histogram histoa=getColorHistogram(a);
			Histogram histob=getColorHistogram(b);

			int[] ha = histoa.getBins(0);
			double na = a.getWidth()*a.getHeight();
			int[] hb = histob.getBins(0);
			double nb = b.getWidth()*b.getHeight();
			
			double sum = 0;
			for (int bin=0;bin<ha.length;bin++) {
				double f=((double)ha[bin]/na-(double)hb[bin]/nb);
				sum+=f*f;
			}
			return Math.sqrt(sum);
	}
	
	/**
	 * Posterizes the source image using a look-up table. The posterize operation reduce the number of colors
	 * on an image.
	 * @param src The source image.
	 * @param levels Number of levels of posterize (>=2). Does not include black and white.
	 * @return The posterized image.
	 */
	public static RenderedOp posterize(PlanarImage src, int[] levels) {
		int bands = src.getSampleModel().getNumBands();

		byte[][] lut = ImageObjects.palettePosterize(bands, levels);
		LookupTableJAI table = new LookupTableJAI(lut);

		ParameterBlock pb = new ParameterBlock();
		pb.addSource(src); // The source image
		pb.add(table);

		RenderedOp op = JAI.create("lookup",pb);

		return op;
	}
	
	/** Applies a simple color transformation swaping Hue and Saturation with the filter image.
	 * The intensity remains the one from the original image.
	 */
	public static PlanarImage changeColor(PlanarImage src, PlanarImage filter) {
		ColorSpace colorSpace = IHSColorSpace.getInstance();
		// BYTE is ok for IHS (HSV) ???
        ComponentColorModel ccm = new ComponentColorModel(colorSpace, 
                                                          new int[] { 8, 8, 8 }, 
                                                          false, false, 
                                                          Transparency.OPAQUE, 
                                                          DataBuffer.TYPE_BYTE);
		
		// Create the ParameterBlock.
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(src).add(ccm);

		// Perform the color conversion.
     	RenderedOp a = JAI.create("ColorConvert", pb);

		pb = new ParameterBlock();
		pb.addSource(filter).add(ccm);
		RenderedOp b = JAI.create("ColorConvert", pb);

		pb = new ParameterBlock();
		pb.addSource(a).add(new double[]{1,0,0});
		a = JAI.create("MultiplyConst",pb);

		pb = new ParameterBlock();
		pb.addSource(b).add(new double[]{0,1,1});
		b = JAI.create("MultiplyConst",pb);
		
		pb = new ParameterBlock();
		pb.addSource(a).addSource(b);
		RenderedOp dst = JAI.create("Add",pb);
		
		pb = new ParameterBlock();
		pb.addSource(dst).add(src.getColorModel());
		dst = JAI.create("ColorConvert", pb);
		
		System.out.println("... Color changed ...");
		return dst;
	}
	
	/**
	  * Save the R,G,B values from pixels from source, selected by mask. It just masks
	  * index 0 from the indexed image.
	  * @param source The source RGB image
	  * @param mask An indexed image.
	  * @param bw The stream.
	  */
	public static void saveMaskedSamples(PlanarImage source, PlanarImage mask, 
		BufferedWriter bwsamples) throws java.io.IOException {

        int height = source.getHeight();
        int width = source.getWidth();

        // used to access the source image
        RandomIter iter = RandomIterFactory.create(source, null);
        // used to access the mask image
        RandomIter iMask = RandomIterFactory.create(mask, null);

		
        for (int i = 0; i < width; i++)
        for (int j = 0; j < height; j++) {
			int r = (int)(iter.getSample(i,j,0));
			int g = (int)(iter.getSample(i,j,1));
			int b = (int)(iter.getSample(i,j,2));
			
			int mm = (int)(iMask.getSample(i,j,0));
			
			// 0 (background) is masked
			if (mm>0) {
				String sample = ""+r+" "+g+" "+b+" "+mm;
				bwsamples.write(sample,0,sample.length());
				bwsamples.newLine();
			}
        }

	}
	
	/**
	 * Ref. http://www.brucelindbloom.com/index.html?Equations.html
	 * Expects RGB normalized input.
	 * 
	 */
	public static float[] RGBtoXYZ(float[] rgb) {
		// it was transposed! now (05/01/15) OK
		float[][] M= new float[][] { 
			new float[] {0.412424f, 0.357579f, 0.180464f},
			new float[] {0.212656f, 0.715158f, 0.0721856f},
			new float[] {0.0193324f, 0.119193f, 0.950444f}};
		return AMath.fmultV(rgb, M);
	}
  
 
	public static float[] XYZtoRGB(float[] xyz) {
		// it was transposed! now OK
		float[][] M= new float[][] {
		new float[] {3.24071f, -1.53726f, -0.498571f}, 
		new float[] {-0.969258f, 1.87599f, 0.0415557f},   
		new float[] {0.0556352f, -0.203996f, 1.05707f}};
		
		return AMath.fmultV(xyz, M);
	}

	
	/** Chang */
	public static float[] RGBtosRGB(float[] rgb) {
		float[] srgb = new float[3];
		if ( rgb[0] > 0.00304f ) 
			srgb[0] =(float) (1.055 * Math.pow(rgb[0],(1.0/2.4)) - 0.055);
		else
			srgb[0] = (float)(12.92 * rgb[0]);
		if ( rgb[1] > 0.00304f )
			srgb[1] = (float)(1.055 * Math.pow(rgb[1],(1.0/2.4)) - 0.055);
		else
			srgb[1] = (float)(12.92 * rgb[1]);
		if ( rgb[2] > 0.00304f )
			srgb[2] = (float)(1.055 * Math.pow(rgb[2],(1.0/2.4)) - 0.055);
		else
			srgb[2] = (float)(12.92 * rgb[2]);
	
		return srgb;
	}
	
	/** Expects normalized values. */
	public static float[] sRGBtoRGB(float[] srgb) {
		float[] rgb = new float[3];
		if ( srgb[0] > 0.04045 ) rgb[0] =(float)Math.pow((srgb[0]+0.055)/1.055,2.4);
		else rgb[0] = (float)(srgb[0] / 12.92);
		if ( srgb[1] > 0.04045 ) rgb[1] =(float)Math.pow((srgb[1]+0.055)/1.055,2.4);
		else rgb[1] = (float)(srgb[1] / 12.92);
		if ( srgb[2] > 0.04045 ) rgb[2] =(float)Math.pow((srgb[2]+0.055)/1.055,2.4);
		else rgb[2] = (float)(srgb[2] / 12.92);
		
		return rgb;
	}
	
}

