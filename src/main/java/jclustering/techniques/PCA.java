package jclustering.techniques;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import jclustering.Voxel;
import static jclustering.MathUtils.getMaxIndex;
import static jclustering.Utils.RealMatrix2IJ;
import static jclustering.GUIUtils.createChoices;

/**
 * <p>Implements a PCA clustering according to 
 * <a href="www.sccg.sk/~haladova/principal_components.pdf">this excellent 
 * guide</a>.</p>
 * 
 * <p>Stores as additional information the V matrix that provides the new
 * orthogonal basis.</p>
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 */
public class PCA extends ClusteringTechnique {

    private int dimensions;
    
    // Should the PCA image be shown after processing?
    private boolean showPCA = true;
    
    private double[][] normalized_data;
    
    private String transform = "Covariance";
    
    @Override
    public void process() {
    
        // Get image dimensions, store PCA dimensions (number of frames).
        int [] dim = ip.getDimensions();        
        dimensions = dim[4];
        
        // Get mean for each dimension
        IJ.showStatus("PCA: computing mean...");
        double [] mean = _getMean();
        _normalizeData(mean);
        
        // Created the normalized data, but don't copy the actual values,
        // just reference the double [][] containing it.
        RealMatrix normalized_data_matrix = new 
                Array2DRowRealMatrix(normalized_data, false);
        
        // Obtain covariance or correlation matrix
        RealMatrix c = null;
        
        if (transform.equals("Covariance")) {
            IJ.showStatus("PCA: computing covariance matrix...");
            c = (new Covariance(normalized_data_matrix)).
                              getCovarianceMatrix();
            IJ.showStatus("PCA: computing covariance matrix SVD...");
        } else if (transform.equals("Correlation")) {
            IJ.showStatus("PCA: computing correlation matrix...");
            c = (new PearsonsCorrelation(normalized_data_matrix)).
                    getCorrelationMatrix();
            IJ.showStatus("PCA: computing correlation matrix SVD...");
        }
        
        // Use SVD on the covariance matrix instead of obtaining the 
        // eigenvectors. Should return the same result, but this way is
        // conceptually better.        
        SingularValueDecomposition svd = new SingularValueDecomposition(c);
        
        // Force memory collection
        c = null;
        System.gc();        
                
        RealMatrix svdv = svd.getV(); // The additional info provided.
        
        // Force memory collection
        svd = null;
        System.gc();
        
        IJ.showStatus("PCA: computing projected vectors and segmentation...");
        RealMatrix result = normalized_data_matrix.multiply(svdv.transpose());
        result = result.transpose();

        // If the PCA image is to be shown, create a new image with
        // result.getRowDimension() frames and the original number of 
        // x, y, z dimensions
        if (showPCA) {     
            ImagePlus PCA_image = RealMatrix2IJ(result, dim, this.ip, 
                    skip_noisy, "PCA image");
            PCA_image.show();
        }
        
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
        
        // Fill in the additionalInfo array.
        additionalInfo = new String[2];
        additionalInfo[0] = "pca_vectors";        
        StringBuilder sb = new StringBuilder();
        int rows = svdv.getRowDimension();
        for (int i = 0; i < rows; i++) {
            double [] row = svdv.getRow(i);
            sb.append(Arrays.toString(row));
            sb.append("\n");
        }
        // Remove brackets
        String temp = sb.toString();
        temp = temp.replace("[", "");
        temp = temp.replace("]", "");
        additionalInfo[1] = temp;
        
    }
    
    public JPanel makeConfig() {
        JPanel jp = new JPanel(new GridLayout(2, 2, 5, 5));
        
        // Show PCA image?
        jp.add(new JLabel("Show PCA image:"));
        JCheckBox jcb_showPCA = new JCheckBox();
        jcb_showPCA.setName("jcb_showPCA");
        jcb_showPCA.setSelected(showPCA);
        jcb_showPCA.addItemListener(this);
        jp.add(jcb_showPCA);
        
        // Use covariance or correlation matrix for PCA?
        jp.add(new JLabel("Use covariance or correlation:"));
        String [] t = new String[]{"Covariance", "Correlation"};
        JComboBox jcb_matrix = createChoices("jcb_matrix", t, this);
        jcb_matrix.setSelectedIndex(0);
        jp.add(jcb_matrix);
        
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
     * Generates removes mean from every variable (column)
     */
    private void _normalizeData(double [] mean) {
        
        int i = 0;
        
        for (Voxel v : ip) {
            // Skip noisy
            if (skip_noisy && isNoise(v)) continue;
            
            // Subtract the mean
            double [] norm = _subtractMean(v.tac, mean);
            // Fill in normalized_data, initialized in the _getMean() method.
            normalized_data[i++] = norm;
        }
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

    public void itemStateChanged(ItemEvent arg0) {
        
        Component c = (Component) arg0.getSource();
        String s = c.getName();
        
        if (s.equals("jcb_showPCA")) {
            // Check the checkbox for the showPCA variable
            JCheckBox jcb = (JCheckBox) c;
            showPCA = jcb.isSelected();
        } else if (s.equals("jcb_matrix")) {
            // Check the type of PCA selected
            JComboBox jcb = (JComboBox) c;
            transform = (String) jcb.getSelectedItem();        
        }
        
    }    
}
