package jclustering.techniques;

import java.awt.GridLayout;
import java.awt.event.ItemEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.StorelessCovariance;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
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
    
    // Should the PCA image be shown after processing?
    private boolean showPCA = true;
    
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
        
        // If the PCA image is to be shown, create a new image with
        // result.getRowDimension() frames and the original number of 
        // x, y, z dimensions
        if (showPCA) {            
            // Get number of components
            int components = result.getRowDimension();
            
            // Create dynamic image
            ImagePlus PCA_image = IJ.createImage("PCA image", "16-bit", 
                                                 dim[0], dim[1],
                                                 dim[3] * components);
            PCA_image.setDimensions(1, dim[3], components);
            PCA_image.setOpenAsHyperStack(true);
            
            // Get stack for easy access
            ImageStack is = PCA_image.getStack();
            
            int column_index = 0;
            
            // Assign voxels to values. It is important to iterate the image
            // in the correct order (first x, then y, then slices), because
            // that is the way the images are normally processed when using
            // the ImagePlusHypIterator object.
            for(int z = 0; z < dim[3]; z++) {
                for(int y = 0; y < dim[1]; y++) {
                    for(int x = 0; x < dim[0]; x++) {
                        
                        // Test it the TAC is noise. If it is noise, 
                        // jump to the next one
                        double [] tac = ip.getTAC(x, y, z + 1);
                        if (skip_noisy && isNoise(tac)) continue;
                        
                        // Not noise: get the next column
                        double [] comp = result.getColumn(column_index++);
                        
                        // Iterate through the component and set the values.
                        // Each row of the component is in one frame.
                        for (int t = 0; t < components; t++) {
                            // Get internal slice number
                            int sn = PCA_image.getStackIndex(1, z + 1, t + 1);
                            is.setVoxel(x, y, sn, comp[t]);                            
                        }                        
                    }
                }
            }
            
            PCA_image.show();
        } // End PCA image show.
        
        // Please note: this is somehow incorrect. As the clustering model
        // that we are following needs one voxel -> one cluster, this step
        // below assigns each voxel to the principal component with the
        // maximum weight for its particular kinetics. In "real life", the
        // resulting images would contain the contribution of that component
        // in all voxels, but for segmentation purposes this approach is
        // chosen.
        int column_index = 0;
        for (Voxel v : ip) {
            
            if (skip_noisy && isNoise(v)) continue;
            
            double [] projection = result.getColumn(column_index++);   
            
            // Every Voxel belongs to the maximum index of its projected TAC
            int max = getMaxIndex(projection) + 1;
            addTACtoCluster(v, max);            
        }        
        
    }
    
    public JPanel makeConfig() {
        JPanel jp = new JPanel(new GridLayout(1, 2, 5, 5));
        
        jp.add(new JLabel("Show PCA image:"));
        JCheckBox jcb_showPCA = new JCheckBox();
        jcb_showPCA.setSelected(showPCA);
        jcb_showPCA.addItemListener(this);
        jp.add(jcb_showPCA);
        
        return jp;        
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
    
    public void itemStateChanged(ItemEvent arg0) {
        
        // Check the checkbox for the showPCA variable
        JCheckBox jcb = (JCheckBox)arg0.getSource();
        showPCA = jcb.isSelected();
        
    }
    
}
