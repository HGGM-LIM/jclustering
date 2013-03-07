package jclustering.techniques;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.fastica.FastICA;
import org.fastica.FastICAException;

import ij.IJ;
import ij.ImagePlus;
import jclustering.Voxel;
import static jclustering.GUIUtils.createJTextField;
import static jclustering.Utils.RealMatrix2IJ;
import static jclustering.MathUtils.getMaxIndex;

/**
 * <p>Implements an Independent Component Analysis on the image data.
 * Uses the <a href="http://sourceforge.net/projects/fastica/">FastICA 
 * implementation by Michael Lambertz</a>.</p>
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 */
public class ICA extends ClusteringTechnique implements FocusListener {

    private static int DEF_ICAN = 5; // Default number of components
    
    private int dimensions;
    
    // Should the ICA image be shown after processing?
    private boolean showICA = true;
    
    // Number of independent components
    private int ican = DEF_ICAN;
    
    @Override
    public void process() {

        // Get image dimensions, store ICA dimensions (number of frames).
        int[] dim = ip.getDimensions();
        dimensions = dim[4];
        
        IJ.showStatus("ICA: reformatting data matrix...");

        // Compute number of real voxels to be used voxels
        int n = 0;
        if (!skip_noisy)
            n = dim[0] * dim[1] * dim[3]; // No noise check: use all
        else {
            for (Voxel v : ip) {
                if (isNoise(v)) continue;
                n++;
            }
        }

        // Create new array and fill it with the image data
        double[][] image_data = new double[n][dimensions];

        int i = 0;
        for (Voxel v : ip) {
            if (skip_noisy && isNoise(v)) continue;
            image_data[i++] = v.tac;
        }
        
        // Transpose the data matrix
        Array2DRowRealMatrix temp = new Array2DRowRealMatrix(image_data, 
                                                             false);
        image_data = temp.transpose().getData();
        temp = null;
        System.gc();        
        
        // Perform the ICA computation
        IJ.showStatus("ICA: performing source separation " +
                      "(may take some time)...");
        FastICA fi = null;
        
        try {
            fi = new FastICA(image_data, ican);
        } catch (FastICAException e) {            
            System.out.println(e.getLocalizedMessage());
        }
        
        // Get projections on each dimension
        double [][] vectors = fi.getICVectors();
        
        // Get independent signals (to be offered as additional information)
        // It is transposed to print the columns.
        double [][] sep = fi.getSeparatingMatrix();
        RealMatrix sources = new Array2DRowRealMatrix(sep);
        sources = sources.transpose();
        
        Array2DRowRealMatrix result = new Array2DRowRealMatrix(vectors, false);        
        
        // If the ICA image is to be shown, create a new image with
        // result.getRowDimension() frames and the original number of 
        // x, y, z dimensions
        if (showICA) {            
            ImagePlus ICA_image = RealMatrix2IJ(result, dim, this.ip, 
                    skip_noisy, "ICA image");            
            ICA_image.show();
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
        additionalInfo[0] = "ica_sources";        
        StringBuilder sb = new StringBuilder();
        int rows = sources.getRowDimension();
        for (int j = 0; j < rows; j++) {
            double [] row = sources.getRow(j);
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
        JPanel jp = new JPanel(new GridLayout(2, 2, 5, 5));
        
        jp.add(new JLabel("Show ICA image:"));
        JCheckBox jcb_showPCA = new JCheckBox();
        jcb_showPCA.setSelected(showICA);
        jcb_showPCA.addItemListener(this);
        jp.add(jcb_showPCA);
        
        jp.add(new JLabel("Number of independent components:"));
        JTextField jtf_ican = createJTextField("jtf_ican", ican, this);
        jp.add(jtf_ican);
        
        return jp;        
    }

    public void itemStateChanged(ItemEvent arg0) {
        
        // Check the checkbox for the showICA variable
        JCheckBox jcb = (JCheckBox)arg0.getSource();
        showICA = jcb.isSelected();
        
    }

    @Override
    public void focusGained(FocusEvent arg0) {
        
        Component c = arg0.getComponent();
        ((JTextField) c).selectAll();
        
    }

    @Override
    public void focusLost(FocusEvent arg0) {
        
        Component c = arg0.getComponent();
        String source = c.getName();        
        
        if (source == "jtf_ican") {
            JTextField jtf = (JTextField) c;
            try {                
                ican = Integer.parseInt(jtf.getText());                
            } catch (NumberFormatException e) {
                ican = DEF_ICAN;
                jtf.setText(Integer.toString(ican));
            }
        }        
    }
}
