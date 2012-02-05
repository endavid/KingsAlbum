package titech.image.dsp;

import javax.media.jai.*;
import javax.media.jai.iterator.*;

import java.util.*;
import titech.image.math.*;

/**
 * An image composed of labelled regions. Each region has a bunch of features associated, such as color or center of gravity.
 * <p>
 * History:<ul>
 * <li>05/04/02  Changed the background from black to gray ("unknown")
 * <li>04/05/07. Clustering stuff.
 * <li>03/12/10. <b>associate</b>: Solved an error that caused some regions be divided in two.
 * <li>03/12/02. <b>label</b>: erased and changed by <b>colorLabel</b>, non-recursive version.
 * <li>03/11/26. <b>label</b>: Took unmodified variables outside the recursion call. Solved Stack Overflow.
 * </ul>
 *
 * @author     David Gavilan
 * @created    2003/11/04
 */
public class ObjectImage {

	public static final int NFEATURES=13;
	
	private byte[][] colormap = new byte[3][256];
	/** The number of regions of this object, including the background */
	private int regions = 0;
	/** The number of regions that were discarded during the process */
	private int discarded = 0;
	private TiledImage labeledImage = null;
	/** The original source image (if exists) */
	private PlanarImage original= null;
	private int[] equivalences = null;

	/** Points per region */
	int[] n;
	/** Volume of each region, normalized between 0 and 1. */
	double[] vol;
	/** Extrema points of the bounding box of each region */
	int[] minx;
	int[] maxx;
	// ini 0
	int[] miny;
	int[] maxy;
	// ini 0
	int[] momentX;
	int[] momentY;
	double[] momentXX;
	double[] momentXY;
	double[] momentYY;
	/**
	 * Color: Average. Normalized between 0 and 1.
	 */
	double[][] meanColor;
	/**
	 * Color: Standard Deviation
	 */
	double[][] deviation;
	/**
	 * Color: Third root of Skewness
	 */
	double[][] skewness;
	double[] centerX;
	double[] centerY;
	double[] relativeVol;
	int[] colorCategory; // index 0 is not the background, but the first region

	/** The class of each region after clustering */
	int[] objClass;
	
	/** The number of classes */
	int nClasses = 0;

	/** The minimum acceptable size of a region, in % respect the size of the image.
	  * Default = 1% */
	double volAcceptance = 0.01;
	
	
	// volume relative to its BB

	/**
	 *  Gets the labeledImage attribute of the ObjectImage object
	 *
	 * @return    The labeledImage value
	 */
	public TiledImage getLabeledImage() {
		return labeledImage;
	}


	/**
	 *  Gets the nRegions attribute of the ObjectImage object
	 *
	 * @return    The nRegions value
	 */
	public int getNRegions() {
		return regions;
	}


	/**
	 *  Gets the width attribute of the ObjectImage object
	 *
	 * @return    The width value
	 */
	public int getWidth() {
		return labeledImage.getWidth();
	}


	/**
	 *  Gets the height attribute of the ObjectImage object
	 *
	 * @return    The height value
	 */
	public int getHeight() {
		return labeledImage.getHeight();
	}


	/** Gets the coordinates of a given blob, mapped in a 1x1 square */
	public double[] getCoords(int i) {
		return new double[] {centerX[i], centerY[i]};
	}
	
	/**
	 *  Gets the colormap attribute of the ObjectImage object
	 *
	 * @return    The colormap value
	 */
	public byte[][] getColormap() {
		return colormap;
	}


	/**
	 * Get Color Cagories of each region.
	 *
	 * @return    The categories value
	 */
	public int[] getCategories() {
		return colorCategory;
	}


	/**
	 * @param  reg  Description of the Parameter
	 */
	public ObjectImage(PlanarImage reg) {
		this(reg, null);
		colormap = ImageObjects.paletteRainbow(regions);
	}


	/** Default threshold = 0.01 */
	public ObjectImage(PlanarImage reg, PlanarImage original) {
		this(reg, original, 0.01);
	}
	
	/**
	 * Constructor for the ObjectImage object
	 *
	 * @param  reg       Indexed PlanarImage
	 * @param  original  Full RGB Image, needed for calculating the mean color
	 * @param  threshold The minimum size for taking a region into accound
	 */
	public ObjectImage(PlanarImage reg, PlanarImage original, double threshold) {
		volAcceptance = threshold;
		this.original = original;
		labeledImage = colorLabel(reg);
		calculateFeatures(original);
		discarded = discardRegions();
		// insignificant regions are removed
		//System.err.println("ObjectImage: " + regions + " regions, " + discarded + " discarded.");
		meanColorPalette();
	}

	public String toString() {
		return "ObjectImage: "+ (regions-1) + " regions, "+ discarded + " discarded.";
	}
	

	/**
	 * Labels Color regions in an indexed image to another indexed image.
	 * <p>
	 * The labeling of a pixed <code>p</code>, which <code>index(p)</code> is not 0 (background color index) occurs as follows:
	 * <ul>
	 * <li>If all four neighbors are different from <code>index(p)</code>, assign a new label to <code>p</code>, else
	 * <li>if only one neighbor has the same color, assign its label to <code>p</code>, else
	 * <li>if one or more of the neighbors have the same color, assign one of the labels to <code>p</code> and make a note of the equivalences.
	 *</ul>
	 * <p>
	 * The original algorithm for binary images can be found in: <a href="http://www.dai.ed.ac.uk/HIPR2/label.htm">Connected Components Algorithm</a>
	 *
	 * @param  img  Input image, supposed to be indexed. Otherwise, only red channel is used.
	 * @return      A labelled indexed image.
	 */
	private TiledImage colorLabel(PlanarImage img) {
		int width = img.getWidth();
		int height = img.getHeight();
		int paletteBits = 8;

		RandomIter src = RandomIterFactory.create(img, null);

		TiledImage tim = ImageObjects.createIndexedImage(width, height, paletteBits, colormap);

		int[][] dst = new int[width][height];
		// neighbours
		int[] nb = new int[4];
		// neighbours' labels
		int[] nbl = new int[4];

		equivalences = new int[width * height];
		// no more groups!
		// equivalents to themselves
		for (int i = 0; i < equivalences.length; i++) {
			equivalences[i] = i;
		}

		// the first label
		int label = 1;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int color = src.getSample(x, y, 0);
				if (color == 0) {
					dst[x][y] = 0;
				} else {
					nb[0] = getSample(src, x - 1, y, width, height);
					nb[1] = getSample(src, x, y - 1, width, height);
					nb[2] = getSample(src, x - 1, y - 1, width, height);
					nb[3] = getSample(src, x + 1, y - 1, width, height);

					nbl[0] = getSample(dst, x - 1, y);
					nbl[1] = getSample(dst, x, y - 1);
					nbl[2] = getSample(dst, x - 1, y - 1);
					nbl[3] = getSample(dst, x + 1, y - 1);

					if (nb[0] != color && nb[1] != color && nb[2] != color && nb[3] != color) {
						dst[x][y] = label++;
					} else {
						// count neighbours with the same color
						int count = 0;
						int found = -1;
						for (int i = 0; i < 4; i++) {
							if (nb[i] == color) {
								count++;
								found = i;
							}
						}
						dst[x][y] = nbl[found];
						if (count > 1) {
							for (int i = 0; i < 4; i++) {
								if (nb[i] == color && nbl[i] != dst[x][y]) {
									//System.out.println("("+x+","+y+")="+nbl[i]+"="+dst[x][y]);
									associate(nbl[i], dst[x][y]);
								}
							}
						}
					}
				}
			}
		}

		//reduce labels ie 76=23=22=3 -> 76=3
		//done in reverse order to preserve sorting
		for (int i = label - 1; i > 0; i--) {
			equivalences[i] = reduce(i);
			//System.out.println("equiv: "+i+"="+equivalences[i]);
		}

		/*
		 *  now labels will look something like 1=1 2=2 3=2 4=2 5=5.. 76=5 77=5
		 *  this needs to be condensed down again, so that there is no wasted
		 *  space eg in the above, the labels 3 and 4 are not used instead it jumps
		 *  to 5.
		 */
		int condensed[] = new int[label];
		// cant be more labels
		//System.err.println("labels: "+label);
		int count = 0;
		for (int i = 0; i < label; i++) {
			if (i == equivalences[i]) {
				condensed[i] = count++;
			}
		}
		// Record the number of labels
		regions = count;
		// that includes the background

		colorCategory = new int[regions - 1];
		// no background

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int i = condensed[equivalences[dst[x][y]]];
				tim.setSample(x, y, 0, i);
				if (i > 0) {
					colorCategory[i - 1] = src.getSample(x, y, 0);
				}
			}
		}

		equivalences = null;

		return tim;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  a  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	private int reduce(int a) {

		if (equivalences[a] == a) {
			return a;
		} else {
			return reduce(equivalences[a]);
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  a  Description of the Parameter
	 * @param  b  Description of the Parameter
	 */
	private void associate(int a, int b) {
		if (a > b) {
			associate(b, a);
			return;
		}
		if (a == b || equivalences[b] == a) {
			return;
		}
		if (equivalences[b] == b) {
			equivalences[b] = a;
		} else {
			associate(equivalences[b], a);
			equivalences[b] = a;
		}
	}


	/**
	 *  Gets the sample attribute of the ObjectImage object
	 *
	 * @param  src  Description of the Parameter
	 * @param  x    Description of the Parameter
	 * @param  y    Description of the Parameter
	 * @param  w    Description of the Parameter
	 * @param  h    Description of the Parameter
	 * @return      The sample value
	 */
	private int getSample(RandomIter src, int x, int y, int w, int h) {
		return ((x < 0) || (x >= w) || (y < 0) || (y >= h)) ? 0 : src.getSample(x, y, 0);
	}


	/**
	 *  Gets the sample attribute of the ObjectImage object
	 *
	 * @param  src  Description of the Parameter
	 * @param  x    Description of the Parameter
	 * @param  y    Description of the Parameter
	 * @return      The sample value
	 */
	private int getSample(int[][] src, int x, int y) {
		int w = src.length;
		int h = src[0].length;
		return ((x < 0) || (x >= w) || (y < 0) || (y >= h)) ? 0 : src[x][y];
	}


	/**
	 *  Description of the Method
	 */
	public void calculateFeatures() {
		calculateFeatures(null);
	}


	/**
	 * Accepts region 0 (background) or regions bigger that 1% of the image by default.
	 * Change volAcceptance. 
	 *
	 * @param  i  Description of the Parameter
	 * @return    Acceptable region (significant).
	 */
	private boolean acceptable(int i) {
		return (vol[i] > volAcceptance || i == 0);
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	private int discardRegions() {
		int count = 0;
		for (int i = 0; i < regions; i++) {
			if (acceptable(i)) {
				count++;
			}
		}
		if (regions == count) {
			return 0;
		}

		double[][] features = getFeatures();
		int nfeats = features[0].length;
		double[][] condFeat = new double[count - 1][nfeats];
		int[] colors = new int[count - 1];
		int[] equivalences = new int[regions];
		int[] npix = new int[count]; // number of pixels of new regions

		npix[0]=n[0];
		// skip background (0), i.e., background "accepted"
		for (int i = 1, j = 1; i < regions; i++) {
			if (acceptable(i)) {
				npix[j]=n[i];
				for (int n = 0; n < nfeats; n++) {
					condFeat[j - 1][n] = features[i - 1][n];
				}
				colors[j - 1] = colorCategory[i - 1];
				equivalences[i] = j;
				j++;
			} else {
				equivalences[i] = 0; // background color for insignificant regions
				npix[0]+=n[i]; // add their volume to the background region
			}
		}

		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				int i = labeledImage.getSample(x, y, 0);
				labeledImage.setSample(x, y, 0, equivalences[i]);
			}
		}

		int discard = regions - count;
		regions = count;
		setFeatures(condFeat);
		colorCategory = colors;
		n = npix;
		vol[0]=(double)n[0]/(double) (getWidth() * getHeight());

		return discard;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  img  The original image, used to compute the mean color of that region.
	 */
	public void calculateFeatures(PlanarImage img) {
		int width = labeledImage.getWidth();
		int height = labeledImage.getHeight();

		n = new int[regions];
		// numero de punts per regio
		vol = new double[regions];
		// volume
		minx = new int[regions];
		for (int i = 0; i < regions; i++) {
			minx[i] = width;
		}
		maxx = new int[regions];
		// ini 0
		miny = new int[regions];
		for (int i = 0; i < regions; i++) {
			miny[i] = height;
		}
		maxy = new int[regions];
		// ini 0
		momentX = new int[regions];
		momentY = new int[regions];
		momentXX = new double[regions];
		momentXY = new double[regions];
		momentYY = new double[regions];
		meanColor = new double[regions][3];
		deviation = new double[regions][3];
		skewness = new double[regions][3];
		centerX = new double[regions];
		centerY = new double[regions];
		relativeVol = new double[regions];
		// volume relative to its BB

		RandomIter iter = null;
		if (img != null) {
			iter = RandomIterFactory.create(img, null);
		}

		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				// get index or color
				int r = labeledImage.getSample(i, j, 0);
				n[r]++; // we count even the pixels of the background
				//System.err.println("i "+i+" j "+j+" r "+r);
				if (r > 0 && r < regions) {
					// if r>= regions it means it has badly labeled!
					// 0 is the background (unknown regions)
					if (i < minx[r]) {
						minx[r] = i;
					}
					if (i > maxx[r]) {
						maxx[r] = i;
					}
					if (j < miny[r]) {
						miny[r] = j;
					}
					if (j > maxy[r]) {
						maxy[r] = j;
					}
					momentX[r] += i;
					momentY[r] += j;

					if (iter != null) {
						// se supone una RGB image sino petara
						meanColor[r][0] += iter.getSample(i, j, 0);
						meanColor[r][1] += iter.getSample(i, j, 1);
						meanColor[r][2] += iter.getSample(i, j, 2);
					}
				}
			}
		}

		for (int r = 1; r < regions; r++) {
			if (n[r] > 0) {
				// normalized 0..1
				for (int c = 0; c < 3; c++) {
					meanColor[r][c] /= n[r];
				}
			}
		}
		if (iter != null) {
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					// get index or color
					int r = labeledImage.getSample(i, j, 0);
					//System.err.println("i "+i+" j "+j+" r "+r);
					if (r > 0 && r < regions) {
						for (int c = 0; c < 3; c++) {
							double p = iter.getSample(i, j, c) - meanColor[r][c];
							deviation[r][c] += p * p;
							skewness[r][c] += p * p * p;
						}
					}
				}
			}
		}
		for (int r = 1; r < regions; r++) {
			if (n[r] > 0) {
				// mass centre
				centerX[r] = (double) momentX[r] / (double) n[r];
				centerY[r] = (double) momentY[r] / (double) n[r];
				// normalized 0..1
				for (int c = 0; c < 3; c++) {
					meanColor[r][c] /= 255;
					deviation[r][c] = Math.sqrt(deviation[r][c] / n[r])/255.0;
					skewness[r][c] = AMath.qbic(skewness[r][c] / n[r])/255.0;
				}

				// calculate 2nd momentums inside bounding box
				for (int j = miny[r]; j < maxy[r]; j++) {
					for (int i = minx[r]; i < maxx[r]; i++) {
						int regio = labeledImage.getSample(i, j, 0);
						if (r == regio) {
							double pX = (double) i - centerX[r];
							double pY = (double) j - centerY[r];
							momentXX[r] += pX * pX;
							momentXY[r] += pX * pY;
							momentYY[r] += pY * pY;
						}
					}
				}
				momentXX[r] /= n[r];
				momentXY[r] /= n[r];
				momentYY[r] /= n[r];

				int w = maxx[r] - minx[r] + 1;
				int h = maxy[r] - miny[r] + 1;

				//normalize 0..1
				if (w > 0) {
					momentXX[r] = Math.sqrt(momentXX[r]) / (double) w;
				}
				if (h > 0) {
					momentYY[r] = Math.sqrt(momentYY[r]) / (double) h;
				}
				if (w * h > 0) {
					relativeVol[r] = (double) n[r] / (double) (w * h);
				}

				// -1..1
				momentXY[r] /= n[r];
				// 0..1
				momentXY[r] = (momentXY[r] + 1) / 2;
				if (momentXY[r] > 1) {
					momentXY[r] = 1;
				}
				if (momentXY[r] < 0) {
					momentXY[r] = 0;
				}
			}

			// normalized characteristics 0..1
			vol[r] = (double) n[r] / (double) (width * height);

			//feat.color = feat.color / 255;
			// mapped to a 1x1 image
			centerX[r] /= (double) width;
			centerY[r] /= (double) height;
		}

		vol[0]=(double)n[0]/(double) (width * height);
	}


	/**
	 * @return    an array with this features: cx, cy, vol, rvol, r, g, b.
	 *         Each row is a blob.
	 */
	public double[][] getFeatures() {
		double[][] features = new double[regions - 1][13];

		// we skip region 0 (background)
		for (int i = 1; i < regions; i++) {
			features[i - 1][0] = centerX[i];
			features[i - 1][1] = centerY[i];
			features[i - 1][2] = vol[i];
			features[i - 1][3] = relativeVol[i];
			features[i - 1][4] = meanColor[i][0];
			features[i - 1][5] = meanColor[i][1];
			features[i - 1][6] = meanColor[i][2];
			features[i - 1][7] = deviation[i][0];
			features[i - 1][8] = deviation[i][1];
			features[i - 1][9] = deviation[i][2];
			features[i - 1][10] = skewness[i][0];
			features[i - 1][11] = skewness[i][1];
			features[i - 1][12] = skewness[i][2];

		}

		return features;
	}


	/**
	 *  Sets the features attribute of the ObjectImage object
	 *
	 * @param  features  The new features value
	 */
	private void setFeatures(double[][] features) {
		// we skip region 0 (background)
		for (int i = 1; i < regions; i++) {
			centerX[i] = features[i - 1][0];
			centerY[i] = features[i - 1][1];
			vol[i] = features[i - 1][2];
			relativeVol[i] = features[i - 1][3];
			meanColor[i][0] = features[i - 1][4];
			meanColor[i][1] = features[i - 1][5];
			meanColor[i][2] = features[i - 1][6];
			deviation[i][0] = features[i - 1][7];
			deviation[i][1] = features[i - 1][8];
			deviation[i][2] = features[i - 1][9];
			skewness[i][0] = features[i - 1][10];
			skewness[i][1] = features[i - 1][11];
			skewness[i][2] = features[i - 1][12];
		}
	}


	/**
	 * Creates a comparison table of blob features of this image and another reference image.<p>
	 * The red channel of the resulting image represents how similar in color 2 blobs are.
	 * Green is used for comparing volume. Blue represents blob's position. Thus, a white pixel(x,y) means that blob(x) from this image and blob(y) from argument image have exactly the same features.
	 *
	 * @param  obi  The image to compare with.
	 * @return      a very small image that represents a table comparison of blobs.
	 */
	public TiledImage compareImages(ObjectImage obi) {
		double[][] fa = this.getFeatures();
		double[][] fb = obi.getFeatures();

		int blobsa = fa.length;

		int blobsb = fb.length;

		// we add 2 more pixels, for the legend axis
		TiledImage res = ImageObjects.createRGBImage(blobsb + 2, blobsa + 2);
		for (int y = 0; y < blobsa; y++) {
			for (int x = 0; x < blobsb; x++) {
				double position = (fa[y][0] - fb[x][0]) * (fa[y][0] - fb[x][0]) +
						(fa[y][1] - fb[x][1]) * (fa[y][1] - fb[x][1]);
				position = Math.sqrt(position / 2.0);
				double volume = (fa[y][2] - fb[x][2]) * (fa[y][2] - fb[x][2]) +
						(fa[y][3] - fb[x][3]) * (fa[y][3] - fb[x][3]);
				volume = Math.sqrt(volume / 2.0);
				double color = 0;
				for (int fe = 4; fe<13; fe++) {
					color += (fa[y][fe] - fb[x][fe]) * (fa[y][fe] - fb[x][fe]);
				}
				color = Math.sqrt(color / 9.0);

				if (color > 1 || volume > 1 || position > 1) {
					System.err.println("ObjectImage: [compare >1] " + color + ":" + volume + ":" + position);
				}
				res.setSample(x, y, 0, (int) (255.0 * (1.0 - color)));
				res.setSample(x, y, 1, (int) (255.0 * (1.0 - volume)));
				res.setSample(x, y, 2, (int) (255.0 * (1.0 - position)));
			}
		}

		// legend axis
		byte[][] cmap = colormap;
		for (int y = 0; y < blobsa; y++) {
			for (int i = 0; i < 3; i++) {
				res.setSample(blobsb + 1, y, i, cmap[i][y + 1]);
			}
		}

		cmap = obi.getColormap();
		for (int x = 0; x < blobsb; x++) {
			for (int i = 0; i < 3; i++) {
				res.setSample(x, blobsa + 1, i, cmap[i][x + 1]);
			}
		}

		return res;
	}


	/**
	 *  Gets the descriptor attribute of the ObjectImage object
	 *
	 * @return    The descriptor value
	 */
	public String getDescriptor() {
		SortedSet sorter = new TreeSet();
		// we skip region 0 (background)
		for (int i = 1; i < regions; i++) {
			// they will be put in order
			sorter.add(blobDescriptor(i));
		}

		String descriptor = "";
		Iterator it = sorter.iterator();
		while (it.hasNext()) {
			descriptor += (String) it.next();
			if (it.hasNext()) {
				descriptor += ".";
			}
		}

		System.err.println("ObjectImage: descriptor: " + (regions - 1) + " - " + descriptor);
		return descriptor;
	}


	/**
	 *  This method is returns a descriptor of the blob.<br>
	 *  This descriptor is a String intented to be used in a database.<br>
	 *  <p>
	 *  The format of the Descriptor is as follows:
	 *  <p>
	 *  ABCDE, where:<br>
	 *    A - hexadecimal value representing the quantized horizontal position<br>
	 *    B - hexadecimal value representing the quantized vertical position<br>
	 *    C - hexadecimal value representing the quantized volume<br>
	 *    D - hexadecimal value representing the quantized relative volume<br>
	 *    E - hexadecimal value representing the color category<br>
	 *
	 * @param  index  Index from the blob list.
	 * @return        The Descriptor.
	 */
	public String blobDescriptor(int index) {
		String descriptor = "";

		//System.err.println("["+centerX[index]+","+centerY[index]+","
		//	+ vol[index] + ","+relativeVol[index]
		//	+ ", " + colorCategory[index-1]+"]");

		// values are supposed to be normalized between 0 and 1 !!!!
		descriptor += unit2hex(centerX[index]);
		descriptor += unit2hex(centerY[index]);
		descriptor += unit2hex(vol[index]);
		descriptor += unit2hex(relativeVol[index]);
		descriptor += int2hex(colorCategory[index-1]); // no back
		
		return descriptor;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  unit  Description of the Parameter
	 * @return       Description of the Return Value
	 */
	public String unit2hex(double unit) {
		return int2hex((int) Math.floor(16.0 * unit));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  part  Description of the Parameter
	 * @return       Description of the Return Value
	 */
	public String int2hex(int part) {
		if (part < 10) {
			return "" + part;
		} else if (part < 11) {
			return "A";
		} else if (part < 12) {
			return "B";
		} else if (part < 13) {
			return "C";
		} else if (part < 14) {
			return "D";
		} else if (part < 15) {
			return "E";
		} else if (part <= 16) {
			return "F";
		}
		return "x";
	}


	/**
	 * Maps the mean color of each region to colormap.
	 *
	 * @return    Description of the Return Value
	 */
	public byte[][] meanColorPalette() {
		for (int i = 0; i < regions; i++) {
			for (int j = 0; j < 3; j++) {
				int col = (int) (255.0 * meanColor[i][j]);
				colormap[j][i] = (byte) col;
			}
		}
		// background color
		colormap[0][0]=80;
		colormap[1][0]=80;
		colormap[2][0]=80;
		
		return colormap;
	}


	/**
	 * Given a SOM network, classifies each region given its features into a cluster.
	 */
	public void regionClustering(titech.nn.SOM xarxa) {
		double [][] feats = getFeatures();
		objClass = new int[feats.length];
		nClasses = xarxa.getNout();
		
		for (int i=0;i<feats.length;i++) {
			objClass[i]=xarxa.selectWinnerNeuron(feats[i]);
		}
		
	}
	
	/**
	 * Remaps the indexes of the regions to its corresponding class.
	 */
	public TiledImage getClusterImage(int paletteBits, byte[][] colormap) {
		TiledImage tim = ImageObjects.createIndexedImage(getWidth(), getHeight(), paletteBits, colormap);
		
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				int reg = labeledImage.getSample(x, y, 0);
				if (reg>0) tim.setSample(x, y, 0, objClass[reg-1]+1);
			}
		}
		
		return tim;
	}


	/** The sum of volumes of every class */
	public double[] getClusterHistogram() {
		if (nClasses < 1) return null;
		double[] h = new double[nClasses];
		for (int i=0;i<objClass.length;i++) {
			h[objClass[i]]+=vol[i+1];
		}
		return h;
	}
	
	/** The distance between the pixel color and the mean color of that region, normalized
	  between 0 and 1. So actually, it's not the error, but 1-error.
	*/
	public TiledImage getError() {
		if (original == null) return null;
		TiledImage errorImage = ImageObjects.createFloatImage(getWidth(), getHeight(), 1);
		RandomIter iter = RandomIterFactory.create(original, null);
		for (int j = 0; j < getHeight(); j++) {
			for (int i = 0; i < getWidth(); i++) {
				// get index or color
				int r = labeledImage.getSample(i, j, 0);
				//System.err.println("i "+i+" j "+j+" r "+r);
				if (r > 0 && r < regions) {
					double p=0;
					for (int c = 0; c < 3; c++) {
						double val = (double)iter.getSample(i, j, c)/255.0 - meanColor[r][c];
						p += val*val;
					}
					p = Math.sqrt(p/3);
					errorImage.setSample(i, j, 0, 1.0-p);
				}
			}
		}
		return errorImage;
	}
	
	
	/**
	 * Evaluation function for segmentation. <p>
	 * <em>Colour Image Science</em>, pag. 188<p>
	 * From the original formula:<p>
	 * <pre>F(I)=1/(1000 N * M) sqrt(R) sum(e_i ^2, sqrt(A_i))</pre>
	 * I supress the image size since every value is normalized to a 1x1 image.
	 * Also, e_i is defined as the euclidean distance between the color of that region
	 * and the mean color of the region. This error measure can not be applied directly 
	 * with neural networks categorization, so the error vector has become a parameter.
	 * For neural networks, it will be the output signal value.
	 * Suggested external command: <code>segeval</code>
	 * @see titech.image.IPPanel
	 * @return a value that evaluates the segmentation ability of this algorithm.
	 *               The smaller the value, the better the segmentation.
	 */
	public double evaluationF(TiledImage errorImage, double catThreshold) {
		double[] catError = categorizationError(errorImage, catThreshold);
		double f=Math.sqrt((double)regions);
		double sum=0; 	
		for (int i=0;i<regions;i++) { //a big background should penalize
			if (vol[i]>0) sum+=catError[i]*catError[i]/Math.sqrt(vol[i]);
			System.out.println("catError: "+catError[i]+" vol: "+vol[i]);
		}
		//System.out.println("sin: "+(sum));
		//sum+=catError[0]*catError[0]/Math.sqrt(vol[0]);
		System.out.println("con: "+(sum));
		return f*sum;
	}
	private double[] categorizationError(TiledImage eImage, double threshold){
		double[] catError = new double[regions];
		
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				int i = labeledImage.getSample(x, y, 0); // 0 is the background region
				catError[i] += (1.0-eImage.getSampleFloat(x, y, 0));
			}
		}
		// normalize
		for (int i=0;i<regions;i++) {
			catError[i]/=n[i];
			//System.err.println("catError["+i+"] = "+catError[i]);
		} 
		// for the background, we'll add a constant error, since it has not been classified at all!
		catError[0]=1.0; // += dependiente del threshold?
		//System.err.println("vol[0]: "+vol[0]);
		return catError;
	}
	
	/** Orders blobs regarding the distance to the center (0.5, 0.5),
	 *  and returns the closer one.
	 */
	public int getCentralBlob() {
		int c=1; // first candidate. Ignore background as candidate.
		double[] center = new double[] {0.5, 0.5};
		double[] cand = new double[] {centerX[c], centerY[c]};
		double d = AMath.distance(cand, center);
		
		for (int i=2; i<regions; i++) {
			cand[0]=centerX[i]; cand[1]=centerY[i];
			double newd = AMath.distance(cand, center);
			if (newd < d) {
				d = newd;
				c=i;
			}
		}
		
		return c;
	}
	
	
	public static final int FOVEAL_LEVELS=6;
	/**
	  * Orders the blobs regarding its distance to the fovealBlob (the one we use
	  * as center), and its angle, clockwise ordered.
	  * The distance is discretized in a logaritmic way. The farther, the more rough.
	  * Using polar coordinates is inspired by foveal vision.
	  */
	public int[] fovealOrdering(int fovealBlob) {
		PolarPoint fp = new PolarPoint(0,0);
		TreeMap tmap = new TreeMap(fp);
		double[] center = getCoords(fovealBlob);
		double[] cand = new double[2];
		double size = (1<<FOVEAL_LEVELS); // 2^6 = 64
		
		tmap.put(fp, new Integer(fovealBlob)); // the first will be the center
		
		for (int i=1;i<regions;i++) { // when we insert i==fovealBlob, it will be replced
			cand[0]=centerX[i]; cand[1]=centerY[i];
			double d = AMath.distance(cand, center);
			double theta = Math.atan2(cand[0]-center[0],cand[1]-center[1]);
			// discretize distances
			d = Math.floor(Math.log(1.+size*d)/Math.log(2.));
			tmap.put(new PolarPoint(d, theta), new Integer(i));
		}
		
		Collection col = tmap.values();
		int[] list = new int[col.size()];
		Iterator it = col.iterator();
		int i = 0;
		while (it.hasNext()) {
			list[i++] = ((Integer)it.next()).intValue();
		}
		
		return list;
	}
}

