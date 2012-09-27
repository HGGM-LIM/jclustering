package jclustering.metrics;

import ij.IJ;

import java.util.Arrays;

import jclustering.Voxel;

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
    
    private RealMatrix invcov = null;
    
    @Override
    public double distance(double[] centroid, double[] data) {  
     
        // If the arrays are the same, the distance is 0.0
        if (Arrays.equals(centroid, data)) {
            return 0.0;
        }
        
        // Create a new array with the difference between the two arrays
        double [] diff = new double[centroid.length];
        
        for (int i = 0; i < centroid.length; i++) {
            diff[i] = centroid[i] - data[i];
        }       
        
        // Left-hand side of the equation: vector * invcov^-1
        double [] left = invcov.preMultiply(diff);
        
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
    public void init() {
        
        IJ.showStatus("Mahalanobis metric initializing...");
        
        int dim[] = ip.getDimensions();
        StorelessCovariance sc = new StorelessCovariance(dim[4]);
        
        // Feed the StorelessCovariance
        for (Voxel v : ip) {
            if (skip_noisy && isNoise(v)) continue;
            
            sc.increment(v.tac);
        }
        
        // Set the covariance value
        RealMatrix temp = sc.getCovarianceMatrix();
        
        // But this matrix is always used inverted. Do it now.
        invcov = new LUDecomposition(temp).getSolver().getInverse();

    }

}
