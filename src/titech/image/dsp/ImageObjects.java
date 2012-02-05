package titech.image.dsp;

import java.awt.Transparency;
import java.awt.color.*;
import java.awt.image.*;

import java.util.StringTokenizer;

import javax.media.jai.*;
import javax.media.jai.iterator.*;
import java.io.*;


import titech.image.math.*;

/**
 * This class provides methods for easily create editable image objects.
 *<p>
 * History:<ul>
 * <li> 05/04/02 - tiles are defined with TILEWIDTH, or the maximum size
 *      The problem is that if =(width,height), there are artifacts!, e.g.
 *      for an image 60x80, the image is repeated at 60x60!!! 80x60 ok!???
 * <li> 03/11/13 - Changed loadPalette(File) to loadPalette(InputStream)
 * </ul>
 * @see javax.media.jai.TiledImage
 * @author David Gavilan
 */
public class ImageObjects {

	public static final int TILEWIDTH=512;
	public static final int TILEHEIGHT=512;
	
    /**
     * @param paletteBits the size of image palette in bits. Max number of colors = 2^paletteBits
     */
    public static TiledImage createIndexedImage(int width, int height, 
                                                int paletteBits, 
                                                byte[][] colormap) {

        TiledImage outImage;

        int tileHeight = TILEHEIGHT;
        int tileWidth = TILEWIDTH;
		
        //int tileHeight = (height>=(TILEHEIGHT<<1))?TILEHEIGHT:height;
        //int tileWidth = (width>=(TILEWIDTH<<1))?TILEWIDTH:width;
		
        int paletteSize = colormap[0].length;

        // we should fill the whole byte in the sample model, thus Bits=5 is not possible
        paletteBits = paletteBits > 4 ? 8 : 4;

        SampleModel csm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, 
                                                          tileWidth, 
                                                          tileHeight, 
                                                          paletteBits);
        IndexColorModel icm = new IndexColorModel(paletteBits, paletteSize, 
                                                  colormap[0], colormap[1], 
                                                  colormap[2]);
        outImage = new TiledImage(0, 0, width, height, 0, 0, csm, icm);

        return outImage;
    }

    /**
     * Creates a TiledImage in RGB colorspace.
	 * on PPC: java.lang.ArrayIndexOutOfBoundsException: 15360
     */
    public static TiledImage createRGBImage(int width, int height) {

        TiledImage outImage;

        int tileHeight = TILEHEIGHT;
        int tileWidth = TILEWIDTH;

        //int tileHeight = (height>=(TILEHEIGHT<<1))?TILEHEIGHT:height;
        //int tileWidth = (width>=(TILEWIDTH<<1))?TILEWIDTH:width;
        int bands = 3;

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
        outImage = new TiledImage(0, 0, width, height, 0, 0, csm, ccm);

        return outImage;
    }

	/**
	 * Creates a TiledImage in IHS (HSV) colorspace.
	 */
	public static TiledImage createHSVImage(int width, int height) {

		TiledImage outImage;

        int tileHeight = TILEHEIGHT;
        int tileWidth = TILEWIDTH;
		
        //int tileHeight = (height>=(TILEHEIGHT<<1))?TILEHEIGHT:height;
        //int tileWidth = (width>=(TILEWIDTH<<1))?TILEWIDTH:width;
		int bands = 3;

		ComponentSampleModel csm = new ComponentSampleModel(
										   DataBuffer.TYPE_BYTE, tileWidth, 
										   tileHeight, tileWidth * bands, 
										   bands, new int[] { 0, 1, 2 });
		ComponentColorModel ccm = new ComponentColorModel(IHSColorSpace.getInstance(), 
														  new int[] { 8, 8, 8 }, 
														  false, false, 
														  Transparency.OPAQUE, 
														  DataBuffer.TYPE_BYTE);
		outImage = new TiledImage(0, 0, width, height, 0, 0, csm, ccm);

		return outImage;
	}
	
	/**
	 * Creates a TiledImage in RGB or Gray colorspace with Float data type.
	 */
	public static TiledImage createFloatImage(int width, int height, int bands) {

		TiledImage outImage;

        int tileHeight = TILEHEIGHT;
        int tileWidth = TILEWIDTH;

        //int tileHeight = (height>=(TILEHEIGHT<<1))?TILEHEIGHT:height;
        //int tileWidth = (width>=(TILEWIDTH<<1))?TILEWIDTH:width;

		int[] off;
		int cs;
		if (bands==3) {
			off=new int[]{0, 1, 2};
			cs = ColorSpace.CS_sRGB;
		} else {
			off=new int[]{0};
			cs=ColorSpace.CS_GRAY;
		}

		ComponentSampleModelJAI csm = new ComponentSampleModelJAI(
										  DataBuffer.TYPE_FLOAT, tileWidth, 
										  tileHeight, tileWidth * bands, 
										  bands, off);
		FloatDoubleColorModel ccm = new FloatDoubleColorModel(ColorSpace.getInstance(cs), 
														  false, false, 
														  Transparency.OPAQUE, 
														  DataBuffer.TYPE_FLOAT);
		outImage = new TiledImage(0, 0, width, height, 0, 0, csm, ccm);

		return outImage;
	}

    public static TiledImage RGB2Indexed(PlanarImage image, int paletteBits, 
                                         byte[][] colormap) {

        TiledImage outImage;
        int bands = image.getSampleModel().getNumBands();
        int height = image.getHeight();
        int width = image.getWidth();
        int tileHeight = image.getTileHeight();
        int tileWidth = image.getTileWidth();
        int paletteLength = colormap[0].length;

        // we should fill the whole byte in the sample model, thus Bits=5 is not possible
        paletteBits = paletteBits > 4 ? 8 : 4;

        SampleModel csm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, 
                                                          tileWidth, 
                                                          tileHeight, 
                                                          paletteBits);
        IndexColorModel icm = new IndexColorModel(paletteBits, paletteLength, 
                                                  colormap[0], colormap[1], 
                                                  colormap[2]);
        outImage = new TiledImage(image.getMinX(), image.getMinY(), 
                                  image.getWidth(), image.getHeight(), 
                                  image.getMinX(), image.getMinY(), csm, icm);

        if (bands != 3)

            return outImage;

        // Loop over the input, copy each pixel to the output, modifying them as we go
        // used to access the source image
        RandomIter iter = RandomIterFactory.create(image, null);

        //float[] hsb = {0f, 0f, 0f};
        for (int samp = 0; samp < width; samp++) {

            for (int line = 0; line < height; line++) {

                int red = iter.getSample(samp, line, 0);
                int green = iter.getSample(samp, line, 1);
                int blue = iter.getSample(samp, line, 2);

                //Color.RGBtoHSB(red, green, blue, hsb);
                // probamos ahora con una simple distancia euclidea
                // en el espacio RGB
                outImage.setSample(samp, line, 0, 
                                   minEuclidean(colormap, red, green, blue));
            }
        }

        return outImage;
    }

	public static TiledImage Indexed2RGB(PlanarImage image, int paletteBits, 
                                         byte[][] colormap) {
		TiledImage outImage = createRGBImage(image.getWidth(), image.getHeight());

        // Loop over the input, copy each pixel to the output, modifying them as we go
        // used to access the source image
        RandomIter iter = RandomIterFactory.create(image, null);

        for (int samp = 0; samp < image.getWidth(); samp++) {
            for (int line = 0; line < image.getHeight(); line++) {

				int i = iter.getSample(samp, line, 0);
                int red = (256+(int)colormap[0][i])%256;
                int green = (256+(int)colormap[1][i])%256;
                int blue = (256+(int)colormap[2][i])%256;

                outImage.setSample(samp, line, 0, red);
                outImage.setSample(samp, line, 1, green);
                outImage.setSample(samp, line, 2, blue);
            }
        }
		
		return outImage;
	}

	public static byte[][] reorderMap(byte[][] colormap, int[] order) {
		byte[][] map = new byte[3][colormap[0].length];
		
		// assume color 0 is black
		for (int i=1;i<=order.length;i++) {
			for (int c=0;c<3;c++) {
				map[c][order[i-1]]=colormap[c][i];
			}
		}
		
		return map;
	}
	
	
	public static TiledImage Indexed2HSV(PlanarImage image, int paletteBits, 
										 byte[][] colormap) {
		TiledImage outImage = createHSVImage(image.getWidth(), image.getHeight());

		// Loop over the input, copy each pixel to the output, modifying them as we go
		// used to access the source image
		RandomIter iter = RandomIterFactory.create(image, null);

		for (int samp = 0; samp < image.getWidth(); samp++) {
			for (int line = 0; line < image.getHeight(); line++) {

				int i = iter.getSample(samp, line, 0);
				int red = (256+(int)colormap[0][i])%256;
				int green = (256+(int)colormap[1][i])%256;
				int blue = (256+(int)colormap[2][i])%256;

				outImage.setSample(samp, line, 0, red);
				outImage.setSample(samp, line, 1, green);
				outImage.setSample(samp, line, 2, blue);
			}
		}
		
		return outImage;
	}
	
    /** @return 0..categories.length-1, index of one of the categories */
    public static int minEuclidean(byte[][] categories, int r, int g, int b) {

        int i = 0;
        int index = 0;
        int dr = r - (256+(int)categories[0][i])%256;
        int dg = g - (256+(int)categories[1][i])%256;
        int db = b - (256+(int)categories[2][i])%256;
        int distance = dr * dr + dg * dg + db * db;
        int d = 0;

        for (i = 1; i < categories[0].length; i++) {
            //System.out.println(
            //        "---- (r,g,b)=" + (256+(int)categories[0][i])%256 + " "
			//		+ (256+(int)categories[1][i])%256 + " " 
			//		+ (256+(int)categories[2][i])%256 + " "+" i: " + i);
            dr = r - (256+(int)categories[0][i])%256;
            dg = g - (256+(int)categories[1][i])%256;
            db = b - (256+(int)categories[2][i])%256;
            d = dr * dr + dg * dg + db * db;

            if (d < distance) {
                index = i;
                distance = d;
            }
        }

        //System.out.println(
        //        "(r,g,b)=" + r + " " + g + " " + b + " index: " + index);

        return index;
    }
	
	public static byte[][] loadPalette(InputStream stream)
                     throws FileNotFoundException {
		byte[][] colormap=new byte[3][32];

        BufferedReader br = new BufferedReader(new InputStreamReader(stream));

        try {

            for (int i = 0;i<32; i++) {

                String str = br.readLine();
                StringTokenizer tzer = new StringTokenizer(str);
                int red = Integer.parseInt(tzer.nextToken());
                int green = Integer.parseInt(tzer.nextToken());
                int blue = Integer.parseInt(tzer.nextToken());
				colormap[0][i]=(byte)red;
				colormap[1][i]=(byte)green;
				colormap[2][i]=(byte)blue;				
            }
        } catch (IOException exc) {
            System.out.println("All colors from file read. ");
        }
         catch (Exception exc) {
            System.err.println("loadPalette: " + exc);
        }
		
		return colormap;
    }
	
	public static byte[][] paletteRGBPartition(int rbin, int gbin, int bbin) {
		int nbins = rbin*gbin*bbin;
		byte[][] pal = new byte[3][nbins];
		
		int k=0;
		for (int b=0;b<bbin;b++)
		for (int g=0;g<gbin;g++)
		for (int r=0;r<rbin;r++) {
			pal[0][k]=(byte)(r*255/rbin);
			pal[1][k]=(byte)(g*255/gbin);
			pal[2][k]=(byte)(b*255/bbin);
			k++;
		}
		
		return pal;
	}
	
	public static byte[][] paletteHSVPartition(int hbin, int sbin, int vbin, int gray) {
		int nbins = hbin*sbin*vbin+gray;
		byte[][] pal = new byte[3][nbins];
		
		int k=0;
		for (int v=1;v<=vbin;v++)
		for (int s=0;s<sbin;s++)
		for (int h=0;h<hbin;h++) {
			pal[0][k]=(byte)(v*255/vbin); // intensity
			pal[1][k]=(byte)(h*255/hbin); // hue
			pal[2][k]=(byte)(s*255/sbin); // saturation
			
			k++;
		}
		for (int g=0;g<gray;g++) {
			pal[0][k]=(byte)((g+1)*255/(gray+2)); //intensity
			pal[1][k]=0;
			pal[2][k]=0;
			k++;
		}
		return pal;
	}
	
	/** tries getting the palette from a PlanarImage (assumes indexed) */
	public static byte[][] paletteFromImage(PlanarImage pim) {
		IndexColorModel icm = (IndexColorModel) pim.getColorModel();
		int nbins = icm.getMapSize();
		byte[][] pal = new byte[3][nbins];
		icm.getReds(pal[0]);
		icm.getGreens(pal[1]);
		icm.getBlues(pal[2]);
		
		return pal;
	}
	
	/** Adds a n black bins to a color map */
	public static byte[][] addBins(byte[][] colormap, int n) {
		int nbins = colormap[0].length;
		byte[][] pal= new byte[3][nbins+n];
		for (int i=0;i<nbins;i++) 
			for (int b=0;b<3;b++) pal[b][n+i]=colormap[b][i];
		
		return pal;
	}
	
	/**
	 * @see ColorManipulation.posterize
	 * @param bands
	 * @param levels levels of each band
	 * @return
	 */
	public static byte[][] palettePosterize(int bands, int[] levels) {
		byte[][] lut = new byte[bands][256];
		double[] lev=new double[levels.length];
		
		for (int i=0;i<levels.length;i++) lev[i] = (levels[i]<2)?2.0:(double)levels[i];
		for (int v=0;v<256;v++) {
			double vv=(double)v/255.0;
			for (int i=0;i<bands;i++) {
				double val = 255.0*Math.rint(vv*(lev[i]-1.0))/(lev[i]-1.0) + 0.5;
				int value = 0;
				if (val>=255) value=0xFF;
				else if (val>0) value=(int)val;
				lut[i][v]=(byte)value;
			}
		}

		return lut;
	}

	public static byte[][] palettePosterizeShrink(int[] levels) {
		byte[][] res=new byte[3][levels[0]*levels[1]*levels[2]];
		double[] lev=new double[levels.length];
		
		for (int i=0;i<levels.length;i++) lev[i] = (levels[i]<2)?2.0:(double)levels[i];
		
		int i=0;
		for (int r=0; r<lev[0]; r++) {
			double vv=(double)r/(lev[0]-1.0);
			double val = 255.0*Math.rint(vv*(lev[0]-1.0))/(lev[0]-1.0) + 0.5;
			int red = 0;
			if (val>=255) red=0xFF;
			else if (val>0) red=(int)val;
			for (int g=0; g<lev[1]; g++) {
				vv=(double)g/(lev[1]-1.0);
				val = 255.0*Math.rint(vv*(lev[1]-1.0))/(lev[1]-1.0) + 0.5;
				int green = 0;
				if (val>=255) green=0xFF;
				else if (val>0) green=(int)val;
				
				for (int b=0; b<lev[2]; b++) {
					vv=(double)b/(lev[2]-1.0);
					val = 255.0*Math.rint(vv*(lev[2]-1.0))/(lev[2]-1.0) + 0.5;
					int blue = 0;
					if (val>=255) blue=0xFF;
					else if (val>0) blue=(int)val;
					
					res[0][i]=(byte)red;
					res[1][i]=(byte)green;
					res[2][i]=(byte)blue;
					i++;
				}
			}
		}
		
		return res;
	}
	
	public static double[][] palettePosterizeD(int bands, int[] levels, double[] amplitude) {
		double[][] lut = new double[bands][256];
		double[] lev=new double[levels.length];
		
		for (int i=0;i<levels.length;i++) lev[i] = (levels[i]<2)?2.0:(double)levels[i];
		for (int v=0;v<256;v++) {
			double vv=(double)v/255.0;
			for (int i=0;i<bands;i++) {
				double val = amplitude[i]*Math.rint(vv*(lev[i]-1.0))/(lev[i]-1.0) + 0.5;
				lut[i][v]=val;
			}
		}

		return lut;
	}


	public static byte[][] paletteGray(int ext) {
		byte[][] colormap = new byte[3][ext+1];
	
		int step = 255 / ext;
		
		for (int i=0;i<=ext;i++) {
			for (int c=0;c<3;c++) colormap[c][i]=(byte)(i*step);
		}
		
		return colormap;
	}
	
	/**
	 *  Generates a palette in rainbow order.
	 *
	 * @param  ext  Number of colors of the palette.
	 * @return      Generated palette of <code>byte[3][ext+1]</code>.
	 */
	public static byte[][] paletteRainbow(int ext) {
		byte[][] colormap = new byte[3][ext+1];
		
		int period = ext / 5;
		// 5 intervals
		int lastPeriod = ext - 4*period;
		int j = 1;

		//System.err.println("ImageObjects:  ext = " + ext);
		// red to yellow
		for (int i = 0; i < period; i++, j++) {
			colormap[0][j] = (byte) 255;
			colormap[1][j] = (byte) (255 * i / period);
			colormap[2][j] = (byte) 0;
		}
		// yellow to green
		for (int i = 0; i < period; i++, j++) {
			colormap[0][j] = (byte) (255 * (period - i) / period);
			colormap[1][j] = (byte) 255;
			colormap[2][j] = (byte) 0;
		}
		// green to cyan
		for (int i = 0; i < period; i++, j++) {
			colormap[0][j] = (byte) 0;
			colormap[1][j] = (byte) 255;
			colormap[2][j] = (byte) (255 * i / period);
		}
		// cyan to blue
		for (int i = 0; i < period; i++, j++) {
			colormap[0][j] = (byte) 0;
			colormap[1][j] = (byte) (255 * (period - i) / period);
			colormap[2][j] = (byte) (255 * i / period);
		}
		// blue to magenta
		for (int i = 0; i < lastPeriod; i++, j++) {
			colormap[0][j] = (byte) (255 * i / lastPeriod);
			colormap[1][j] = (byte) 0;
			colormap[2][j] = (byte) 255;
		}
		
		//System.err.println("ImageObjects:  rainbow regions = " + j);

		return colormap;
	}


	/**
	 *  Generates a random palette.
	 *
	 * @param  ext  Number of colors of the palette.
	 * @return      Description of the Return Value
	 */
	public static byte[][] paletteRandom(int ext) {
		byte[][] colormap = new byte[3][ext];
		
		for (int i = 0; i < ext; i++) {
			double tmp = Math.random();
			int rgb = (int) (tmp * 16777215);

			colormap[0][i] = (byte) ((rgb & 0x00ff0000)>>16);
			colormap[1][i] = (byte) ((rgb & 0x0000ff00)>>8);
			colormap[2][i] = (byte) (rgb & 0x000000ff);

			if (i == 0) {
				colormap[0][i] = colormap[1][i] = colormap[2][i] = 0;
			}
		}

		return colormap;
	}
	
	/**
	 * This is not the standard way to show colors.
	 * @param plane X+Y+Z=plane. Usually, X+Y+Z=1.
	 * @see createXYDiagram
	 */
	public static TiledImage createXYPlane(int size, double plane) {
		TiledImage result = createRGBImage(size, size);
		
		float length=(float)(Math.sqrt(2.)*plane);
		float step = (float)(Math.sqrt(2.)*plane/(double)size); 
		float[] xyz = new float[3];
		float[] low = new float[] {0f, 0f, 0f};
		float[] top = new float[] {1f, 1f, 1f};
				
		int i,j; float x,y;
		for (i=0, x=0; i<size; i++, x+=step)
		for (j=0, y=0; j<size; j++, y+=step) {
			xyz[0]=x/length; xyz[1]=y/length; xyz[2]=(float)plane-(x+y)/length;
			float[] rgb = ColorManipulation.XYZtoRGB(xyz);
			//System.out.println("("+x+","+y+")"+AMath.showVector(xyz)+" : "+AMath.showVector(rgb));
			if (AMath.inRange(low, rgb, top)) { // check if it's a valid RGB point
				float[] srgb=ColorManipulation.RGBtosRGB(rgb);
				for (int k=0; k<3; k++) {
					int c = (int)(255.*srgb[k]);
					result.setSample(i, size-1-j, k, c);
				}
			} else { // otherwise, pixels will be white (erase for let them black)
				for (int k=0; k<3; k++) {
					result.setSample(i, size-1-j, k, 255);
				}
			}
		}
		
		return result;
	}
	
	
	/**
	 * This is the standard Yxy plane.
	 * @param vY is the lightness.
	 */
	public static TiledImage createXYDiagram(int size, float vY) {
		TiledImage result = createRGBImage(size, size);
		
		float step = (float)(1./(double)size); 
		float[] xyz = new float[3]; // this is actually XYZ 
		float[] low = new float[] {0f, 0f, 0f};
		float[] top = new float[] {1f, 1f, 1f};
		int gridSize = (int)(0.1*size);
		
		int i,j; float x,y;
		xyz[1]=vY;
		for (i=0, x=0; i<size; i++, x+=step) 		  
		  for (j=1, y=step; j<size; j++, y+=step) { // skip y=0
			xyz[0]=(float)(x*vY/y);
			xyz[2]=(float)((1.-(x+y))*vY/y);
			
			float[] rgb = ColorManipulation.XYZtoRGB(xyz);
			//System.out.println("("+x+","+y+")"+AMath.showVector(xyz)+" : "+AMath.showVector(rgb));
			// If you always want a triangle, trim the top
			AMath.trimTop(rgb, top);
			if (AMath.inRange(low, rgb, top)) { // check if it's a valid RGB point
				float[] srgb=ColorManipulation.RGBtosRGB(rgb);
				for (int k=0; k<3; k++) {
					int c = (int)(255.*srgb[k]);
					result.setSample(i, size-1-j, k, c);
				}
			} else { // otherwise, pixels will be white, or black for the grid
				if (i%gridSize!=0 && j%gridSize!=0) {
					for (int k=0; k<3; k++) {
						result.setSample(i, size-1-j, k, 255);
					}
				}
			}
		  }
	
		
		
		return result;		
	}

	/**
	 * This is the standard Yxy plane.
	 * @param size The resulting size of the image.
	 * @param source Where to take the samples from.
	 */
	public static TiledImage createXYDiagram(int size, PlanarImage source) {
	int gridSize = (int)(0.1*size);
	TiledImage result = createGrid(size, size, gridSize, gridSize);

        int bands = source.getSampleModel().getNumBands();
        int height = source.getHeight();
        int width = source.getWidth();
		
		if (bands<3) {
			System.err.println("XYDiagram: No color image!");
			return result;
		}

		float[] srgb = new float[] {0f, 0f, 0f};
		double x=0, y=0;
        RandomIter iter = RandomIterFactory.create(source, null);
        for (int samp = 0; samp < width; samp++) {
            for (int line = 0; line < height; line++) {

                int red = iter.getSample(samp, line, 0);
                int green = iter.getSample(samp, line, 1);
                int blue = iter.getSample(samp, line, 2);
				
				srgb[0]=(float)red; srgb[1]=(float)green; srgb[2]=(float)blue;
				float[] rgb=ColorManipulation.sRGBtoRGB(srgb);
				float[] xyz=ColorManipulation.RGBtoXYZ(rgb);
				x=xyz[0]/(xyz[0]+xyz[1]+xyz[2]);
				y=xyz[1]/(xyz[0]+xyz[1]+xyz[2]);
				
				int i=(int)(x*size);
				int j=(int)(y*size);
				result.setSample(i, size-1-j, 0, red);
				result.setSample(i, size-1-j, 1, green);
				result.setSample(i, size-1-j, 2, blue);
			}
		}
		
		
		return result;		
	}

	public static TiledImage createGrid(int width, int height, int stepx, int stepy) {
	  TiledImage result = createRGBImage(width, height);
	  for (int x=0; x<width; x++) {
	     for (int y=0;y<height; y++) {
	        // paint all the pixels white, except those from the grid
		if (x%stepx!=0 && y%stepy!=0) {
		   for (int c=0;c<3;c++)
		     result.setSample(x,height-1-y,c,255);
		}
	     }
	  }

	  return result;
	}
}

