package titech.image.dsp;

import javax.media.jai.*;

public class EdgeImage {

    public static final int SOBEL = 1;
    public static final int GRADIENT = 2;
    public static final int GAUSSIAN_GRADIENT = 3;
    public static final int GAUSSIAN_LAPLACIAN = 4;
    public static final int GAUSSIAN_GRADIENT_CANNY = 5;

    /** Gradient Magnitude */
    public PlanarImage magnitude;

    /** Gradient Direction (angle) */
    public PlanarImage direction;

    public EdgeImage(PlanarImage source) {
	this(source, GRADIENT);
    }

    public EdgeImage(PlanarImage source, int algorithm) {
	//magnitude = COps.cannyEdge(source);
	PlanarImage[] vector = null;
	double scale = 1./255.;
	switch (algorithm) {
	case SOBEL:
	    magnitude = COps.sobelGradientMagnitude(source);
	    break;
	case GRADIENT:
	    vector = Difference.gradientVector(source);
	    magnitude = Difference.gradientMagnitude(vector);
	    break;
	case GAUSSIAN_GRADIENT:
	    source = COps.gaussianBlur(source, 5, 1.);
	    vector = Difference.gradientVector(source);
	    magnitude = Difference.gradientMagnitude(vector);	    
	    break;
	case GAUSSIAN_GRADIENT_CANNY:
	    PlanarImage cannied = COps.cannyEdge(source);
	    source = COps.gaussianBlur(source, 5, 1.);
	    vector = Difference.gradientVector(source);
	    magnitude = COps.add(Difference.gradientMagnitude(vector),
				 COps.multiply(cannied,0.5));
	    scale = 1./510.;
	    break;
	default: // SOBEL
    	    magnitude = COps.sobelGradientMagnitude(source);
	}
	if (vector == null) vector = Difference.gradientVector(source);
	direction = Difference.gradientDirection(vector);

	magnitude = COps.multiply(magnitude, scale); // 0..1
    }

    public int getWidth() { return magnitude.getWidth(); }
    public int getHeight() { return magnitude.getHeight(); }

}
