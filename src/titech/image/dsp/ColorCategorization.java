package titech.image.dsp;


import javax.media.jai.*;
import javax.media.jai.iterator.*;
import titech.nn.*;


/**
 * This class provides methods to convert RGB images to Indexed images. One of those methods
 * is classifying those colors into different color categories using Neural Networks.
 */
public class ColorCategorization {
	
	Perceptron nn = null;
	byte[][] colormap;
	int paletteBits = 8;

	/** This image contains the continuous output from the Perceptron */
	TiledImage errorImage;
	
	public ColorCategorization(Perceptron nn, int paletteBits, byte[][] colormap) {
		this.nn = nn;
		this.paletteBits = paletteBits;
		this.colormap = colormap;
		errorImage = null;
	}
	
	public TiledImage categorize(PlanarImage image) {
		return MLPCategorization(image);
	}
	
	public byte[][] getColormap() { return colormap; }
	public int getPaletteBits() { return paletteBits; }
	public Perceptron getNN() { return nn;}
	/** The error is normalized between 0 and 1, instead of the bipolar -1 and -1 values */
	public TiledImage getError() {return errorImage;}
	
	/**
	 * Categorizes using a Multi-Layer Perceptron. The first color of the palette
	 * (0, the background color) should be interpreted as colors below the threshold
	 * of the classification.<p>
	 *
	 * @see titech.nn.Perceptron
	 */
    private TiledImage MLPCategorization(PlanarImage image) {

        TiledImage outImage = ImageObjects.createIndexedImage(image.getWidth(), image.getHeight(), paletteBits, colormap);
		errorImage = ImageObjects.createFloatImage(image.getWidth(), image.getHeight(), 1);

        // Loop over the input, copy each pixel to the output, modifying // them as we go
        int bands = image.getSampleModel().getNumBands();
        int height = image.getHeight();
        int width = image.getWidth();

        // used to access the source image
        RandomIter iter = RandomIterFactory.create(image, null);


        for (int samp = 0; samp < width; samp++) {
			//String debug = "";
            for (int line = 0; line < height; line++) {

                double[] rgb = new double[3];

                for (int band = 0; band < bands; band++) {

                    double dn = iter.getSample(samp, line, band);
                    rgb[band] = (dn/255.0); // normalize between 0 and 1
                }

				double[][] v = prepareColorInput(rgb);
				bipolarize(v);
				
                // search for the cluster in the neural network
                // el orden es importante pq un color se puede parecer a varios de la paletta
				
				//System.out.println(titech.image.math.AMath.showMatrix(nn.forward(v)));
				double[][] ff = nn.forward(v);
				int activatedNeuron[] = nn.selectWinnerNeuron(ff);
				int aNeuron = activatedNeuron[0];
				//debug +=activatedNeuron[0];
                outImage.setSample(samp, line, 0, aNeuron);
                if (aNeuron==0) { // below the threshold
                	double tt = nn.getThreshold();
                	nn.setThreshold(-1); // no threshold
                	activatedNeuron = nn.selectWinnerNeuron(ff);
                	aNeuron = activatedNeuron[0];
                	nn.setThreshold(tt); // recover Threshold
                }
				errorImage.setSample(samp, line, 0, (ff[0][aNeuron-1]+1.0)/2.0); // normalized between 0..1

            }
			//System.out.println(debug);
        }

        return outImage;
    }
	
	/**
	 * This method takes RGB values (range 0-1), and outputs RGBYPL vectors, the input
	 * of our Color Categorization NN. <br>
	 * RGBYPL: Gedness, Greeness, Blueness, Yellowness, Purpleness, Lightness 
	 */
	public static double[][] prepareColorInput(double[] rgb) {
		double l = rgb[0]+rgb[1]+rgb[2];
		double y = (rgb[0]+rgb[1])/2.0;
		double p = (rgb[0]+rgb[2])/2.0;
		if (l==0) l=1;
		double r = rgb[0]/l, g = rgb[1]/l, b = rgb[2]/l;
		l/=3.0;
		
		double[][] input = new double[][] {
		new double[] { r, g, b, y, p, l}};
		
		return input;
	}
	
	/**
	 * Maps range 0..1 to a bipolar value, range -1..1
	 */
	public static void bipolarize(double[][] v) {
		for (int j=0; j<v.length; j++) {
		for (int i=0; i<v[j].length; i++) { // normalize -1 .. 1
			v[j][i]=2.0*v[j][i]-1.0;
		}
		}
	}
	
	/**
	 * Just find the minimum euclidean distance between the colors in the palette.
	 */
	public static TiledImage euclideanCategorization(PlanarImage src, int paletteBits, byte[][] colormap) {
		return ImageObjects.RGB2Indexed(src,paletteBits,colormap);
	}
}

