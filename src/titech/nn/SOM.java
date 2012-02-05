package titech.nn;

import titech.image.math.AMath;
import java.io.*;

/**
 * Class that implements a SOM Network (Self-Organized Maps).
 * <p>History:
 * <ul> 
 *  <li> 04/03/21 First Version
 * </ul>
 * @author David Gavilan
 */
public class SOM {
	public final static int SQEUCLIDEAN=1;
    /** Number of inputs */
    int nin;

    /** Number of outputs */
    int nout;

    /** Number of weights and biases */
    int nwts;

    /** Type of Distance Function */
    int func;

    /** Matrix of weights. Each column represents an input. */
    double[][] w;
	
	public double[][] getWeights() {
		return w;
	}
	
	public int getNout() { return nout; }
	public int getNin() { return nin;}
	
	public SOM(int nin, int nout) {
		this(nin, nout, SQEUCLIDEAN);
	}
	
	/** Creates a 1-Layer SOM */
	public SOM(int nin, int nout, int func) {
        this.nin = nin;
        this.nout = nout;
        this.func = func;
        nwts = (nin + 1) * nout;
        w = new double[nout][nin];
	}
	
	public SOM(Reader reader) {
		load(reader);
	}
	
	/** Random initialization, normalized to module 1. */
	public void iniRandom() {
		for (int i=0;i<nin;i++) {
			for (int j=0;j<nout;j++) {
				w[j][i]=1.0-2.0*Math.random();
			}
		}
		for (int j=0;j<nout;j++) {
			double sum=0;
			for (int i=0;i<nin;i++) {
				sum+=w[j][i]*w[j][i];
			}
			if (sum==0.0) sum=1.0;
			AMath.multV(w[j],1.0/Math.sqrt(sum));
		}
	}

	/** Pick up random samples to initialize the net */
	public void iniRandomSamples(double[][] x) {
		for (int j=0;j<nout;j++) {
		int sample = (int)Math.floor(x.length*Math.random());
		for (int i=0;i<nin;i++) {
				w[j][i]=x[sample][i];
			}
		}
	}
	
	public int selectWinnerNeuron(double[] xp) {
		int winner=0;
		double min=AMath.VERY_BIG;
		for (int j=0;j<nout;j++) {
			double d=AMath.VERY_BIG;
			switch(func) {
				case SQEUCLIDEAN:
				d=AMath.distance(w[j],xp);
			}
			if (d<min) {
				winner=j;
				min=d;
			}
		}
		
		return winner;
	}

	/**
	 * @param x inputs, x[nsamples][dimension];
	 */
	 public void learn(double[][] x, double a, double e){
		 while (a>0.0001) {
			 for (int p=0;p<x.length;p++) {
				 int j=selectWinnerNeuron(x[p]);
				 for (int i=0;i<nin;i++)
					 w[j][i]=w[j][i]+a*(x[p][i]-w[j][i]);
			 }
			 a*=e;
		 }
	 }
	 
	 /** Reads a network from a file */
	 public void load(Reader reader) {
		 StreamTokenizer st = new StreamTokenizer(reader);
		 int varFound = 0;
		 
		 try {
			 

			 while (st.nextToken()!=StreamTokenizer.TT_EOF && varFound <2) {
				 if (st.ttype == StreamTokenizer.TT_NUMBER) {
					 if (varFound == 0) {
						 this.nin = (int)st.nval;
					 } else {
						 this.nout = (int)st.nval;
					 }
					 varFound++;
				 }
			 }
 			 this.func = SQEUCLIDEAN;

	         nwts = (nin + 1) * nout;
			 w = new double[nout][nin];
			 
			 varFound = 0;
			 int i=0,j=0;
			 while (st.nextToken()!=StreamTokenizer.TT_EOF && varFound <nwts) {
				 if (st.ttype == StreamTokenizer.TT_NUMBER) {
					 w[i][j++]=st.nval;
					 if (j>=nin) { j=0; i++; }
					 varFound++;
				 }
			 }
			 
		 } catch (java.io.IOException ex) {
			 System.err.println("SOM: "+ex);
		 }
	 }
	 
	 public String toString() {
		 return new String("SOM NN: "+nin+" inputs, "+nout+" outputs");
	 }
	 
	 public final static void main(String[] args) {
		 SOM net = new SOM(Integer.parseInt(args[0]),
		 		Integer.parseInt(args[1]));
		
		net.iniRandom();
		net.w=	new double[][] {new double[] {0.45, 0, 0},
			new double[] {0, 0, 0.1},
			new double[] {0, 0.4, 0},
			new double[] {0, 0, 0.9},
			new double[] {0.3, 0.6, 0.9}};

		System.out.println(AMath.showMatrix(net.w));
		
		net.learn(new double[][] {
			new double[] {0.5, 0, 0},
			new double[] {0, 0, 0},
			new double[] {0, 0.5, 0},
			new double[] {0, 0, 0.9},
			new double[] {0.5, 0, 0},
			new double[] {0.4, 0.5, 0.9},
			new double[] {0, 0.5, 0},
			new double[] {0, 0, 0.9},
			new double[] {0.5, 0, 0},
			new double[] {0, 0, 0},
			new double[] {0, 0.5, 0},
			new double[] {0, 0, 0.8},
			new double[] {0.5, 0, 0},
			new double[] {0, 0, 0},
			new double[] {0, 0.5, 0},
			new double[] {0, 0, 0.9},
			new double[] {0.5, 0, 0},
			new double[] {0.4, 0.6, 0.7},
			new double[] {0, 0.5, 0},
			new double[] {0, 0, 0.9},
			new double[] {0.5, 0, 0},
			new double[] {0, 0, 0},
			new double[] {0, 0.5, 0},
			new double[] {0, 0, 0.9},
			new double[] {0.5, 0, 0},
			new double[] {0, 0, 0},
			new double[] {0, 0.5, 0},
			new double[] {0, 0, 0.9},
			new double[] {0.5, 0, 0},
			new double[] {0, 0, 0},
			new double[] {0, 0.5, 0},
			new double[] {0, 0, 0.9},
			new double[] {0.5, 0, 0},
			new double[] {0, 0, 0},
			new double[] {0, 0.5, 0},
			new double[] {0, 0, 0.9},
			new double[] {0.5, 0, 0},
			new double[] {0, 0, 0},
			new double[] {0, 0.5, 0},
			new double[] {0, 0, 0.9}
			
			}, 0.8, 0.00000001);
			
		System.out.println(AMath.showMatrix(net.w));
			
	}
}

