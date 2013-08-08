package jclustering.techniques;

import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import ij.IJ;
import ij.ImagePlus;
import jclustering.Voxel;
import static jclustering.MathUtils.getMaxIndex;
import static jclustering.Utils.RealMatrix2IJ;

/**
 * Implements a SVD on the original image matrix.
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 */
public class SVD extends ClusteringTechnique {

    private int dimensions;
    
    // Should the SVD image be shown after processing?
    private boolean showSVD = true;
    
    @Override
    public void process() {
    
        // Get image dimensions, store SVD dimensions (number of frames).
        int [] dim = ip.getDimensions();        
        dimensions = dim[4];
        
        // Compute number of real voxels to be used 
        int n = 0;
        for (Voxel v : ip) {
            if (skip_noisy && isNoise(v)) continue;
            n++;
        }    
        
        // Create new array and fill it with the image data
        double [][] image_data = new double[n][dimensions];
        
        int i = 0;
        for(Voxel v : ip) {
            if(skip_noisy && isNoise(v)) continue;
            image_data[i++] = v.tac;
        }
        
        // Use "false" as the second argument prevents the new object from
        // copying all the data. It is not needed, the matrix object will not be
        // modified.
        Array2DRowRealMatrix data = new Array2DRowRealMatrix(image_data, false);
        
        // Use SVD on the data matrix.
        IJ.showStatus("SVD: computing singular values...");
        SingularValueDecomposition svd = new SingularValueDecomposition(data);        
        RealMatrix svdv = svd.getV();
        
        RealMatrix result = data.multiply(svdv.transpose());
        
        // If the SVD image is to be shown, create a new image with
        // result.getRowDimension() frames and the original number of 
        // x, y, z dimensions
        if (showSVD) {       
            // Have to transpose the result matrix as the rows and columns
            // need to be in the opposite orientation.
            ImagePlus SVD_image = RealMatrix2IJ(result.transpose(), dim, 
                                                this.ip, 
                                                skip_noisy, "SVD image");
            SVD_image.show();
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
            
            double [] projection = result.getRow(column_index++);   
            
            // Every Voxel belongs to the maximum index of its projected TAC
            int max = getMaxIndex(projection) + 1;
            addTACtoCluster(v, max);            
        }
        
        // Fill in the additionalInfo array.
        additionalInfo = new String[2];
        additionalInfo[0] = "svd_v_matrix";        
        StringBuilder sb = new StringBuilder();
        int rows = svdv.getRowDimension();
        for (int j = 0; j < rows; j++) {
            double [] row = svdv.getRow(j);
            sb.append(Arrays.toString(row));
            sb.append("\n");
        }
        // Remove brackets
        String tempstr = sb.toString();
        tempstr = tempstr.replace("[", "");
        tempstr = tempstr.replace("]", "");
        additionalInfo[1] = tempstr;
        
    }
    
    public JPanel makeConfig() {
        JPanel jp = new JPanel(new GridLayout(1, 2, 5, 5));
        
        jp.add(new JLabel("Show SVD image:"));
        JCheckBox jcb_showSVD = new JCheckBox();
        jcb_showSVD.setSelected(showSVD);
        jcb_showSVD.addItemListener(this);
        jp.add(jcb_showSVD);
        
        return jp;        
    }
    
    public void itemStateChanged(ItemEvent arg0) {
        
        // Check the checkbox for the showPCA variable
        JCheckBox jcb = (JCheckBox)arg0.getSource();
        showSVD = jcb.isSelected();
        
    }
    
}
