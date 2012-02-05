package titech.image.blob;

import java.io.*;
import javax.media.jai.*;

import java.text.NumberFormat;

import titech.image.dsp.*;
import titech.file.*;
import titech.image.math.AMath;

/**
 *  Builds a table comparing the blobs of all the images in a directory.
 *
 * You may need to set up the proxy depending on your config.
 * java -classpath dist/ICPainter.jar -Dhttp.proxyHost=proxyhost [-Dhttp.proxyPort=portNumber] titech.image.blob.GenerateComparisons DIR >output.html
 * @author     Owner
 * @created    2003/10/30
 */
public class GenerateComparisons {
	final static ImageFileFilter filter = new ImageFileFilter();

	static ColorCategorization colorCatT = null;

	/**
	 * @param  args 
	 */
	public final static void main(String[] args) {

		if (args.length < 1) {
			System.out.println("java titech.image.blob.GenerateComparisons IMAGE_DIR");
			System.exit(0);
		}

		try {
			titech.nn.Perceptron nnT = new titech.nn.Perceptron();

			InputStream sPalette = new FileInputStream("universalEx.palette");
			byte[][] colormap = ImageObjects.loadPalette(sPalette);
			
			FileInputStream pFile = new FileInputStream("colorsEx.xml");			
			nnT.load(pFile);
			nnT.setThreshold(0.6);
			colorCatT = new ColorCategorization(nnT,5,colormap);

			filter.addExtension("jpg");
			filter.addExtension("png");
			String currentPath = args[0];
			
			File sf = new File(currentPath);
			File[] fileList = sf.listFiles(filter);
			
			System.out.println("<html><body>");
			System.out.println("<table border=1>");
			
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(4);

			System.out.println("<tr><td></td>");			
			for (int i = 0; i < fileList.length; i++) {
				File f = fileList[i];
				File bf = new File(f.getParent()+"/res/"+i+".png");
				System.out.println("<td align=center><img width=60 src=\""+f.toURL()+"\">");
				System.out.println("<img src=\""+bf.toURL()+"\"></td>");
			}
			System.out.println("</tr>");

			for (int i = 0; i < fileList.length; i++) {
				File f = fileList[i];			
				PlanarImage pimg = JAI.create("fileload", f.getAbsolutePath());
				ObjectImage obi = BlobOps.getBlobs(pimg, colorCatT);
				double[][] features =  obi.getFeatures();
				//RenderedOp res = ImageObjects.Indexed2RGB(obi.getLabeledImage(), 8, obi.getColormap());
				String bfile = f.getParent()+"/res/"+i+".png";
				COps.saveAsPNG(obi.getLabeledImage(), AMath.vflattenInt(obi.getColormap()), bfile);
				File bf = new File(bfile);
								
				
				System.out.println("<tr>");
				System.out.println("<td><img width=60 src=\""+f.toURL()+"\">");
				System.out.println("<img src=\""+bf.toURL()+"\"></td>");
				String fet="";
				for (int x=0;x<features.length;x++) {
					fet += "<b>("+x+")</b>: "+nf.format(features[x][0]);
					for (int y=1;y<features[0].length;y++) {
						fet+=", "+nf.format(features[x][y]);
					}
					fet+="<br>";
				}
						
				//System.out.println("<td>"+fet+"</td>");
				
				for (int j=0;j<fileList.length;j++) {
					File fc = fileList[j];
					PlanarImage pimgc = JAI.create("fileload", fc.getAbsolutePath());
					ObjectImage obic = BlobOps.getBlobs(pimgc, colorCatT);
					
					TiledImage comparison = obi.compareImages(obic);
					String cfile = f.getParent()+"/res/"+i+"-"+j+".png";
					int zoomed = comparison.getWidth() * 8;
					//RenderedOp res = COps.scale(comparison,8,8,0,0);
					COps.saveAsPNG(comparison, cfile);
					File cf = new File(cfile);
					
					double[][] featc = obic.getFeatures();
					
					//System.err.println(AMath.showMatrix(featc));
					System.out.println("<td align=center valign=top>");
					System.out.println("<img width="+zoomed+" src=\""+cf.toURL()+"\">");
					double maxim = 0, maxPos=0, maxVol=0, maxCol=0;
					int maxInd = 0, mIndPos=0, mIndVol=0, mIndCol=0;
					for (int x=0;x<features.length;x++) {
						double minim = 100, minPos=1, minVol=1, minCol=1;
						int minInd = 0, indPos=0, indVol=0, indCol=0;
						for (int xc=0;xc<featc.length;xc++) {
							//System.out.println("<b>("+x+", "+xc+")</b>:");
							double sum=0;
							double position = (features[x][0]-featc[xc][0])*
								(features[x][0]-featc[xc][0]) +
								(features[x][1]-featc[xc][1])*
								(features[x][1]-featc[xc][1]);
							position = Math.sqrt(position/2.0);
							double volume = (features[x][2]-featc[xc][2])*
								(features[x][2]-featc[xc][2]) +
								(features[x][3]-featc[xc][3])*
								(features[x][3]-featc[xc][3]);
							volume = Math.sqrt(volume/2.0);
							double color = 0;
							for (int fe = 4; fe<13; fe++) {
								color += (features[x][fe]-featc[xc][fe])*
								(features[x][fe]-featc[xc][fe]);
							}
							color = Math.sqrt(color/9.0);
							
							if (position<minPos) { minPos=position; indPos=xc; }
							if (volume<minVol) { minVol=volume; indVol=xc; }
							if (color<minCol) { minCol=color; indCol=xc; }
							
							/*
							for (int y=1;y<features[0].length;y++) {
								double ff=features[x][y]-featc[xc][y];
								ff*=ff;
								sum+=ff;
							}
							sum = Math.sqrt(sum);
							*/
							sum = volume + color + position;  // weights = 1,1,1
							if (sum<minim) { 
								minim = sum;
								minInd = xc;
							}
							//System.out.println("= "+nf.format(sum)+"<br>");
						}
						if (minim > maxim) {
							maxim = minim;
							maxInd = minInd;
						}
						if (minPos > maxPos) { maxPos = minPos; mIndPos=indPos; }
						if (minVol > maxVol) { maxVol = minVol; mIndVol=indVol; }
						if (minCol > maxCol) { maxCol = minCol; mIndCol=indCol; }
						//System.out.println("<b>("+minInd+")</b>:"+nf.format(minim));
					}
					System.out.println("<br>"+"<b>max ("+maxInd+")</b>: " + nf.format(maxim));
					System.out.println("<br>"+"<b>pos ("+mIndPos+")</b>: " + nf.format(maxPos));
					System.out.println("<br>"+"<b>vol ("+mIndVol+")</b>: " + nf.format(maxVol));
					System.out.println("<br>"+"<b>col ("+mIndCol+")</b>: " + nf.format(maxCol));
					
					//double hd0 = ColorManipulation.histogramDistance(pimg,pimgc,null);
					double hd1 = ColorManipulation.colorHistogramDistance(pimg,pimgc);
					//System.out.println("<br>"+"<b>h0</b>: " + nf.format(hd0));					
					System.out.println("<br>"+"<b>h1</b>: " + nf.format(hd1));
					
					System.out.println("</td>");
					
				}
				
				System.out.println("</tr>");
			}

			System.out.println("</table></body></html>");
		} catch (Exception e) {
			System.err.println("main: " + e);
		}
	} // end main
	
}

