package jclustering.metrics;

import java.util.Arrays;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.StorelessCovariance;

/**
 * Implements a Mahanalnobis distance. See 
 * <a href="http://en.wikipedia.org/wiki/Mahalanobis_distance">the
 * Mahalanobis distance</a> page on Wikipedia for more information.
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 *
 */
public class Mahalanobis extends ClusteringMetric {

    private boolean init = false;
    private RealMatrix cov = null;
    
    @Override
    public double distance(double[] centroid, double[] data) {
        
        // Need some data before starting
        if (!init) {
            _init();
        }
        
        // If the arrays are the same, the distance is 0.0
        if (Arrays.equals(centroid, data)) {
            return 0.0;
        }
        
        // Create a new array with the difference between the two arrays
        double [] diff = new double[centroid.length];
        
        for (int i = 0; i < centroid.length; i++) {
            diff[i] = centroid[i] - data[i];
        }       
        
        // Left-hand side of the equation: vector * cov^-1
        double [] left = cov.preMultiply(diff);
        
        // Compute the dot product of both vectors
        double res = 0.0;
        for (int i = 0; i < diff.length; i++) {
            res += left[i] * diff[i];
        }
        
        return Math.sqrt(res);
        
    }
    
    /*
     * Initializes the covariance matrix
     */
    private void _init() {
        
        int dim[] = ip.getDimensions();
        StorelessCovariance sc = new StorelessCovariance(dim[4]);
        
        // Feed the StorelessCovariance
        for (int slice = 1; slice < dim[3]; slice++) {
            for (int x = 0; x < dim[0]; x++) {
                for (int y = 0; y < dim[1]; y++) {                    
                    double [] tac = ip.getTAC(x, y, slice);
                    sc.increment(tac);                    
                }
            }
        }
        
        // Set the covariance value
        RealMatrix temp = sc.getCovarianceMatrix();
        
        // But this matrix is always used inverted. Do it now.
        cov = new LUDecomposition(temp).getSolver().getInverse();
        
        // Don't init again
        init = true;
        
    }

}
