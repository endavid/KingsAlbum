package titech.image.dsp;

import titech.nn.Perceptron;

public class ImageCategorization {

	/** Image Categories */
	public static final int ARTIFICIAL=1, NATURAL=2, PORTRAIT=3, TEXT=4;
	
	private Perceptron net;
	
	public ImageCategorization() {
		net = new Perceptron(new int[] {13*5,26,10,4});
	}
	
	/**
	 * @param array of target image categories, pairing each input.
	 */
	public void learn(ObjectImage[] inputs, int[] targets) {
		// pick up the 5 biggest regions from each input, ordered by size.
		
	}
}

