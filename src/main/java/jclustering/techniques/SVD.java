package jclustering.techniques;

import java.awt.GridLayout;
import java.awt.event.ItemEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import jclustering.Voxel;
import static jclustering.MathUtils.getMaxIndex;

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
        
        // Compute number of real voxels to be used voxels
        int n = 0;
        if(!skip_noisy) n = dim[0] * dim[1] * dim[3]; // No noise check: use all
        else {
            for(Voxel v: ip) {
                if(isNoise(v)) continue;
                n++;
            }
        }
        
        System.out.println(n);
        
        // Create new array and fill it with the image data
        
        double [][] image_data = new double[n][dimensions];
        
        int i = 0;
        for(Voxel v : ip) {
            image_data[i] = v.tac;
        }
        
        // Use "false" as the second argument prevents the new object from
        // copying all the data. It is not needed, the matrix object will not be
        // modified.
        Array2DRowRealMatrix data = new Array2DRowRealMatrix(image_data, false);
        
        RealMatrix data2 = data.transpose();

        // Use SVD on the data matrix.
        IJ.showStatus("SVD: computing covariance matrix SVD...");
        SingularValueDecomposition svd = new SingularValueDecomposition(data2);        
        RealMatrix result = svd.getV();
        
        // Dim
        System.out.println("Dim: " + result.getRowDimension() + " " + 
                            result.getColumnDimension());
                
        // Force memory collection
        svd = null;
        System.gc();
        
        // If the SVD image is to be shown, create a new image with
        // result.getRowDimension() frames and the original number of 
        // x, y, z dimensions
        if (showSVD) {            
            // Get number of components
            int components = result.getColumnDimension();
            
            // Create dynamic image
            ImagePlus SVD_image = IJ.createImage("SVD image", "16-bit", 
                                                 dim[0], dim[1],
                                                 dim[3] * components);
            SVD_image.setDimensions(1, dim[3], components);
            SVD_image.setOpenAsHyperStack(true);
            
            // Get stack for easy access
            ImageStack is = SVD_image.getStack();
            
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
                        double [] comp = result.getRow(column_index++);
                        
                        // Iterate through the component and set the values.
                        // Each row of the component is in one frame.
                        for (int t = 0; t < components; t++) {
                            // Get internal slice number
                            int sn = SVD_image.getStackIndex(1, z + 1, t + 1);
                            is.setVoxel(x, y, sn, comp[t]);                            
                        }                        
                    }
                }
            }
            
            SVD_image.show();
        } // End SVD image show.
        
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
