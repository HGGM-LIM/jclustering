package jclustering;

/**
 * Math helper class.
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 * 
 */
public class MathUtils {

    /**
     * Returns the maximum value for the given array
     * 
     * @param data Array of double precision values.
     * @return The maximum element for the array.
     */
    public static double getMax(double[] data) {

        // Initialize to first element
        double r = data[0];

        // Get the maximum
        for (double d : data) {
            if (d > r) {
                r = d;
            }
        }

        return r;
    }
    
    /**
     * Returns the index for the maximum value of the array.
     * @param d A double array.
     * @return The index for the maximum value.
     */
    public static int getMaxIndex(double[] d) {

        int res = 0;
        double aux = d[0];

        for (int i = 0; i < d.length; i++) {
            if (d[i] > aux) {
                res = i;
                aux = d[i];
            }
        }

        return res;
    }
    
    /**
     * Smooths the given TAC using a 5-point filtering.
     * @param data The raw TAC.
     * @return The smoothed TAC.
     */
    public static double [] smooth(double [] data) {
        
        int t = data.length;
        double [] res = data.clone();
        double kernel[] = {0.13, 0.185, 0.37, 0.185, 0.13};        
        for (int i = 2; i < t-2; i++) {
            res[i] = res[i-2]*kernel[0] + res[i-1]*kernel[1] + res[i]*kernel[2]
                    + res[i+1]*kernel[3] + res[i+2]*kernel[4];
        }
        return res;
        
        
    }

}
