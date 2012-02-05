package titech.nn;

import java.io.InputStream;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.util.StringTokenizer;


/**
 * Class that implements a MLP Network (MultiLayer Perceptron).
 * <P> It provides methods to read XMLS configuration files. 
 *
 * History: 
 *  - 03/11/13 - load method changed to general InputStream, not only File
 *             - debug made public and load(is,debug) eliminated
 *  - 03/06/22 First Version
 * @author David Gavilan
 */
public class Perceptron {

    private Layer[] layers;

    /** For bipolar functions, threshold==-1 means no threshold at all. */
    private double threshold = -1;

    /** When <code>true</code> shows some output through the console. */
    public boolean debug = false;

	
	/**
	 * Creates a Perceptron with no layers.
	 */
	public Perceptron(){
	}
	
	
	/**
	 * Initialize randomly (-1..1) the weights of each layer.
	 * @param ninput an array containing the number of inputs of each layer. The last element is the number of outputs of the last layer.
	 */
	public Perceptron(int[] ninputs) {
		int n=ninputs.length-1;
		layers = new Layer[n];
		for (int i=0;i<n;i++) {
			layers[i]=new Layer(ninputs[i],ninputs[i+1]);
			layers[i].iniRandom();
		}
	}
	
	public void setThreshold(double t) {
		threshold = t;
	}
	public double getThreshold() {
		return threshold;	
	}
    /**
      * With Matlab syntax:
      * <br><br>
	  * <code>
      * mostres=size (x,1);
      * for i=1:xarxa.Capes
      *   x=[x,ones(mostres,1)]*[xarxa.Capa(i).w1;xarxa.Capa(i).b1];
      *   x=eval (sprintf ('%s(x)',xarxa.Capa(i).actfns));
      * end
      * y=x;
      *</code>
	  *
      * @param x es una matriu de mostres de entrada, on cada fila es una mostra de entrada.
      *        Si es nomes un vector, new double[1][s]. <br> Be sure that the input is also
      *        bipolar (-1..1) when working with Bipolar Sigmoidal.
      * @return una matriu de sortides, on cada fila es la sortida de una de les mostres de entrada
      */
    public double[][] forward(double[][] x) {

        double[][] y = new double[x.length][];

        // first layer
        for (int s = 0; s < x.length; s++) {
            y[s] = layers[0].activate(x[s]);
        }

        // for the other layers, the inputs are the outputs of previous layer
        for (int i = 1; i < layers.length; i++) {

            for (int s = 0; s < x.length; s++) {
                y[s] = layers[i].activate(y[s]);
            }
        }

        return y;
    }

	
	/**
	 * Changes the state of the Network by applying a cycle of Backpropagation learning.
	 * @param x inputs, x[nsamples][dimension];
	 * @param t objectives;
	 * @param eta learning coeficient
	 */
	public void backPropagation(double[][] x, double[] t,double eta) {
		int nsamples = x.length;
		
		for(int i=0;i<nsamples;i++) {
			// nets (entrades netes)
			double[][] xx=new double[1][x[0].length];
			xx[0]=x[i];
			double[][] y=forward(xx); // stored in Layer.netInputs
			
			// calculate deltas
			double[] error = new double[t.length];
			for (int z=0;z<t.length;z++) error[z]=t[z]-y[0][z];

			for (int z = layers.length; z >= 0; z--) {
				error = layers[z].delta(error);
			}

			double[] yy=x[i];
			// update weights and biases
			for (int z=0; z<layers.length; z++) {
				yy=layers[z].update(yy,eta);
			}
			
		}
	}
	
    /**
     * Select the strongest response neuron, upto a certain threshold. The neurons are
     * numbered beginning from <b>1</b>. A <b>0</b> value means that no neuron passed
     * the threshold.
     * @param y is an output matrix, where each row is an output vector and each column
     *        is the response of each neuron to input signal.
     */
    public int[] selectWinnerNeuron(double[][] y) {

        int[] winners = new int[y.length];

        for (int i = 0; i < y.length; i++) {

            int winner = 0;
            double winnerVal = y[i][0];

            for (int j = 1; j < y[0].length; j++) {

                if (y[i][j] > winnerVal) {
                    winner = j;
                    winnerVal = y[i][j];
                }
            }

            // 0 if no neuron is selected, [1..numNeurons] otherwise
            winners[i] = (winnerVal < threshold) ? 0 : winner + 1;
        } // for all samples

        return winners;
    }


    /** Reads a Perceptron object from an XML file.
     *  @param stream InputStream (URL, File...) to the XML file
     *  @param debug whether or not to show some output to the console.
     */
    public void load(InputStream stream)
              throws javax.xml.parsers.ParserConfigurationException, 
                     org.xml.sax.SAXException, java.io.IOException {

        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = db.parse(stream);
        Element root = doc.getDocumentElement();
        int nlayers = Integer.parseInt(root.getAttribute("nlayers"));
        NodeList layerNodes = root.getElementsByTagName("layer");
        int n = layerNodes.getLength();
        layers = new Layer[n];
		if (debug) System.out.println("Layers: "+n);		

        if (n != nlayers)
            System.err.println("Warning: number of layers differ");

        for (int i = 0; i < n; i++) {

            // cada item es un Node generico (CDATA, etc..), pero
            // hacemos un cast a Element, que es lo que sabemos que
            // devuelve el getElementsByTagName
            Element item = (Element)layerNodes.item(i);
			int nin = Integer.parseInt(item.getAttribute("nin"));
			int nout = Integer.parseInt(item.getAttribute("nout"));
			layers[i]=new Layer(nin,nout);
			
            NodeList weights = item.getElementsByTagName("weights");
            int ns = weights.getLength(); // solo tiene que haber una

            if (ns > 1)
                System.err.println("Warning: more than one matrix of weights");
			if (ns < 1) {
                System.err.println("Error: no matrix of weights!");
			} else {
				CharacterData wm = (CharacterData)weights.item(0).getFirstChild();
				if (debug) System.out.println(wm.getNodeName()+": "+wm.getData());
				layers[i].iniWeights(wm.getData());
				//System.out.println(titech.image.math.AMath.showMatrix(layers[i].w));
			}
			
            NodeList biases = item.getElementsByTagName("biases");
            ns = biases.getLength(); // solo tiene que haber una

            if (ns > 1)
                System.err.println("Warning: more than one vector of biases");
			if (ns < 1) {
                System.err.println("Error: no bias vector!");
			} else {
				CharacterData wm = (CharacterData)biases.item(0).getFirstChild();
				if (debug) System.out.println(wm.getNodeName()+": "+wm.getData());
				layers[i].iniBiases(wm.getData());
			}

        }
    }
	
	/** This main method is just to write a static look-up table.
	  * This table is used in the mobile application.
	  * # titech.nn.Perceptron [nn.xml] [output]
	  */
	public final static void main(String[] args) {
		 Perceptron net = new Perceptron();
		 try {
			 net.load(new java.io.FileInputStream(args[0])); //colorsEx.xml
		 } catch (Exception e) {
			 System.err.println(e);
		 }
		 byte[] lut=new byte[32768]; // 15-bit hi-color
		 int i=0;
		 for (int r=0;r<32;r++){
		  for (int g=0;g<32;g++)
		 	for (int b=0;b<32;b++) {
				double[][] x=titech.image.dsp.ColorCategorization.prepareColorInput( 
				  new double[] {(r*8.)/255.,(g*8.)/255.,(b*8.)/255.});
				titech.image.dsp.ColorCategorization.bipolarize(x);
				int[] wn = net.selectWinnerNeuron(net.forward(x));
				lut[i++]=(byte)wn[0];
			} 
		 }
		 
		 if (args.length>1) {
			 try {
				 java.io.FileOutputStream fo = new java.io.FileOutputStream(args[1]);
				 fo.write(lut);
				 fo.close();
			 } catch (Exception e) { 
				System.err.println(e);
			 }
		 } else {
			 //String format="";
			 //int cols = 20;
			 i=0;
			 java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
		     nf.setMaximumFractionDigits(4);
			 
			 for (int r=0;r<32;r++){
			   for (int g=0;g<32;g++)
			    for (int b=0;b<32;b++) {
					float[] xyz = titech.image.dsp.ColorManipulation.RGBtoXYZ( 
					  new float[] {(r*8f)/255f,(g*8f)/255f,(b*8f)/255f} );
					  
					  System.out.println(
					       nf.format(xyz[0])+"\t"+
						   nf.format(xyz[1])+"\t"+
						   nf.format(xyz[2])+"\t"+lut[i]);
					  /*
					  format+="("+xyz[0]+","+xyz[1]+","+xyz[2]+") = ";
				      format+=lut[i]+", ";
				      cols--;
					  if (cols<1) {
					    System.out.println(format);
						  format="";
						  cols=20;
					  }
					  */
					  i++;
					  
				}
			 }
			 //System.out.println(format);
		 }
	}

}

class Layer {

    /** Number of inputs */
    int nin;

    /** Number of outputs */
    int nout;

    /** Number of weights and biases */
    int nwts;

    /** Type of Function */
    int func;

    /** Matrix of weights */
    double[][] w;

    /** Vector of biases */
    double[] b;

	double[] netInputs;
	
	double[] deltas;
    /**
net.type = 'MlpLayer';

net.nin = nin;
net.nout = nout;
net.nwts = (nin + 1)*nout;

net.actfns = func;
net.w1 = randn(nin, nout)/sqrt(nin + 1);
net.b1 = randn(1, nout)/sqrt(nin + 1);

net.EntradaNeta=zeros (1,nout);
net.Deltas=zeros (1,nout);
    */
	
	Layer(int nin, int nout) {
		this(nin,nout,0);
	}
	
    Layer(int nin, int nout, int func) {
        this.nin = nin;
        this.nout = nout;
        this.func = func;
        nwts = (nin + 1) * nout;
        w = new double[nin][nout];
        b = new double[nout];
		netInputs = new double[nout];
		deltas = new double[nout];
    }

    double[] activate(double[] x) {

        double[] y = new double[nout];

        for (int j = 0; j < nout; j++) {

            double net = b[j]; //bias

            for (int i = 0; i < x.length; i++) {
                net += x[i] * w[i][j];
            }

			netInputs[j]=net;
            y[j] = fact(net);
        }

        return y;
    }

	/** Returns backward-propagated errors.
	* The size of the input error should be == nout, and the output to nin.
	*/
	double[] delta(double[] error) {
		
        for (int j = 0; j < nout; j++) {
			deltas[j]=error[j]*dfact(netInputs[j]);
        }

		double[] e=new double[nin];
		
		for(int i=0;i<nin;i++) {
			e[i]=0;
			for(int j=0;j<nout;j++) {
				e[i]+=deltas[j]*w[i][j];
			}
		}
		
        return e;
	}

	/** Updates weights and biases */
   double[] update(double[] x, double eta) {

	   	double[] y = new double[nout];
		
        for (int j = 0; j < nout; j++) {
            for (int i = 0; i < nin; i++) {
                w[i][j] += eta*x[i]*deltas[j];
            }

			b[j]+=eta*deltas[j];
			
			y[j]=fact(netInputs[j]);
        }

        return y;
    }

	
	/** Activation function */
    double fact(double net) {

        double val = 0;

        // sigmoidea bipolar
        val = 2.0 / (1.0 + Math.exp(-net)) - 1.0;

        return val;
    }
	
	/** Derivative of the activation function */
	double dfact(double net) {
        double val = 0;

        // derivative of bipolar sigmoidal
        val = (1.0 + fact(net))*(1.0-fact(net))/2.0;

        return val;
	}
	
	/** Random initialization */
	void iniRandom() {
		for (int i=0;i<nin;i++) {
			for (int j=0;j<nout;j++) {
				w[i][j]=1.0-2.0*Math.random();
			}
		}
		for (int j=0;j<nout;j++) {
				b[j]=1.0-2.0*Math.random();
		}		
	}
	
	void iniWeights(String numbers) {
		StringTokenizer numSeq = new StringTokenizer(numbers);
		for (int i=0;i<nin;i++) {
			for (int j=0;j<nout;j++) {
				w[i][j]=Double.parseDouble(numSeq.nextToken());
			}
		}
	}
	
	void iniBiases(String numbers) {
		StringTokenizer numSeq = new StringTokenizer(numbers);
		for (int j=0;j<nout;j++) {
				b[j]=Double.parseDouble(numSeq.nextToken());
		}
	}	
}
