package jclustering;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;

/**
 * Math helper class.
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 * 
 */
public class MathUtils {
    
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
    
    /**
     * Computes the root-mean-square deviation for the given TACs
     * @param x1 TAC 1
     * @param x2 TAC 2
     * @return RMSD for the given TACs
     */
    public static double rmsd(double [] x1, double [] x2) {
        
        double res = 0.0;
        
        for (int i = 0; i < x1.length; i++)
            res += FastMath.pow(x1[i] - x2[i], 2);
        
        res = FastMath.sqrt(res / x1.length);
        
        return res;
    }
    
    /**
     * Computes the normalized RMSD for the given TACs
     * @param x1 TAC 1
     * @param x2 TAC 2
     * @return NRMSD for the given TACs
     */
    public static double nrmsd(double [] x1, double [] x2) {
        
        double rmsd = rmsd(x1, x2);
        double min = StatUtils.min(x1);
        double max = StatUtils.max(x1);
        
        return rmsd / (max - min);
    }
    
    /**
     * Checks if a given voxel is zero (has been masked).
     * @param v Voxel to check
     * @return @code{true} if the voxel is always 0.0. @code{false} otherwise.
     */
    public static boolean isMasked(Voxel v) {
        return isMasked(v.tac);
    }
    
    /**
     * Checks if a given voxel is zero (has been masked).
     * @param tac Time-activity curve to check
     * @return @code{true} if the voxel is always 0.0. @code{false} otherwise.
     */
    public static boolean isMasked(double [] tac) {
        for (int i = 0; i < tac.length; i++)
            if (tac[i] != 0.0) return false;
        return true;
    }

}
