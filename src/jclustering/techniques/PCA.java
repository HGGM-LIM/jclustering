package jclustering.techniques;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.StorelessCovariance;

import ij.IJ;
import jclustering.Voxel;
import static jclustering.MathUtils.getMaxIndex;

/**
 * Implements a PCA clustering according to 
 * <a href="www.sccg.sk/~haladova/principal_components.pdf">this excellent 
 * guide</a>.
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 */
public class PCA extends ClusteringTechnique {

    private int dimensions;
    
    private double[][] normalized_data;
    
    @Override
    public void process() {
    
        // Get image dimensions, store PCA dimensions (number of frames).
        int [] dim = ip.getDimensions();        
        dimensions = dim[4];
        
        // Get mean for each dimension
        IJ.showStatus("PCA: computing mean...");
        double [] mean = _getMean();
        
        // Obtain covariance matrix
        IJ.showStatus("PCA: computing covariance matrix...");
        RealMatrix cov = _generateCovariance(mean);        
        
        // Eigenvectors and eigenvalues (the second double parameter is dummy).
        // The EigenDecomposition offers the eigenvectors in descending
        // eigenvalue order.
        IJ.showStatus("PCA: obtaining eigenvectors...");
        EigenDecomposition ed = new EigenDecomposition(cov, 0.0);        
        RealMatrix eigenvectors = _getEigenVectors(ed);
        
        RealMatrix normalized_data_matrix = new 
                Array2DRowRealMatrix(normalized_data);
        
        // Compute projected vectors and fill clusters object
        IJ.showStatus("PCA: computing projected vectors and segmentation...");
        RealMatrix result = eigenvectors.multiply(
                            normalized_data_matrix.transpose());
        
        int column_index = 0;
        for (Voxel v : ip) {
            
            if (skip_noisy && isNoise(v)) continue;
            
            double [] projection = result.getColumn(column_index++);   
            
            // Every Voxel belongs to the maximum index of its projected TAC
            int max = getMaxIndex(projection) + 1;
            addTACtoCluster(v, max);            
        }        
        
    }
    
    /*
     * Get mean for each dimension and initializes normalized data to its size.
     */
    private double [] _getMean() {
        
        double [] mean = new double[dimensions];   
        int total_voxels = 0;
        
        for (Voxel v : ip) {            
            // Skip noise
            if (skip_noisy && isNoise(v)) continue;
            
            total_voxels++;
            
            for (int i = 0; i < dimensions; i++) {
                mean[i] += v.tac[i];
            }
        }
        
        normalized_data = new double[total_voxels][dimensions];

        // Mean value
        for (int i = 0; i < dimensions; i++) {
            mean[i] /= total_voxels;
        }
        
        return mean;
        
    }

    /*
     * Generates covariance matrix with mean removed 
     */
    private RealMatrix _generateCovariance(double [] mean) {
        
        StorelessCovariance sc = new StorelessCovariance(dimensions);
        
        int i = 0;
        
        for (Voxel v : ip) {
            // Skip noisy
            if (skip_noisy && isNoise(v)) continue;
            
            // Subtract the mean and add to the StorelessCovariance object
            double [] norm = _subtractMean(v.tac, mean);
            // Fill in normalized_data, initialized in the _getMean() method.
            normalized_data[i++] = norm;
            sc.increment(norm);
        }
        
        return sc.getCovarianceMatrix();
        
    }
    
    /*
     * Returns a TAC with the mean removed
     */
    private double [] _subtractMean(double [] tac, double [] mean) {
        double [] norm = new double[tac.length];
        
        for (int i = 0; i < tac.length; i++) {
            norm[i] = tac[i] - mean[i];
        }
        
        return norm;
    }

    /*
     * Returns the eigenvectors from the EigenDecomposition object
     */
    private RealMatrix _getEigenVectors(EigenDecomposition ed) {
        
        double [][] eigenarray = new double[dimensions][dimensions];
        for (int i = 0; i < dimensions; i++) {
            eigenarray[i] = ed.getEigenvector(i).toArray();
        }
        
        RealMatrix eigen = new Array2DRowRealMatrix(eigenarray);
        
        return eigen;
    }

    
}
