package titech.image.math;

public class PolarPoint implements java.util.Comparator {

	double r;
	double theta;
	
	public PolarPoint(double r, double theta) {
		this.r = r;
		this.theta = theta;
	}
	
	/** Compares its two arguments for order. 
	  * @see java.util.Comparator 
	  */
	public int	compare(Object o1, Object o2) throws ClassCastException {
		// if class differs, throws a ClassCastException
		PolarPoint pp1 = (PolarPoint)o1;
		PolarPoint pp2 = (PolarPoint)o2;
		
		// first check distance
		if (pp1.r - pp2.r < 0) return -1; // less
		if (pp1.r - pp2.r > 0) return 1; // greater
		// is same distance, check angle
		if (pp1.theta - pp2.theta < 0) return -1;
		if (pp1.theta - pp2.theta > 0) return 1;
		
		// they are equal
		return 0;
	}
    
	/** Indicates whether some other object is "equal to" this Comparator.
	 * @see java.util.Comparator
	 */
	public boolean	equals(Object obj) { 
		if (this.getClass().isInstance(obj)) { // same class
			PolarPoint pp = (PolarPoint) obj;
			if (pp.r == this.r && pp.theta == pp.theta) return true;
		}
		return false;
	}
 
}
