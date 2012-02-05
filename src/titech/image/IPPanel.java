package titech.image;

import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.media.jai.*;
import javax.media.jai.iterator.*;
import javax.media.jai.registry.*;
import java.text.NumberFormat;
import java.util.*;
import java.io.*;
import titech.image.dsp.*;
import titech.image.math.*;
import titech.nn.*;
import titech.file.*;

/**
 *  A Panel that implements a command interpreter that contains different Image
 *  Processing functions contained in this API.
 * 
 * Version refers to both IPPanel and the scripting language defined here.
 * Version: 1.10 
 * <p><h5>
 * Last modified: 2005/01/26 (jedit)
 * </h5>
 * @author     David Gavilan
 * @created    2003/12/15
 */
public class IPPanel extends JPanel {

	/**
	 * Feedback text area
	 */
	JTextArea log;
	/**
	 * Area for the user to input commands
	 */
	JTextField command;
	/**
	 * File dialog
	 */
	JFileChooser fc;
	/**
	 * Name of the last loaded image
	 */
	String lastLoaded;

	/**
	 * This is a buffer to store last input commands (history)
	 */
	Vector commandBuf = new Vector(20);
	/**
	 * To move around command buffer
	 */
	int commandIndex = 0;

	/** A stack of images - for the commands push and pop */
	Stack imageStack;
	
	/** To format numbers into Strings */
	NumberFormat nf;

	/** When true, this component doesn't generate events to their listener (to stop pulling images -- loopdir) */
	private boolean passive = false;

	/**
	 * Image being processed
	 */
	PlanarImage currentImage;

	byte[][] colormap = null;
	int colormapBits = 8;
	Perceptron perceptron = null;
	ColorCategorization colorCat = null;
	ObjectImage objectImage = null;
	SOM nnsom = null;
	
	/**
	 * The component listening when we change images, etc.
	 */
	ChangeListener listener = null;

	ImageFileFilter filter = new ImageFileFilter();


	// Register "Wavelet" operator and its RIFs
	static {
		OperationRegistry registry =
				JAI.getDefaultInstance().getOperationRegistry();

		registry.registerDescriptor(new WaveletDescriptor());
		RenderedImageFactory waveletRIF = new WaveletRIF();
		RIFRegistry.register(registry, "Wavelet", "ccd-hyper", waveletRIF);

		registry.registerDescriptor(new IWaveletDescriptor());
		RenderedImageFactory iWaveletRIF = new IWaveletRIF();
		RIFRegistry.register(registry, "IWavelet", "ccd-hyper", iWaveletRIF);
		
		registry.registerDescriptor(new DerivativeDescriptor());
		RenderedImageFactory derivativeRIF = new DerivativeRIF();
		RIFRegistry.register(registry, "Derivative", "titech", derivativeRIF);

	}

	/**
	 * This class is used to capture keyboard events. It calls command interpreter
	 * whenever <b>ENTER</b> is typed. It uses a buffer to remember last commands,
	 * browsable using <b>UP</b> and <b>DOWN</b> keys.
	 *
	 * @author     david
	 * @created    2003/12/15
	 * @version    July 23, 2002
	 */
	class Accions extends KeyAdapter {
		/**
		 *  A key is typed.
		 *
		 * @param  e  Description of the Parameter
		 */
		public void keyTyped(KeyEvent e) {
			try {
				int code = e.getKeyChar();
				switch (code) {
								case KeyEvent.VK_ENTER:
									String s = command.getText();
									commandBuf.add(s);
									commandIndex = commandBuf.size();
									interpret(s);
									command.setText("");
									break;
								default:
				}
			} catch (Exception ex) {
				if (!passive) print(ex + "\n");
			}
		}


		/**
		 *  A key is pressed.
		 *
		 * @param  e  Description of the Parameter
		 */
		public void keyPressed(KeyEvent e) {
			try {
				int code = e.getKeyCode();
				switch (code) {
								case KeyEvent.VK_UP:
									if (commandIndex > 0) {
										commandIndex--;
									}
									command.setText((String) commandBuf.elementAt(commandIndex));
									break;
								case KeyEvent.VK_DOWN:
									commandIndex++;
									if (commandIndex >= commandBuf.size()) {
										commandIndex = commandBuf.size() - 1;
									}
									command.setText((String) commandBuf.elementAt(commandIndex));
									break;
								default:
				}
			} catch (Exception ex) {
				if (!passive) print(ex + "\n");
			}
		}

	}


	// end class Accions

	/**
	 *  Sets the image attribute of the IPPanel object
	 *
	 * @param  pimg  The new image value
	 */
	public void setImage(PlanarImage pimg) {
		currentImage = pimg;
	}


	/**
	 *  Gets the image attribute of the IPPanel object
	 *
	 * @return    The image value
	 */
	public PlanarImage getImage() {
		return currentImage;
	}


	/**
	 *Constructor for the IPPanel object
	 */
	public IPPanel() {
		super(new BorderLayout());

		log = new JTextArea(5, 20);
		command = new JTextField(20);
		fc = new JFileChooser(".");

		log.setMargin(new Insets(5, 5, 5, 5));
		log.setEditable(false);
		log.setLineWrap(true);
		log.setWrapStyleWord(true);
		log.setBackground(new Color(220, 200, 240));
		JScrollPane lpane = new JScrollPane(log);

		command.addKeyListener(new Accions());

		add(lpane, BorderLayout.CENTER);
		add(command, BorderLayout.SOUTH);

		nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(4);
		
		imageStack = new Stack();
		
		filter.addExtension("jpg");
		filter.addExtension("png");
		filter.addExtension("jpeg");
		filter.addExtension("bmp");
		filter.addExtension("tiff");
	}


	public static String commandList = "add, binarize, blur, canny, clear, cs, div, extrema, features, fwt, gblob, getblobs, getsample, gradient, gray, help, histohsv, invert, iwt, laplacian, load, loop2dirs, loopdir, maxcon, mean, modwhiteg, mult, palette, quanterror, quantize, quantizer, reload, rgb, segeval, sharpen, size, threshold, type, xydiagram, xyimage, ?";
	
	/**
	 * The command interpreter. Translates a String into actions.
	 * Those actions are detailed within the user documentation.
	 * <p>
	 * If the result of the operation is a visualizable image, it
	 * produces an event, if a listener is present.
	 * <p>
	 * Commands:<p>
	 <center>
	 <table border=1 align=center>
	 <tr>
	 	<th>Command</th><th>Arguments</th><th>Functionality</th></tr>
	 <tr>
	 	<th>add</th><td></td><td></td></tr>
	 <tr>
	 	<th>binarize</th><td></td><td></td></tr>
	 <tr>
	 	<th>blur</th><td></td><td></td></tr>
	 <tr>
	 	<th>canny</th><td></td><td></td></tr>
	 <tr>
	 	<th>clear</th><td></td><td>Clears the output window.</td></tr>
	 <tr>
	 	<th>cluster</th><td></td><td></td></tr>
		
	 <tr>
	 	<th>crop</th><td></td><td></td></tr>
	 <tr>
	 	<th>cropx</th><td></td><td></td></tr>		
	 <tr>
	 	<th>cs</th><td></td><td></td></tr>
	 <tr>
	 	<th>div</th><td></td><td></td></tr>
	 <tr>
	 	<th>extrema</th><td></td><td></td></tr>
	 <tr>
	 	<th>features</th><td></td><td></td></tr>
	 <tr>
	 	<th>fwt</th><td></td><td></td></tr>
	 <tr>
	 	<th>gblob</th><td></td><td></td></tr>
	 <tr>
	 	<th>getblobs</th><td></td><td></td></tr>
	 <tr>
	 	<th>getsample</th><td>x y band</td><td></td></tr>
	<tr>
	 	<th>gradient</th><td></td><td>The image is automatically converted to a float image with values between 0 and 1. To convert it again to byte, use:<br>
		<pre>
		mult 255
		type int
		</pre>
		</td></tr>
	 <tr>
	 	<th>gray/grey</th><td></td><td></td></tr>		
 	<tr>
	 	<th>histohsv</th><td></td><td></td></tr>
	 <tr>
	 	<th>invert</th><td></td><td><code>dst[x][y][b] = -src[x][y][b]</code></td></tr>
	 <tr>
	 	<th>iwt</th><td></td><td></td></tr>
	 <tr>
	 	<th>laplacian</th><td></td><td></td></tr>
		<tr>
	    <th>learn</th><td>(obj) [DIR]</td><td>Learns something from the images in DIR. 
		<ul><li><code>obj:</code> applies getblobs to get properties, and learns them in a SOM network.
		</ul>
		</td></tr>	 		
	 <tr>
	 	<th>load</th><td>[filename]</td><td>Loads an object into this panel. Possible objects are: colormaps (<code>.palette</code>), neural networks (<code>.xml</code>) and images (<code>jpeg, gif, tiff, bmp, ppm, ...</code>). If no argument is provided, a load requester is shown.</td></tr>
	 <tr>
	 <th>loop2dirs</th><td>[DIR1] [DIR2] [COMMAND1] [COMMAND2]</td><td>Executes COMMAND1 on every picture in directory DIR1 and then COMMAND2 on a picture of DIR2, sorted by filename.</td></tr>
 	 <tr>
	 	<th>loopdir</th><td>[DIR] [COMMAND]</td><td>Executes COMMAND on every picture in directory DIR.</td></tr>
	 <tr>
	 	<th>maxcon</th><td></td><td></td></tr>		
	<tr>
	 	<th>mean</th><td></td><td></td></tr>		
	 <tr>
	 	<th>modwhite</th><td></td><td></td></tr>
	 <tr>
	 	<th>modwhiteg</th><td></td><td></td></tr>
	 <tr>
	 	<th>mult</th><td></td><td></td></tr>
	 <tr>
	 	<th>objects</th><td>(map|index)<br>(an image in the stack)</td><td>Pops an image from the stack.</td></tr>
		
	 <tr>
	 	<th>palette</th><td>(partition|posterize|rainbow|indexed|show) [size]</td><td></td></tr>
	 <tr>
	 	<th>pop</th><td></td><td>Takes an image from the stack.</td></tr>
		
	 <tr>
	 	<th>posterize</th><td></td><td></td></tr>
	 <tr>
	 	<th>push</th><td></td><td>Puts current image into the image stack.</td></tr>

	<tr>
	 	<th>quanterror</th><td></td><td></td></tr>
	 <tr>
	 	<th>quantize</th><td>(map)</td><td>Outputs an indexed image. If "map" is specified, uses the euclidean distance to current palette. Otherwise, it uses a ColorCategorization object.</td></tr>
	 <tr>
	 	<th>quantizer</th><td>[threshold]</td><td>Builds a color quantizer object using a neural network, with the initial threshold. Before creating it, we need to load a netword using <b>load</b>. We also need to load a color map. For instance, <br>
	 	<pre>
	 	load universalEx.palette
		load colorsEx.xml
		quantizer
	 	</pre>
		</td></tr>
	 <tr>
	 	<th>reload</th><td></td><td></td></tr>
	 <tr>
	 	<th>rgb</th><td></td><td></td></tr>
	 <tr>
	 	<th>save</th><td>[fileName]</td><td>Saver current image to disk or overwrites last loaded one if no name is specified.</td></tr>
	 <tr>
	 	<th>savesamples</th><td>fileName</td><td>@see ColorManipulation.saveMaskedSamples</td></tr>
		
	<tr>
	 	<th>segeval</th><td><em>none</em></td><td>Evaluates the segmentation result of an instanciated <code>ObjectImage</code>. To instanciate one copy, you need a <code>ColorCategorization</code> object, obtained with the <b>quantizer</b> command, and then you can use <b>getblobs</b></td></tr>
	 <tr>
	 	<th>sharpen</th><td></td><td></td></tr>
	 <tr>
	 	<th>size</th><td>[WIDTH] [HEIGHT]</td><td></td></tr>
	 <tr>
	 	<th>sizex</th><td>[LONG] [SHORT]</td><td></td>
		</tr>
	 <tr>
	 	<th>threshold</th><td>[LOW] [HIGH] [CONSTANTS]</td><td></td>
		</tr>
	 <tr>
	 	<th>type</th><td></td><td></td></tr>
	 <tr>
	 	<th>xydiagram</th><td></td><td></td></tr>
	 <tr>
	 	<th>xyimage</th><td></td><td></td></tr>
	 <tr>
	 	<th>? / help</th><td></td><td>Displays some help.</td></tr>
	 </table>
	 </center>
	 * 
	 *
	 * @param  com  The command string.
	 */
	public void interpret(String com) {
		StringTokenizer stok = new StringTokenizer(com);
		if (!stok.hasMoreTokens()) {
			return;
		}
		String c = stok.nextToken();
		String token;
		try {
			if (c.equals("add")) {
		   		if (stok.hasMoreTokens()) {
			   		token = stok.nextToken();
			   		float v = Float.parseFloat(token);
			   		currentImage = COps.add(currentImage, v);
			   		generateEvent();
			   		if (!passive) print(hora() + "added constant "+v+"\n");
		   		} else {
			   		if (!passive) print(hora() + "add (const)\n");
		   		}
			} else if (c.equals("binarize")) {
			   if (stok.hasMoreTokens()) {
				   token = stok.nextToken();
				   double th = Double.parseDouble(token);
				   currentImage = COps.binarize(currentImage, th);
				   generateEvent();
				   if (!passive) print(hora() + "Image binarized (>"+ th + ")\n");
			   } else {
				   if (!passive) print(hora() + "binarize (const)\n");
			   }
		    } else if (c.equals("blur")) {			
				double sigma = 0.45;
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					sigma = Double.parseDouble(token);
				}
				int size = (int)(Math.ceil(sigma*3) * 2 + 1);
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					size = Integer.parseInt(token);
				}
				currentImage = COps.gaussianBlur(currentImage, size, sigma);
				generateEvent();
				if (!passive) print(hora() + "Blurred (sigma, kernel size)=("
						 + sigma + ", " + size + ")\n");
		    } else if (c.equals("canny")) {
			   currentImage = COps.cannyEdge(currentImage);
			   generateEvent();
			   if (!passive) print(hora() + "Canny Edge Operator applied.\n");
		    } else if (c.equals("clear")) {
			   log.setText("");
		    } else if (c.equals("cluster")) {
			   // classifies objects using a SOM
			   // load objects.som
			   // before doing so, do a load objects.palette
			   if (nnsom == null) {
				   print(hora()+" Create a SOM\n");
			   } else if (objectImage == null) {
				   print(hora() +" No object image! see getblobs.\n");
			   } else {
				   objectImage.regionClustering(nnsom);
				   currentImage = objectImage.getClusterImage(8, colormap);
				   generateEvent();
				   print(hora()+ " Clustered!\n");
			   }
			} else if (c.equals("crop")) {
				int x=0, y=0, width=64, height=64;
				if (stok.hasMoreTokens()) {
					x=Integer.parseInt(stok.nextToken());
					y=Integer.parseInt(stok.nextToken());
					width=Integer.parseInt(stok.nextToken());
					height=Integer.parseInt(stok.nextToken());
				}
				
				currentImage=COps.crop(currentImage,x,y,width,height);
				generateEvent();
				
				print("Image cropped ("+x+", "+y+", "+width+", "+height+")\n");
			} else if (c.equals("cropx")) {
				int x=0, y=0, width=64, height=64;
				if (stok.hasMoreTokens()) {
					x=Integer.parseInt(stok.nextToken());
					y=Integer.parseInt(stok.nextToken());
					width=Integer.parseInt(stok.nextToken());
					height=Integer.parseInt(stok.nextToken());
				}
				
				if (currentImage.getWidth()>currentImage.getHeight()) {
					interpret("crop "+x+" "+y+" "+" "+width+" "+height);
				} else {
					interpret("crop "+x+" "+y+" "+" "+height+" "+width);
				}
				
			} else if (c.equals("cs")) {
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					if (token.equals("hsv")) {
						currentImage = COps.colorConvert(currentImage, IHSColorSpace.getInstance());
						generateEvent();
						if (!passive) print(hora()+"Color space changed to IHS\n");
					} else if (token.equals("rgb")) {
						currentImage = COps.colorConvert(currentImage, ColorSpace.getInstance(ColorSpace.CS_sRGB));
						generateEvent();
						if (!passive) print(hora()+"Color space changed to RGB\n");
					}
				} else {
					if (!passive) print("Specify color space: (hsv|rgb) n");
				}
		    } else if (c.equals("colortr")) { // color transform
				PlanarImage filter = currentImage;
				interpret("pop");
				currentImage = ColorManipulation.changeColor(currentImage, filter);
				generateEvent();
				if (!passive) print(hora()+"Color transformed\n");
			} else if (c.equals("derivative")) {
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					if (token.equals("x")) {
						currentImage = Difference.differenciateX(currentImage);
						generateEvent();
						print(hora()+"Differenciated by X\n");
					} else if (token.equals("y")) {
						currentImage = Difference.differenciateY(currentImage);
						generateEvent();
						print(hora()+"Differenciated by Y\n");	
					} else {
						print("derivative (x|y)\n");
					}
				}
			} else if (c.equals("div")) {
			   if (stok.hasMoreTokens()) {
				   token = stok.nextToken();
				   float v = Float.parseFloat(token);
				   currentImage = COps.multiply(currentImage, 1f/v);
				   generateEvent();
				   if (!passive) print(hora() + "divided by "+v+"\n");
			   } else {
				   if (!passive) print(hora() + "div (const)\n");
			   }
			} else if (c.equals("exec")) {
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					execScript(new File(token));
				} else {
					scriptCallback();
				}				
			} else if (c.equals("extrema")) {
				double[][] ext = COps.extrema(currentImage);
				if (!passive) print(AMath.showMatrix(ext)+"\n");
			} else if (c.equals("features")) {
				if (objectImage != null) {
					double[][] feats = objectImage.getFeatures();
					int[] cats = objectImage.getCategories();
					
					int[][] fi = AMath.toInt(AMath.mult(feats,255));
					if (stok.hasMoreTokens()) {
						int i = Integer.parseInt(stok.nextToken());
						if (!passive) print(AMath.showVector(fi[i])+"\n");
					} else {
						if (!passive) print(AMath.showMatrix(fi)+"\n");
					}
					if (!passive) print("colors: "+AMath.showVector(cats)+"\n");
					int cb = objectImage.getCentralBlob();
					if (!passive) print("central blob: " + cb + " @ " 
						+ AMath.showVector(objectImage.getCoords(cb))	
						+ "\n");
					if (!passive) print("foveal order: " + 
						AMath.showVector(objectImage.fovealOrdering(cb))+"\n");
				} else {
					if (!passive) print(hora()+"No ObjectImage! Use [getblobs]\n");
				}

			} else if (c.equals("fwt")) {
				String alg = "haar";
				int level = 3;
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					if (token.equals("shore") || token.equals("haar")) {
						alg = token;
					}
				}
				if (stok.hasMoreTokens()) {
					level = Integer.parseInt(stok.nextToken());
				}
				
				fwtCallback(alg, level);

			} else if (c.equals("gblob")) { // gaussian blob
				int size = 21;
				double[] sigmas = new double[] {1, 1, 1};
				double[] mean = new double[] {255, 255, 255};
				if (stok.hasMoreTokens()) {
					size = Integer.parseInt(stok.nextToken());
				}
				if (stok.hasMoreTokens()) {
					sigmas = new double[] {
						Double.parseDouble(stok.nextToken()),
						Double.parseDouble(stok.nextToken()),
						Double.parseDouble(stok.nextToken())
					};
				}
				if (stok.hasMoreTokens()) {
					mean = new double[] {
						Double.parseDouble(stok.nextToken()),
						Double.parseDouble(stok.nextToken()),
						Double.parseDouble(stok.nextToken())
					};
				}				
				currentImage = TOps.gaussianBlob(size, sigmas, mean);
				generateEvent();
			} else if (c.equals("getblobs")) {
				if (colorCat != null) {
					double threshold=0.01;
					if (stok.hasMoreTokens()) {
						token = stok.nextToken();
						threshold = Double.parseDouble(token); 
					}
					
					objectImage = BlobOps.getBlobs(currentImage, colorCat, threshold);
					currentImage = ImageObjects.Indexed2RGB(
						objectImage.getLabeledImage(), 8,
						objectImage.getColormap());
					generateEvent();
					if (!passive) print(" "+objectImage+"\n");
				} else {
					if (!passive) print(hora()+"No quantizer selected.\n");
				}
			} else if (c.equals("getsample")) { // gets the value of a pixel
				if (stok.hasMoreTokens()) {
					int x = Integer.parseInt(stok.nextToken());
					int y = Integer.parseInt(stok.nextToken());
					int b = Integer.parseInt(stok.nextToken());
					
					RandomIter src = RandomIterFactory.create(currentImage, null);
					int color = src.getSample(x, y, b);
					
					print(""+color+"\n");
				} else {
					print(hora()+" getsample x y band \n");
				}
			} else if (c.equals("gradient")) {
				// magnitude and direction
				EdgeImage ei = new EdgeImage(currentImage, EdgeImage.GRADIENT);
				currentImage = ei.magnitude;
				String what="Magnitude";
				
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					if (token.equals("direction")) {
						currentImage = ei.direction;
						what="Direction";
					}
				}
				
				generateEvent();
				if (!passive) print(hora()+"Gradient "+what+"\n");
			} else if (c.equals("gray") || c.equals("grey")) {
				currentImage = COps.toBW(currentImage);
				generateEvent();
				if (!passive) print(hora()+"Color to Gray\n");
			} else if (c.equals("histohsv")) {
				ColorManipulation.getHSVColorHistogram(currentImage);
				if (!passive) print(hora()+"Computed HSV Color Histogram\n");
			} else if (c.equals("hsv")) {
				if (colormap != null) {
					currentImage = ImageObjects.Indexed2HSV(currentImage, colormapBits, colormap);
					generateEvent();
				}			
			} else if (c.equals("invert")) {
				currentImage = COps.invert(currentImage);
				generateEvent();
				if (!passive) print(hora()+"Colors Inverted\n");
			} else if (c.equals("iwt")) {
				String alg = "haar";
				int level = 3;
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					if (token.equals("shore") || token.equals("haar")) {
						alg = token;
					}
				}
				if (stok.hasMoreTokens()) {
					level = Integer.parseInt(stok.nextToken());
				}
				
				iwtCallback(alg, level);
			} else if (c.equals("laplacian")) {
				double sigma = 0.45;
				
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					sigma = Double.parseDouble(token);
				}
				int size = (int)(Math.ceil(sigma*3) * 2 + 1);
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					size = Integer.parseInt(token);
				}
				currentImage = COps.laplacianGaussian(currentImage, size, sigma);
				generateEvent();
				if (!passive) print(hora() + "LoG (sigma, kernel size)=("
						 + sigma + ", " + size + ")\n");
				
			} else if (c.equals("learn")) { // this executes a batch of commands on every picture in the dir and learns something from them
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					
					if (token.equals("obj")) {
					
					if (stok.hasMoreTokens()) {
						String currentPath = stok.nextToken();
						File sf = new File(currentPath);
						File[] fileList = sf.listFiles(filter);
						// sort the list
						Vector fileVector = new Vector(fileList.length);
						for (int i = 0;i<fileList.length;i++) fileVector.add(fileList[i]);
						Collections.sort(fileVector);
						for (int i = 0;i<fileList.length;i++) fileList[i]=(File)fileVector.get(i);
						
						// create a matrix of samples
						int nsamples=fileList.length;
						int nfeatures=ObjectImage.NFEATURES;
						double[][] x=null,aux=null;


						long totalTime = 0;
						int nx=0;
						for (int i = 0; i < fileList.length; i++) {
							File f = fileList[i];
							passive = true;
							interpret("load "+f.getAbsolutePath());
							long time = System.currentTimeMillis();
							//print(f.getName() + ":\t");
							interpret("getblobs");
							double[][] feats = objectImage.getFeatures();
							//nnsom.learn(feats, 0.8, 0.00001);
							
							if (i==0) {
								x = feats;
								aux = x;
							} else {
								//growing vector
								aux=new double[x.length+feats.length][nfeatures];
								for (int nb=0;nb<x.length;nb++)
									for (int nf=0;nf<nfeatures;nf++)
										aux[nb][nf]=x[nb][nf];
							
								for (int nb=0;nb<feats.length;nb++)
									for (int nf=0;nf<nfeatures;nf++)
										aux[x.length+nb][nf]=feats[nb][nf];
							
								x=aux;
							}
							
							totalTime += System.currentTimeMillis() - time;
						}
						passive = false;
						nnsom.iniRandomSamples(x);
						interpret("som"); //output values
						nnsom.learn(x, 0.8, 0.00001);
						
						double t = (double) totalTime / 1000.0;
						if (!passive) print(hora()+"Learned "+fileList.length
							+" images in "+nf.format(t)+" secs.\n");
						
						// input the samples to the network
						
						//nnsom.learn(x, 0.8, 0.00001);
					} else if (token.equals("img")) {
						// learn image categories
						
						// getblobs, cluster and getclusterhistogram to learn
						
					}
					
					} else {
						if (!passive) print("learn (obj) DIR \n");						
					}
					
				} else {
					if (!passive) print("loopdir DIR ACTION_COMMAND \n Please, specify directory.\n");
				}
				
			} else if (c.equals("load")) {
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					load(token);
				} else {
					openCallback();
				}
			} else if (c.equals("loopdir")) { // this executes a batch of commands on every picture in the dir and measures time
				if (stok.hasMoreTokens()) {
					String currentPath = stok.nextToken();
								
					if (stok.hasMoreTokens()) {
						String command = stok.nextToken("\t"); // hopefully, the rest of the String
						File sf = new File(currentPath);
						File[] fileList = sf.listFiles(filter);
						// sort the list
						Vector fileVector = new Vector(fileList.length);
						for (int i = 0;i<fileList.length;i++) fileVector.add(fileList[i]);
						Collections.sort(fileVector);
						for (int i = 0;i<fileList.length;i++) fileList[i]=(File)fileVector.get(i);

						
						long totalTime = 0;
						for (int i = 0; i < fileList.length; i++) {
							File f = fileList[i];
							passive = true;
							interpret("load "+f.getAbsolutePath());
							long time = System.currentTimeMillis();
							print(f.getName() + ":\t");
							interpret(command);
							totalTime += System.currentTimeMillis() - time;
						}
						passive = false;
						double t = (double) totalTime / 1000.0;
						if (!passive) print(hora()+"Computed "+command+" for "+fileList.length
							+" images in "+nf.format(t)+" secs.\n");
						
					} else {
						if (!passive) print("loopdir DIR ACTION_COMMAND \n Please, specify Command.\n");						
					}
					
				} else {
					if (!passive) print("loopdir DIR ACTION_COMMAND \n Please, specify directory.\n");
				}
			} else if (c.equals("loop2dirs")) { // this executes a batch of commands on every picture in the dir and measures time
				if (stok.hasMoreTokens()) {
					String dirA = stok.nextToken();
					String dirB = stok.nextToken();
					
					if (stok.hasMoreTokens()) {
						String command = stok.nextToken("\t");
						StringTokenizer stcomms = new StringTokenizer(command,",");
						String commandA = stcomms.nextToken();
						String commandB = stcomms.nextToken();
						
						File sf = new File(dirA);
						File[] fileListA = sf.listFiles(filter);
						// sort the list
						Vector fileVector = new Vector(fileListA.length);
						for (int i = 0;i<fileListA.length;i++) fileVector.add(fileListA[i]);
						Collections.sort(fileVector);
						for (int i = 0;i<fileListA.length;i++) fileListA[i]=(File)fileVector.get(i);

						sf = new File(dirB);
						File[] fileListB = sf.listFiles(filter);
						// sort the list
						fileVector = new Vector(fileListB.length);
						for (int i = 0;i<fileListB.length;i++) fileVector.add(fileListB[i]);
						Collections.sort(fileVector);
						for (int i = 0;i<fileListB.length;i++) fileListB[i]=(File)fileVector.get(i);
						
						long totalTime = 0;
						for (int i = 0; i < fileListA.length; i++) {
							File fA = fileListA[i];
							File fB = fileListB[i];

							passive = true;
							long time = System.currentTimeMillis();
							print(fA.getName() + " - "+fB.getName()+":\t");

							interpret("load "+fA.getAbsolutePath());							
							interpret(commandA);
							interpret("load "+fB.getAbsolutePath());
							interpret(commandB);
							
							totalTime += System.currentTimeMillis() - time;
						}
						passive = false;
						double t = (double) totalTime / 1000.0;
						if (!passive) print(hora()+"Computed "+commandA+" and "+commandB+" for 2x"+fileListA.length
							+" images in "+	nf.format(t)+" secs.\n");
						
					} else {
						if (!passive) print("loop2dirs DIR-1 DIR-2 \\t ACTION_COMMAND-1 \\t ACTION_COMMAND-2\n Please, specify Command.\n");						
					}
					
				} else {
					if (!passive) print("loop2dir DIR-1 DIR-2 \\t ACTION_COMMAND-1 \\t ACTION_COMMAND-2\n Please, specify directory.\n");
				}
			} else if (c.equals("maxcon")) {
				currentImage = COps.maxcon(currentImage);
				generateEvent();
				if (!passive) print(hora()+"Contrast linearly maximized.\n");
			} else if (c.equals("mean")) {
				double[] means = COps.mean(currentImage);
				print(AMath.showVector(means)+"\n");				
			} else if (c.equals("modwhite")) {
				currentImage = ColorManipulation.modWhite(currentImage);
				generateEvent();
				if (!passive) print(hora()+"Color corrected.\n");				
			} else if (c.equals("modwhiteg")) {
				currentImage = ColorManipulation.modWhiteInGray(currentImage);
				generateEvent();
				if (!passive) print(hora()+"Color corrected.\n");
			} else if (c.equals("mult")) {
			   if (stok.hasMoreTokens()) {
				   token = stok.nextToken();
				   float v = Float.parseFloat(token);
				   currentImage = COps.multiply(currentImage, v);
				   generateEvent();
				   if (!passive) print(hora() + "multiplied by "+v+"\n");
			   } else {
				   if (!passive) print(hora() + "mult (const)\n");
			   }
			} else if (c.equals("objects")) {
				PlanarImage indexed=null;
				double threshold = 0.01;
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					if (token.equals("map") && colormap!=null) { // euclidean distance to colormap
						if (stok.hasMoreTokens()) {
							token = stok.nextToken();
							threshold = Double.parseDouble(token); 
						}						
						colormapBits = 8;
						indexed = ImageObjects.RGB2Indexed(currentImage, colormapBits, colormap);
					} else if (token.equals("index")) { // currentImage is already indexed
						if (stok.hasMoreTokens()) {
							token = stok.nextToken();
							threshold = Double.parseDouble(token); 
						}
						indexed = currentImage;
					} else {
						threshold = Double.parseDouble(token);
					}
				
				} else if (colorCat != null) {
					indexed = colorCat.categorize(currentImage);
				}
				if (indexed!=null) {
					if (imageStack.empty()) {
						objectImage = new ObjectImage(indexed, null, threshold);
						currentImage = objectImage.getLabeledImage();
					} else {
						// recovers the original (not blurred) image from the stack
						interpret("pop"); 
						objectImage = new ObjectImage(indexed, currentImage, threshold);
					
						currentImage = ImageObjects.Indexed2RGB(
							objectImage.getLabeledImage(), 8,
							objectImage.getColormap());
					}
					generateEvent();
					print(hora() + " "+objectImage+"\n");
				} else {
					print("There is no quantizer!\n");
				}
			} else if (c.equals("palette")) {
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					if (token.equals("partition")) {
						//colormap = ImageObjects.paletteHSVPartition(18, 2, 2, 4);
						int[] b = new int[] {6, 6, 5};
						int k=0;
						while (stok.hasMoreTokens() && k<3) b[k++]=Integer.parseInt(stok.nextToken());
						 
						colormap = ImageObjects.paletteRGBPartition(b[0], b[1], b[2]);
						//if (!passive) print(AMath.showMatrix(colormap)+"\n");
					} else if (token.equals("posterize")) {
						int[] b = new int[] {6, 6, 6};
						int k=0;
						while (stok.hasMoreTokens() && k<3) b[k++]=Integer.parseInt(stok.nextToken());
						 
						colormap = ImageObjects.palettePosterizeShrink(b);
						//if (!passive) print(AMath.showMatrix(colormap)+"\n");
						
					} else if (token.equals("rainbow")) {
						int ncols = 32;
						if (stok.hasMoreTokens()) ncols = Integer.parseInt(stok.nextToken());
						colormap = ImageObjects.paletteRainbow(ncols);
						
					} else if (token.equals("gray")) {
						int ncols = 32;
						if (stok.hasMoreTokens()) ncols = Integer.parseInt(stok.nextToken());
						colormap = ImageObjects.paletteGray(ncols);
						
					} else if (token.equals("indexed")) {
						// tries getting the palette from currentImage (assumes indexed)
						colormap = ImageObjects.paletteFromImage(currentImage);
					} else if (token.equals("add")) { // add bins to current palette
						int nbins = 1;
						if (stok.hasMoreTokens()) nbins = Integer.parseInt(stok.nextToken());
						
						colormap = ImageObjects.addBins(colormap, nbins);
					} else { // if (token.equals("show"))
						print(AMath.showMatrix(colormap) + "\n");
					}
					if (!passive) print(hora()+" You got a palette of "+colormap[0].length+"bins.\n");
				} else {
					if (!passive) print("Specify type: (partition) (size)\n");
				}
			} else if (c.equals("pop")) {
				currentImage = (PlanarImage) imageStack.pop();
				generateEvent();
				if (!passive) print("Image Popped\n");
			} else if (c.equals("posterize")) {
				int[] levels = new int[] {6, 6, 6};
				
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					levels[0] = Integer.parseInt(token);
					if (stok.hasMoreTokens()) {
						levels[1] = Integer.parseInt(stok.nextToken());
						levels[2] = Integer.parseInt(stok.nextToken());
					} else {
						levels[2] = levels[1] = levels[0];
					}
				}
				currentImage = ColorManipulation.posterize(currentImage, levels);
				generateEvent();
				if (!passive) print(hora() + "posterized - "+AMath.showVector(levels)+" levels.\n");
			} else if (c.equals("push")) {
				imageStack.push(currentImage);
				if (!passive) print("Image pushed\n");
			} else if (c.equals("quanterror")) {
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					if (token.equals("obj") && objectImage !=null) {
						currentImage = objectImage.getError();
						generateEvent();
						if (!passive) print(hora()+"Obj Quantization Error.\n");
					}
				} else if (colorCat != null) {
					if (colorCat.getError()==null) colorCat.categorize(currentImage);
					currentImage=colorCat.getError();
					generateEvent();
					if (!passive) print(hora()+"Quantization Error.\n");
				} else {
					if (!passive) print("No quantizer selected.\n");
				}
			} else if (c.equals("quantize")) {
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					if (token.equals("map") && colormap!=null) { // euclidean distance to colormap
						colormapBits = 8;
						currentImage = ImageObjects.RGB2Indexed(currentImage, colormapBits, colormap);
						generateEvent();
					}
				} else if (colorCat != null) {
					currentImage = colorCat.categorize(currentImage);
					generateEvent();
					if (!passive) print(hora()+"Color Quantized.\n");
				} else {
					if (!passive) print(hora()+"No quantizer selected.\n");
				}
			} else if (c.equals("quantizer")) {
				double threshold = BlobOps.THRESHOLD;
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					threshold = Double.parseDouble(token);
				}
				if (colormap != null && perceptron != null) {
					perceptron.setThreshold(threshold);
					colormapBits = 5;
					colorCat = new ColorCategorization(perceptron,colormapBits,colormap);
					if (!passive) print(hora()+"New quantizer. Threshold = "+ nf.format(threshold) +"\n");
				}
			} else if (c.equals("reload")) {
				reloadCallback();
				generateEvent();
			} else if (c.equals("rgb")) {
				if (colormap != null) {
					byte[][] map = colormap;
					if (stok.hasMoreTokens() && objectImage != null) {
						if (stok.nextToken().equals("foveal")) {
							// reorder palette using objectImage.fovealOrdering
							int cb = objectImage.getCentralBlob();
							if (stok.hasMoreTokens()) {
								cb = Integer.parseInt(stok.nextToken());
							}
							int[] order = objectImage.fovealOrdering(cb);
							map = ImageObjects.reorderMap(colormap, order);
						}
					}
					currentImage = ImageObjects.Indexed2RGB(currentImage, colormapBits, map);
					generateEvent();
					if (!passive) print(hora()+"Indexed Image transformed to RGB\n");
				}
			} else if (c.equals("save")) {
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					save(token);
				} else { // overwrites lastLoaded file
					save(lastLoaded);
				}
			} else if (c.equals("savesamples")) {
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					
					BufferedWriter bwsamples = new BufferedWriter(
						new OutputStreamWriter(
						new FileOutputStream(token,true))); //append

					PlanarImage mask = currentImage;
					currentImage = (PlanarImage) imageStack.pop();
					
					ColorManipulation.saveMaskedSamples(currentImage, mask, bwsamples);
					bwsamples.close();
					
					if (!passive) print(hora()+ "Samples saved ["+token+"].\n"); 

				} else { 
					if (!passive) print("savesamples filename\n");
				}				
			} else if (c.equals("segeval")) { // Evaluation of Color Segmentation
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					if (token.equals("map")) {
						// even in passive mode, outputs the result
						print("F(I) = " + objectImage.evaluationF(objectImage.getError(), 0) + "\n");

					} else if (token.equals("regions")) { //number of regions
						print("#reg = " + objectImage.getNRegions() +"\n");
					}
				} else if (colorCat!=null && objectImage!=null) {
					// even in passive mode, outputs the result
					print("F(I) = " + objectImage.evaluationF(colorCat.getError(), 
						colorCat.getNN().getThreshold())+"\n");
				} else {
					if (!passive) print(hora()+"No segmentation performed. See 'getblobs'\n");
				}
			} else if (c.equals("sharpen")) {
				currentImage = COps.sharpen(currentImage);
				generateEvent();
				if (!passive) print(hora() + "sharpened\n");
			} else if (c.equals("size")) {
				int bands = currentImage.getSampleModel().getNumBands();				
				int width = currentImage.getWidth();
				int height = currentImage.getHeight();
				int tileWidth = currentImage.getTileWidth();
				int tileHeight = currentImage.getTileHeight();

				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					int x = Integer.parseInt(token);
					if (stok.hasMoreTokens()) {
						int y = Integer.parseInt(stok.nextToken());
						if (stok.hasMoreTokens()) {
							if (stok.nextToken().equals("linear")) {
								currentImage = COps.scaleN(currentImage,
								(float) x / (float) width,
								(float) y / (float) height);
							} else {
								if (!passive) print(hora()+"interpolation? (linear)\n");
							}
						} else {
							currentImage = COps.scale(currentImage,
								(float) x / (float) width,
								(float) y / (float) height, 0, 0);
						}
					} else {
						currentImage = COps.scale(currentImage,
								(float) x / (float) width,
								(float) x / (float) width, 0, 0);
					}
					generateEvent();
				} else {
					if (!passive) print(hora() + "size: " + width + "x" + height +
							"x" + bands + " - tile: " + 
							tileWidth + "x" + tileHeight + "\n");
				}
			} else if (c.equals("sizex")) {
				int imglong = BlobOps.IMGLONG, imgshort=BlobOps.IMGSHORT;
				int resultWidth = BlobOps.IMGLONG, resultHeight=BlobOps.IMGLONG;
				String algo="";
				
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					imglong = Integer.parseInt(token);
					if (stok.hasMoreTokens()) {
						imgshort = Integer.parseInt(stok.nextToken());
						if (stok.hasMoreTokens()) {
							algo = " "+stok.nextToken();
						}
					}
				}
		
				if (currentImage.getWidth()>currentImage.getHeight()) {
					resultWidth = imglong; resultHeight = imgshort;
				} else {
					resultWidth = imgshort; resultHeight = imglong;
				}
				
				interpret("size "+resultWidth+" "+resultHeight+algo);
			} else if (c.equals("som")) {
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					int noutputs = Integer.parseInt(token);
					int ninputs = ObjectImage.NFEATURES;
					if (stok.hasMoreTokens()) 
						ninputs = Integer.parseInt(stok.nextToken());
					
					nnsom = new SOM(ninputs,noutputs);
					nnsom.iniRandom();
					
					if (!passive) print(hora()+"new "+nnsom+"\n");
					if (!passive) print(AMath.showMatrix(nnsom.getWeights()));
				} else {
					if (!passive) print(hora()+nnsom+"\n");
					if (!passive && nnsom!=null ) print(AMath.showMatrix(nnsom.getWeights()));
				}
			} else if (c.equals("ssr") || c.equals("ssc")) {
				int s = 20;
				int row = 0;
				double step = 1.;
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					s = Integer.parseInt(token);
					if (stok.hasMoreTokens()) {
						token = stok.nextToken();
						row = Integer.parseInt(token);
					}
					if (stok.hasMoreTokens()) {
						token = stok.nextToken();
						step = Double.parseDouble(token);
					}
					if (c.equals("ssr")) 
					    currentImage = TOps.buildSSrow(currentImage, s, row, step);
					else
					    currentImage = TOps.buildSScol(currentImage, s, row);
					
					generateEvent();
					if (!passive) print("Scale Space of " + s + " scales\n");
				} else {
					if (!passive) print("SS <scales> [row] \n");
				}
				
			} else if (c.equals("threshold")) {
			   double low = 1.,high=255.,out=255.;
			   
			   if (stok.hasMoreTokens()) {
				   token = stok.nextToken();
				   low = Double.parseDouble(token);
			   }
			   if (stok.hasMoreTokens()) {
				   token = stok.nextToken();
				   high = Double.parseDouble(token);
			   }
			   if (stok.hasMoreTokens()) {
				   token = stok.nextToken();
				   out = Double.parseDouble(token);
			   }
			   
			   currentImage = COps.threshold(currentImage, low, high, out);
			   generateEvent();
			   
			   if (!passive) print(hora() + "Image thresholded: "+ low + " <= img <= "+high+"\n");
				
			} else if (c.equals("type")) {
				if (stok.hasMoreTokens()) {
					token = stok.nextToken();
					int t = DataBuffer.TYPE_FLOAT;
					if (token.equals("int")) t = DataBuffer.TYPE_INT;
					else if (token.equals("byte")) t = DataBuffer.TYPE_BYTE;
					else if (token.equals("float")) t = DataBuffer.TYPE_FLOAT;					
					currentImage = COps.reformat(currentImage, t);
					generateEvent();
					if (!passive) print(hora()+"Type changed to "+token+"\n");
				} else {
					if (!passive) print(hora()+"Type: "+COps.getDataTypeName(
						currentImage.getSampleModel().getDataType())+"\n");
				}
			} else if (c.equals("xydiagram")) {
				int size = 512;
				double luz = 0.1;
				if (stok.hasMoreTokens()) {
					size = Integer.parseInt(stok.nextToken());
				}
				if (stok.hasMoreTokens()) {
					luz = Double.parseDouble(stok.nextToken());
				}
				currentImage = ImageObjects.createXYDiagram(size, (float)luz);
				generateEvent();
				if (!passive) print(hora()+" XY diagram created\n");

			} else if (c.equals("xyimage")) {
				int size = 512;
				if (stok.hasMoreTokens()) {
					size = Integer.parseInt(stok.nextToken());
				}
				currentImage = ImageObjects.createXYDiagram(size, currentImage);
				generateEvent();
				if (!passive) print(hora()+" XY diagram created\n");
				
			} else if (c.equals("?")||c.equals("help")) {
				if (!passive) print("Available commands: "+commandList);
			} // other commands are ignored (# for comments)						
		} catch (Exception e) {
			if (!passive) print("interpret: " + e + "\n");
			// just in case it crashed in loopdir:
			passive = false;
		}
	}

	// end interpret




	// --------------------------------------- COMMANDS ------------------------------

	/**
	 * Opens a file-load requester to load an image
	 */
	void openCallback() {
		try {
			int returnVal = fc.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				String path = (fc.getSelectedFile()).getAbsolutePath();

				load(path);
			} else {
				if (!passive) print(hora() + "Open command cancelled by user.\n");
			}
		} catch (Exception e) {
			if (!passive) print("load: " + e + "\n");
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  file  Description of the Parameter
	 */
	void load(String file) throws 
		java.io.IOException,
		javax.xml.parsers.ParserConfigurationException,
		org.xml.sax.SAXException {
		String ext = getExtension(file);
		if (ext.equals(".palette")) {
			FileInputStream sPalette = new FileInputStream(file);
			colormap = ImageObjects.loadPalette(sPalette);
		} else if (ext.equals(".xml")) {
			perceptron = new Perceptron();
			perceptron.load(new FileInputStream(file));
			//nn.setThreshold(0.6);
			//colorCat = new ColorCategorization(nn,5,colormap);
		} else if (ext.equals(".som")) {
			nnsom = new SOM(new FileReader(file));
		} else {
			if (file.substring(0,4).equals("http")) {
				currentImage = JAI.create("url", new java.net.URL(file));
			} else {
				currentImage = JAI.create("fileload", file);
			}
			lastLoaded = file;
			generateEvent();
		}

		if (!passive) print(hora() + "Loaded \"" + file + "\"\n");

	}

	void save(String fileName) throws java.io.IOException {
		String ext = getExtension(fileName);
				
		if (ext.equals(".jpg") || ext.equals(".jpeg")) {
			COps.saveAsJPG(currentImage, fileName, 1f);
		} else if (ext.equals(".png")) {
			if (indexed()) {
				COps.saveAsPNG(currentImage, AMath.vflattenInt(colormap), fileName);
			} else {
				COps.saveAsPNG(currentImage, fileName);
			}
		} else {
			print("Unknown Extension! ("+ext+")");
		}
		
		print(hora()+"Saved \"" + fileName + "\"");
		
	}

	/**
	 * Executes a script file
	 *
	 * @param  f             the file name of the script
	 * @throws  IOException
	 */
	void execScript(File f) throws IOException {
		String script = f.getAbsolutePath();
		BufferedReader br = new BufferedReader(new FileReader(f));
		if (!passive) print(hora() + "[Begin \"" + script + "\"]\n");

		String comm;
		do {
			comm = br.readLine();
			if (comm != null) {
				interpret(comm);
			}
		} while (comm != null);

		if (!passive) print(hora() + "[End \"" + script + "\"]\n");
	}


	/**
	 * Opens a file requester to select a script file to execute
	 */
	void scriptCallback() {
		try {
			int returnVal = fc.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				execScript(fc.getSelectedFile());
			} else {
				if (!passive) print(hora() + "Execution cancelled by user.\n");
			}
		} catch (Exception e) {
			if (!passive) print("script: " + e + "\n");
		}
	}
	// end scriptCallback




	/**
	 * Reloads last loaded image
	 */
	void reloadCallback() {
		try {
			load(lastLoaded);
			//currentImage = JAI.create("fileload", lastLoaded);

			//if (!passive) print(hora() + "Loaded \"" + lastLoaded + "\"\n");
		} catch (Exception e) {
			if (!passive) print(e + "\n");
		}

	}



	/**
	 * Checks whether the image in the Image Panel is multiple of 2^level
	 *
	 * @param  operation  a String representing the operation which wants to
	 *        check this condition
	 * @return            Description of the Return Value
	 */
	boolean canBeDecomposed(String operation, int level) {
		int factor = 1 << level;
		int width = currentImage.getTileWidth();
		int height = currentImage.getTileHeight();
		if ((width % factor != 0) ||
				(height % factor != 0)) {
			if (!passive) print("Operation \"" + operation + "\" can not be applied to " +
					width + "x" + height + " tiles being level " + level + "\n");
			return false;
		}
		return true;
	}


	/**
	 * Called to do <b>Forward Wavelet Transform</b> on the current image.
	 *
	 * @param  algorythm  the algorythm to use (haar, shore, ...)
	 */
	void fwtCallback(String algorythm, int level) {

		//if (!isSquare("Wavelet")) return;
		if (!canBeDecomposed("Wavelet",level)) {
			return;
		}

		try {

			//runGc();
			RenderedOp rop =
					COps.wavelet(currentImage, algorythm, level);
			//currentImage = rop.createInstance();
			currentImage = rop;
			generateEvent();

			if (!passive) print(hora() + "Forward " + algorythm + " DWT\n");
		} catch (Exception e) {
			if (!passive) print("FWT: " + e + "\n");
		}
	}


	/**
	 * Called to do <b>Inverse Wavelet Transform</b> on the CURRENT image.
	 *
	 * @param  algorythm  the algorythm to use (haar, shore, ...)
	 */
	void iwtCallback(String algorythm, int level) {
		//if (!isSquare("IWavelet")) return;
		if (!canBeDecomposed("IWavelet",level)) {
			return;
		}

		try {
			RenderedOp rop =
					COps.iwavelet(currentImage, algorythm, level);
			currentImage = rop.createInstance();
			generateEvent();

			if (!passive) print(hora() + "Inverse DWT\n");
		} catch (Exception e) {
			if (!passive) print(hora() + e);
		}
	}



	// ----------------------------------- COMMODITIES ------------------------------


	/**
	 * Returns a string with system's time.
	 *
	 * @return    System's Time.
	 */
	public final static String hora() {
		Calendar c = new GregorianCalendar();
		int min = c.get(Calendar.MINUTE);
		return ("[" + c.get(Calendar.HOUR_OF_DAY) + ":" + ((min < 10) ? "0" + min : "" + min) + "] ");
	}


	/**
	 *  Gets the extension of a file, that is, just the substring after the last '.',
	 *  in lower case. It also returns the colon!
	 *
	 * @param  file  Description of the Parameter
	 * @return       The extension value
	 */
	public final static String getExtension(String file) {
		int pos = file.lastIndexOf('.');
		return file.substring(pos).toLowerCase();
	}

	/** Tells you if currentImage is indexed type */
	public boolean indexed() {
		int bands = currentImage.getSampleModel().getNumBands();				
		if (bands == 1 && colormap!=null) return true;
		return false;
	}
	
	/** Appends some text in the log output, and scrolls down the text */
	public void print(String text) {
		log.append(text);
		log.setCaretPosition(log.getDocument().getLength());
	}

	// --------------------------------------- EVENTS ------------------------------

	/**
	 *  Adds a feature to the ChangeListener attribute of the IPPanel object
	 *
	 * @param  listener  The feature to be added to the ChangeListener attribute
	 */
	public void addChangeListener(ChangeListener listener) {
		this.listener = listener;
	}
	
	
	/**
	 *  Description of the Method
	 */
	private void generateEvent() {
		if (listener != null && !passive) {
			ChangeEvent che = new ChangeEvent(this);
			listener.stateChanged(che);
		}
	}	
}


