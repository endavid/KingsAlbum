package titech.image.dsp;

import com.sun.media.jai.codec.*;

import java.awt.*;
import java.awt.Transparency;
import java.awt.color.*;
import java.awt.image.*;
import java.awt.image.renderable.*;

import java.io.*;

import java.util.HashMap;
import java.util.Vector;

import javax.media.jai.*;
import javax.media.jai.iterator.*;
import javax.media.jai.operator.*;

import titech.image.math.*;


/**
 * COps = Common Operations.
 * This class comprises a series of static methods in order to invoke <b>JAI</b>
 * operations via an easy-to-use function.
 * @see <a href="http://java.sun.com/products/java-media/jai/forDevelopers/jai1_0_1guide-unc/index.html">JAI manual</a>
 * @author David Gavilan
 */
public class COps {

    /**
    * Applies the wavelet operation to a <b>float</b> image. The result is another
    * float image.
    * @param image the input image
    * @param algorism which algorism to apply (Haar, Shore)
    * @param level number of levels of the DWT Transform
    * @return the output image as a <b>RenderedOp</b>
    * @see hyper.dsp.WaveletOpImage
    */
    public static RenderedOp wavelet(PlanarImage image, String algorism, 
                                     int level) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(algorism);
        pb.add(level);

        return JAI.create("Wavelet", pb);
    }

    /**
    * Applies the inverse wavelet transform to a <b>float</b> image. The result is
    * another float image.
    * @param image the input image
    * @param algorism which algorism to apply (Haar, Shore)
    * @param level number of levels of the DWT Transform
    * @return the output image as a <b>RenderedOp</b>
    * @see hyper.dsp.IWaveletOpImage
    */
    public static RenderedOp iwavelet(PlanarImage image, String algorism, 
                                      int level) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(algorism);
        pb.add(level);

        return JAI.create("IWavelet", pb);
    }

    /**
    * Applies the quantization operation to a <b>float</b> image. The result is
    * a <b>short</b> image.
    * @param image the input image
    * @param algorism which algorism to apply (Uniform, SBUniform, Latice)
    * @param level number of levels of the DWT Transform
    * @param coefs a vector containing the parameters per subband
    * @return the output image as a <b>RenderedOp</b>
    * @see hyper.dsp.QuantizationOpImage
    * @see hyper.dsp.ParamLattice
    */
    public static RenderedOp quantization(PlanarImage image, String algorism, 
                                          int level, Vector[] coefs) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(algorism);
        pb.add(level);
        pb.add(coefs);

        return JAI.create("Quantization", pb);
    }

    /**
    * Applies the dequantization operation to a <b>short</b> image. The result is
    * a <b>float</b> image.
    * @param image the input image
    * @param algorism which algorism to apply (Uniform, SBUniform, Latice)
    * @param level number of levels of the DWT Transform
    * @param coefs a vector containing the parameters per subband
    * @return the output image as a <b>RenderedOp</b>
    * @see hyper.dsp.QuantizationOpImage
    * @see hyper.dsp.ParamLattice
    */
    public static RenderedOp dequantization(PlanarImage image, String algorism, 
                                            int level, Vector[] coefs) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(algorism);
        pb.add(level);
        pb.add(coefs);

        return JAI.create("Dequantization", pb);
    }

    /*  public static RenderedOp lookup(PlanarImage image,) {
        float blurmatrix[] = {1/16f, 1/8f, 1/16f,
                              1/8f,  1/4f, 1/8f,
                  1/16f, 1/8f, 1/16f};
        KernelJAI blurkernel = new KernelJAI(3,3,blurmatrix);
        return JAI.create("lookup", image, blurkernel);
      }*/
	/**
	 * Applies a gaussian kernel over the image.
	 * @param size The size of the kernel matrix.
	 * @param sigma The standard deviation of the gaussian.
	 */
    public static RenderedOp gaussianBlur(PlanarImage image, int size, 
                                           double sigma) {

        double[][] kernel = Difference.gaussian2Dkernel(size, sigma);

        //System.out.println(AMath.showMatrix(kernel));
        if (kernel == null)

            return null;

        KernelJAI blurkernel = new KernelJAI(size, size, AMath.flattenFloat(kernel));
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(blurkernel);

        BorderExtender be = BorderExtender.createInstance(
                                    BorderExtender.BORDER_REFLECT);
        RenderingHints rhints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, be);

        return JAI.create("convolve", pb, rhints);
    }

	public static RenderedOp gaussianBlur(PlanarImage image, double sigma) {
		return gaussianBlur(image, (int)(Math.ceil(sigma*3) * 2 + 1), sigma);
	}
	
	/**
	 * Applies a Laplacian of Gaussian kernel over the image.
	 * @param size The size of the kernel matrix.
	 * @param sigma The standard deviation of the gaussian.
	 */
    public static RenderedOp laplacianGaussian(PlanarImage image, int size, 
                                           double sigma) {

        double[][] kernel = Difference.laplacianGaussianKernel(size, sigma);

        //System.out.println(AMath.showMatrix(kernel));
        if (kernel == null)

            return null;

        KernelJAI blurkernel = new KernelJAI(size, size, AMath.flattenFloat(kernel));
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(blurkernel);

        BorderExtender be = BorderExtender.createInstance(
                                    BorderExtender.BORDER_REFLECT);
        RenderingHints rhints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, be);

        return JAI.create("convolve", pb, rhints);
    }

    public static RenderedOp blur(PlanarImage image) {

        float[] blurmatrix = {
            1 / 16f, 1 / 8f, 1 / 16f,
			1 / 8f,  1 / 4f, 1 / 8f,
			1 / 16f, 1 / 8f, 1 / 16f
        };
        KernelJAI blurkernel = new KernelJAI(3, 3, blurmatrix);

        return JAI.create("convolve", image, blurkernel);
    }

    public static RenderedOp sharpen(PlanarImage image) {

        float[] sharpmatrix = {
            -1 / 16f, -1 / 8f, -1 / 16f, -1 / 8f, 7 / 4f, -1 / 8f, -1 / 16f, 
            -1 / 8f, -1 / 16f
        };
        KernelJAI sharpkernel = new KernelJAI(3, 3, sharpmatrix);

        return JAI.create("convolve", image, sharpkernel);
    }

	public static RenderedOp laplacian(PlanarImage image) {
		float[] laplacianMatrix = {
			0f,  -1f,  0f,
			-1f,  4f, -1f,
		    0f,  -1f, 0f};
		KernelJAI laplacianKernel = new KernelJAI(3,3,laplacianMatrix);
		
		return JAI.create("convolve", image, laplacianKernel);
	}
	
	public static RenderedOp convolve(PlanarImage image, float[] matrix, int x, int y) {
		KernelJAI kernel = new KernelJAI(x, y, matrix);
		
		return JAI.create("convolve", image, kernel);
	}
	
    public static RenderedOp medianFilter(PlanarImage image, 
                                          MedianFilterShape maskShape, 
                                          int maskSize) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(maskShape);
        pb.add(maskSize);

        return JAI.create("MedianFilter", pb);
    }

    public static RenderedOp sobelGradientMagnitude(PlanarImage image) {

        KernelJAI sobelVertKernel = KernelJAI.GRADIENT_MASK_SOBEL_VERTICAL;
        KernelJAI sobelHorizKernel = KernelJAI.GRADIENT_MASK_SOBEL_HORIZONTAL;
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(sobelHorizKernel);
        pb.add(sobelVertKernel);

        return JAI.create("gradientmagnitude", pb);
    }

    public static TiledImage cannyEdge(PlanarImage image) {

        Canny can = new Canny();

        // 3x3 kernel, theta 0.45, 10, 1
        return can.apply_canny(image, 3, 0.45f, 10, 1, 1f, 0);
    }

	/** Changes image dimensions to (x,y) */
	public static RenderedOp scale (RenderedImage image, float x, float y) {
		return COps.scale(image, 
						   x/image.getWidth(),
						   y/image.getHeight(), 0, 0);
	}
    /**
     * x'=(x-transx)/magx
     */
    public static RenderedOp scale(RenderedImage image, float magx, float magy, 
                                   float transx, float transy) {

		InterpolationBilinear interp = new InterpolationBilinear();
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(magx);
        pb.add(magy);
        pb.add(transx);
        pb.add(transy);
        pb.add(interp);

        return JAI.create("scale", pb);
    }

	/** Uses default values: transparency 0, nearest interpolation */
    public static RenderedOp scaleN(RenderedImage image, float magx, float magy) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(magx);
        pb.add(magy);

        return JAI.create("scale", pb);
    }
	
	
	public static RenderedOp crop(PlanarImage img, float x, float y, float w, float h) {
		
		ParameterBlock pb = new ParameterBlock();
		
		pb.addSource(img);
		pb.add(x).add(y).add(w).add(h);
		
		return JAI.create("crop", pb);
	}

	public static RenderedOp crop(PlanarImage img, int x, int y, int w, int h) {		
		return crop(img, (float)x, (float)y, (float)w, (float)h);
	}
	
    public static RenderedOp add(PlanarImage img, double val) {

        int bands = img.getSampleModel().getNumBands();
        double[] cons = new double[bands];

        for (int i = 0; i < bands; i++)
            cons[i] = val;

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(cons);

        return JAI.create("AddConst", pb);
    }

    public static RenderedOp add(PlanarImage img1, PlanarImage img2) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img1);
        pb.addSource(img2);

        return JAI.create("Add", pb);
    }

    public static RenderedOp subtract(PlanarImage img1, PlanarImage img2) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img1);
        pb.addSource(img2);

        return JAI.create("subtract", pb);
    }

    public static RenderedOp DCT(PlanarImage img) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);

        return JAI.create("dct", pb);
    }

    public static RenderedOp inverseDCT(PlanarImage img) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);

        return JAI.create("idct", pb);
    }

    public static RenderedOp absolute(PlanarImage img) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);

        return JAI.create("absolute", pb);
    }

    public static RenderedOp invert(PlanarImage img) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);

        return JAI.create("invert", pb);
    }

	public static RenderedOp binarize(PlanarImage img, double threshold) {
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(img);
		pb.add(threshold);
		
		return JAI.create("binarize", pb);
	}
	
    public static RenderedOp multiply(PlanarImage img, double val) {

        int bands = img.getSampleModel().getNumBands();
        double[] cons = new double[bands];

        for (int i = 0; i < bands; i++)
            cons[i] = val;

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(cons);

        return JAI.create("MultiplyConst", pb);
    }

    public static RenderedOp divide(PlanarImage img1, PlanarImage img2) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img1);
        pb.addSource(img2);

        return JAI.create("Divide", pb);
    }

    public static RenderedOp rescale(PlanarImage img, int factor, int offset) {

        int bands = img.getSampleModel().getNumBands();
        int[] con = new int[bands];
        int[] off = new int[bands];

        for (int i = 0; i < bands; i++) {
            con[i] = factor;
            off[i] = offset;
        }

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(con);
        pb.add(off);

        return JAI.create("rescale", pb);
    }

    /** Rescale samples of the image from by doing <br>
    * <code>dst[x][y][b] = src[x][y][b]*constant + offset</code>
    */
    public static RenderedOp rescale(PlanarImage img, double scale, 
                                     double offset) {

        int bands = img.getSampleModel().getNumBands();
        double[] offsets = new double[bands];
        double[] scales = new double[bands];

        for (int i = 0; i < bands; i++) {
            offsets[i] = offset; // offsets
            scales[i] = scale; // scales
        }

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(scales);
        pb.add(offsets);

        return JAI.create("rescale", pb);
    }

    /** Rescale samples of the image from by doing <br>
    * <code>dst[x][y][b] = src[x][y][b]*constant + offset</code>
    */
    public static RenderedOp rescale(PlanarImage img, double[] scales, 
                                     double[] offsets) {

        int bands = img.getSampleModel().getNumBands();
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(scales);
        pb.add(offsets);

        return JAI.create("rescale", pb);
    }

    /** Find the extrema values of the image */
    public static double[][] extrema(PlanarImage im) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im); // The source image

        //pb.add(roi);        // The region of the image to scan
        pb.add(null); // the entire image
        pb.add(1); // The horizontal sampling rate
        pb.add(1); // The vertical sampling rate

        // Perform the extrema operation on the source image
        RenderedOp op = JAI.create("extrema", pb);

        // Retrieve both the maximum and minimum pixel value
        double[][] extrema = (double[][])op.getProperty("extrema");

        return extrema;
    }

    /** Find the mean value of the image (for each band)*/
    public static double[] mean(PlanarImage im) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im); // The source image

        //pb.add(roi);        // The region of the image to scan
        pb.add(null); // the entire image
        pb.add(1); // The horizontal sampling rate
        pb.add(1); // The vertical sampling rate

        // Perform the extrema operation on the source image
        RenderedOp op = JAI.create("mean", pb);

        // Retrieve both the maximum and minimum pixel value
        double[] mean = (double[])op.getProperty("mean");

        return mean;
    }
	
	
    /** Maximizes contrast of image using linear interpolation.
	  * The image range is supposed to go from 0 to 255 (any datatype).
	  */
    public static RenderedOp maxcon(PlanarImage img) {

        int bands = img.getSampleModel().getNumBands();
        double[][] extrema = extrema(img);
        double[] scales = new double[bands];
        double[] offsets = new double[bands];

        for (int i = 0; i < bands; i++) {
            scales[i] = 255f / (extrema[1][i] - extrema[0][i]);
            offsets[i] = 255f * extrema[0][i] / (extrema[0][i] - 
                         extrema[1][i]);
        }

        return rescale(img, scales, offsets);
    }

    /**
   * Used to change data type of the image databuffer and/or re-tile it.
   * Due to a bug in JAI, data type could not be changed with this operator 
   * until JAI 1.1.1
   * @param img the input image
   * @param tileDim the Dimension of the new tile
   * @param type target image type
   * @return an output image with the desired type and tile size.
   */
    public static RenderedOp reformat(PlanarImage img, Dimension tileDim, 
                                      int type) {

        int tileWidth = tileDim.width;
        int tileHeight = tileDim.height;
        ImageLayout tileLayout = new ImageLayout(img);
        tileLayout.setTileWidth(tileWidth);
        tileLayout.setTileHeight(tileHeight);

        HashMap map = new HashMap();
        map.put(JAI.KEY_IMAGE_LAYOUT, tileLayout);
        map.put(JAI.KEY_INTERPOLATION, 
                Interpolation.getInstance(Interpolation.INTERP_BICUBIC));

        RenderingHints tileHints = new RenderingHints(map);
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(type);

        return JAI.create("format", pb, tileHints);
    }

    /**
   * Just to change the datatype. No tile size change.
   * @param img the input image
   * @param type the destination image type
   * @return the output image
   */
    public static RenderedOp reformat(PlanarImage img, int type) {

        Dimension d = new Dimension(img.getTileWidth(), img.getTileHeight());

        return reformat(img, d, type);
    }

    /** Treating img as the difference between 2 images, computes
    * the Mean Square Error (MSE) per channel
    * @param img the input img, which is supposed to be the difference between two
    * @return a vector containing the Mean Square Error per band
    */
    public static double[] computeMSE(PlanarImage img) {

        int bands = img.getSampleModel().getNumBands();
        int height = img.getHeight();
        int width = img.getWidth();
        double[] mse = new double[bands];

        // used to access the source image
        RandomIter iter = RandomIterFactory.create(img, null);
        double v;

        for (int band = 0; band < bands; band++) {
            mse[band] = 0f;

            for (int i = 0; i < width; i++)

                for (int j = 0; j < height; j++) {
                    v = (double)iter.getSampleFloat(i, j, band);
                    mse[band] += v * v;
                }

            mse[band] /= (double)width * height;
        }

        return mse;
    }

    /**
      * Returns a string representation of datatype constants.
      * @param type the DataBuffer type
      * @return a String representing the type
      */
    public static String getDataTypeName(int type) {

        switch (type) {

            case DataBuffer.TYPE_BYTE:
                return "byte";

            case DataBuffer.TYPE_SHORT:
                return "short";

            case DataBuffer.TYPE_INT:
                return "int";

            case DataBuffer.TYPE_USHORT:
                return "unsigned short";

            case DataBuffer.TYPE_DOUBLE:
                return "double";

            case DataBuffer.TYPE_FLOAT:
                return "float";
        }

        return "unknown";
    }

    /**
    * Saves images to a JPEG file. Quality is set to 0.75 by default.
    */
    public static void saveAsJPG(PlanarImage pimg, String file)
                          throws java.io.IOException {

        OutputStream out = new FileOutputStream(file);
        JPEGEncodeParam param = new JPEGEncodeParam();
        ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG", out, 
                                                             param);
        encoder.encode(pimg);
        out.close();
    }

   /**
    * Saves images to a JPEG file with the desired quality.
	* @param quality desired quality, between 0.0 to 1.0.
	* @see com.sun.media.jai.codec.JPEGEncodeParam
    */
    public static void saveAsJPG(PlanarImage pimg, String file, float quality)
                          throws java.io.IOException {

        OutputStream out = new FileOutputStream(file);
        JPEGEncodeParam param = new JPEGEncodeParam();
		param.setQuality(quality);
        ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG", out, 
                                                             param);
        encoder.encode(pimg);
        out.close();
    }
	
    /**
    * Saves images to a TIFF file
    */
    public static void saveAsTIFF(PlanarImage pimg, String file)
                           throws java.io.IOException {

        OutputStream out = new FileOutputStream(file);
        TIFFEncodeParam param = new TIFFEncodeParam();
        ImageEncoder encoder = ImageCodec.createImageEncoder("TIFF", out, 
                                                             param);
        encoder.encode(pimg);
        out.close();
    }

    /**
    * Saves images to a BMP file
    */
    public static void saveAsBMP(PlanarImage pimg, String file)
                          throws java.io.IOException {

        OutputStream out = new FileOutputStream(file);
        BMPEncodeParam param = new BMPEncodeParam();
        ImageEncoder encoder = ImageCodec.createImageEncoder("BMP", out, param);
        encoder.encode(pimg);
        out.close();
    }

    /**
    * Saves images to a PNM file
    */
    public static void saveAsPNM(PlanarImage pimg, String file)
                          throws IOException {

        OutputStream out = new FileOutputStream(file);
        PNMEncodeParam param = new PNMEncodeParam();
        ImageEncoder encoder = ImageCodec.createImageEncoder("PNM", out, param);
        encoder.encode(pimg);
        out.close();
    }

    /**
    * Saves images to a PNG file
    */
    public static void saveAsPNG(PlanarImage pimg, String file)
                          throws IOException {

        OutputStream out = new FileOutputStream(file);
        PNGEncodeParam param = PNGEncodeParam.getDefaultEncodeParam(pimg);
        ImageEncoder encoder = ImageCodec.createImageEncoder("PNG", out, param);
        encoder.encode(pimg);
        out.close();
    }

	/**
	 * Saves an indexed image to a PNG file.
	 * @param palette A 3*num_colors matrix, in R,G,B order.
	 */
    public static void saveAsPNG(PlanarImage pimg, int[] palette, String file)
                          throws IOException {

        OutputStream out = new FileOutputStream(file);
        PNGEncodeParam.Palette param = new PNGEncodeParam.Palette();
		param.setPalette(palette);
        ImageEncoder encoder = ImageCodec.createImageEncoder("PNG", out, param);
        encoder.encode(pimg);
        out.close();
    }
	 
	
    /**
    * Use this under JAI < 1.1.1 to reformat image. (slow!)
    */
    public static TiledImage toFloat(PlanarImage in) {

        int bands = in.getSampleModel().getNumBands();
        int height = in.getHeight();
        int tileHeight = in.getTileHeight();
        int width = in.getWidth();
        int tileWidth = in.getTileWidth();
        ComponentSampleModelJAI csm = new ComponentSampleModelJAI(
                                              DataBuffer.TYPE_FLOAT, tileWidth, 
                                              tileHeight, tileWidth * bands, 
                                              bands, new int[] { 0, 1, 2 });
        FloatDoubleColorModel ccm = new FloatDoubleColorModel(ColorSpace.getInstance(
                                                                      ColorSpace.CS_sRGB), 
                                                              false, false, 
                                                              Transparency.OPAQUE, 
                                                              DataBuffer.TYPE_FLOAT);
        TiledImage outImage = new TiledImage(in.getMinX(), in.getMinY(), 
                                             in.getWidth(), in.getHeight(), 
                                             in.getMinX(), in.getMinY(), csm, 
                                             ccm);

        // used to access the source image
        RandomIter iter = RandomIterFactory.create(in, null);

        for (int band = 0; band < bands; band++) {

            for (int i = 0; i < width; i++)

                for (int j = 0; j < height; j++) {
                    outImage.setSample(i, j, band, 
                                       (float)iter.getSample(i, j, band));
                }
        }

        return outImage;
    }

    /**
    * Use this under JAI < 1.1.1 to reformat image. (slow!)
    */
    public static TiledImage toByte(PlanarImage in) {

        int bands = in.getSampleModel().getNumBands();
        int height = in.getHeight();
        int tileHeight = in.getTileHeight();
        int width = in.getWidth();
        int tileWidth = in.getTileWidth();
        ComponentSampleModel csm = new ComponentSampleModel(
                                           DataBuffer.TYPE_BYTE, tileWidth, 
                                           tileHeight, tileWidth * bands, 
                                           bands, new int[] { 0, 1, 2 });
        ComponentColorModel ccm = new ComponentColorModel(ColorSpace.getInstance(
                                                                  ColorSpace.CS_sRGB), 
                                                          new int[] { 8, 8, 8 }, 
                                                          false, false, 
                                                          Transparency.OPAQUE, 
                                                          DataBuffer.TYPE_BYTE);
        TiledImage outImage = new TiledImage(in.getMinX(), in.getMinY(), 
                                             in.getWidth(), in.getHeight(), 
                                             in.getMinX(), in.getMinY(), csm, 
                                             ccm);

        // used to access the source image
        RandomIter iter = RandomIterFactory.create(in, null);

        for (int band = 0; band < bands; band++) {

            for (int i = 0; i < width; i++)

                for (int j = 0; j < height; j++) {
                    outImage.setSample(i, j, band, 
                                       (byte)iter.getSampleFloat(i, j, band));
                }
        }

        return outImage;
    }

	/** Converts an image to gray. The input image should have just 3 channels (doesn't work with alpha channel). */
    public static RenderedOp toBW(PlanarImage image) {

        /*     ColorSpace colorSpace = ICC_ColorSpace.getInstance(ColorSpace.CS_GRAY);
        
             ParameterBlock pb = new ParameterBlock();
             pb.addSource(image).add(colorSpace);
        
             // Perform the color conversion.
             return JAI.create("ColorConvert", pb);
        */
        double[][] matrix = { { 0.114D, 0.587D, 0.299D, 0.0D } };

        //    if (i.getSampleModel().getNumBands() != 3) { throw new IllegalArgumentException("Image # bands <> 3"); }
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(matrix);

        return (JAI.create("bandcombine", pb, null));
    }

    /** Converts a 1 band image into 3 band RGB, byte datatype.
     * This is very slow! There should be some predefined JAI operator...
     * (bandcombine? colorconvert?)
     */
    public static PlanarImage toRGB(PlanarImage in) {

        int bands = in.getSampleModel().getNumBands();
        int height = in.getHeight();
        int tileHeight = in.getTileHeight();
        int width = in.getWidth();
        int tileWidth = in.getTileWidth();

        if (bands != 1)

            return in;

        ComponentSampleModel csm = new ComponentSampleModel(
                                           DataBuffer.TYPE_BYTE, tileWidth, 
                                           tileHeight, tileWidth * bands, 
                                           bands, new int[] { 0, 1, 2 });
        ComponentColorModel ccm = new ComponentColorModel(ColorSpace.getInstance(
                                                                  ColorSpace.CS_sRGB), 
                                                          new int[] { 8, 8, 8 }, 
                                                          false, false, 
                                                          Transparency.OPAQUE, 
                                                          DataBuffer.TYPE_BYTE);
        TiledImage outImage = new TiledImage(in.getMinX(), in.getMinY(), 
                                             in.getWidth(), in.getHeight(), 
                                             in.getMinX(), in.getMinY(), csm, 
                                             ccm);

        // used to access the source image
        RandomIter iter = RandomIterFactory.create(in, null);

        for (int band = 0; band < 3; band++)

            for (int i = 0; i < width; i++)

                for (int j = 0; j < height; j++) {
                    outImage.setSample(i, j, band, 
                                       (byte)iter.getSample(i, j, 0));
                }

        return outImage;
    }

	/**
	 * Performs a color conversion. Now it may just work for type byte images.
	 * <pre>r = COps.colorConvert(r, IHSColorSpace.getInstance());</pre>
	 * @param src The source image.
	 * @param cs The desired color space.
	 * @return
	 */
	public static RenderedOp colorConvert(PlanarImage src, ColorSpace cs) {
		// ColorSpace colorSpace = IHSColorSpace.getInstance();
		// BYTE is ok for IHS (HSV) ???
		int dataType = src.getSampleModel().getDataType();
		int[] cSize = src.getColorModel().getComponentSize(); // {8, 8, 8} for BYTE
		ComponentColorModel ccm = new ComponentColorModel(cs, 
														  cSize, 
														  false, false, 
														  Transparency.OPAQUE, 
														  dataType);
		
		// Create the ParameterBlock.
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(src).add(ccm);

		// Perform the color conversion.
		RenderedOp a = JAI.create("ColorConvert", pb);

		return a;
	}
    public static TiledImage colorClassify(PlanarImage image) {

        TiledImage outImage;
        outImage = new TiledImage(image.getMinX(), image.getMinY(), 
                                  image.getWidth(), image.getHeight(), 
                                  image.getMinX(), image.getMinY(), 
                                  image.getSampleModel(), 
                                  image.getColorModel());

        // Loop over the input, copy each pixel to the output, modifying // them as we go
        int bands = image.getSampleModel().getNumBands();
        int height = image.getHeight();
        int width = image.getWidth();

        // used to access the source image
        RandomIter iter = RandomIterFactory.create(image, null);

        // normalized R,G values
        float[] palette = {
            0.7f, // orange
            0.3f, 0.256f, //green
            0.53f, 0.411f, // color carne - color mas concreto, distancia mas pequenya
            0.32f, 0.235f, // azul marino
            0.253f, 0.33333f, //white
            0.33333f
        };
        float[] distances = { 0.02f, 0.035f, 0.0012f, 0.005f, 0.001f };
        int[] lookup = {
            255, 106, 0, 77, 160, 64, 221, 172, 144, 95, 102, 206, 255, 255, 
            255
        };

        for (int samp = 0; samp < width; samp++) {

            for (int line = 0; line < height; line++) {

                float[] v = new float[3];
                float sum = 0f;

                for (int band = 0; band < bands; band++) {

                    int dn = iter.getSample(samp, line, band);
                    v[band] = dn; // whatever
                    sum += dn;
                }

                v[0] /= sum;
                v[1] /= sum;

                // distances to palette colors
                // el orden es importante pq un color se puede parecer a varios de la paletta
                for (int p = 0, k = 0, lk = 0;
                     p < palette.length;
                     lk += 3, p += 2, k++) {

                    if ((v[0] - palette[p]) * (v[0] - palette[p]) + 
                        (v[1] - palette[p + 1]) * (v[1] - palette[p + 1]) < distances[k]) {
                        outImage.setSample(samp, line, 0, lookup[lk]);
                        outImage.setSample(samp, line, 1, lookup[lk + 1]);
                        outImage.setSample(samp, line, 2, lookup[lk + 2]);

                        break;
                    }
                }
            }
        }

        return outImage;
    }

    /**
     * Returns a ColorSpace object according to the number of bands.
     * @param nbands the number of bands of the model
     */
    public static ColorSpace colorSpaceFromBands(int nbands) {

        ColorSpace cs = null;

        switch (nbands) {

            case 1:
                cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);

                break;

            case 3:
                cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);

                break;

            default:

                // F = 15 component
                cs = ColorSpace.getInstance(ColorSpace.TYPE_FCLR);
        }

        return cs;
    }
	
	/** 
	The Threshold operation takes one rendered image, and maps all the pixels of this image whose value falls within a specified range to a specified constant. The range is specified by a low value and a high value.
	*/
	public static RenderedOp threshold(PlanarImage img, 
		double[] low, double[] high, double[] constants) {
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(img);
		pb.add(low);
		pb.add(high);
		pb.add(constants);
		
		return JAI.create("threshold", pb);
	}
	
	public static RenderedOp threshold(PlanarImage img, 
		double low, double high, double constant) {
        int bands = img.getSampleModel().getNumBands();
		
		double[] l = new double[bands];
		double[] h = new double[bands];
		double[] c = new double[bands];
		
		for (int i=0;i<bands;i++) {
			l[i]=low; h[i]=high; c[i]=constant;
		}
		
		return threshold(img, l, h, c);
		
	}
}
