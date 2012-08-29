package jclustering.techniques;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import jclustering.Cluster;
import jclustering.MathUtils;
import static jclustering.Utils.*;
import static jclustering.GUIUtils.*;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

/**
 * Implements a leader-follower clustering method using only correlation
 * as its main metric. Also uses the mean peak value of all the TACs inside
 * a Cluster to decide whether a certain TAC belongs or not to a voxel
 * with similar dynamic shape. 
 * <p>
 * Leader-follower works in an inverse way as K-means does. The number of
 * clusters is unknown, and a threshold is set so that clusters are formed
 * with TACs with a distance (correlation, in this case) that evaluates
 * above said threshold. An optional increment value can be set so that
 * every time a voxel is added to a cluster this becomes more restrictive.
 * <p>
 * Also, peak amplitude is taken into account when adding new TACs to an 
 * existing cluster.
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 */
public class LeaderFollower extends ClusteringTechnique 
    implements FocusListener {

    // Default values
    private final int DEF_MAX_CLUSTERS = 1000;
    private final int DEF_KEEP_CLUSTERS = 50;
    private final double DEF_THRESHOLD = 0.3;
    private final double DEF_T_INC = 1.00005;
    
    // Maximum number of clusters to compute
    private int max_clusters = DEF_MAX_CLUSTERS;
    
    // Maximum number of clusters to keep
    private int keep_clusters = DEF_KEEP_CLUSTERS;
    
    // Threshold for cluster addition
    private double threshold = DEF_THRESHOLD;
    
    // Threshold increment
    private double t_inc = DEF_T_INC;
    private Hashtable<Cluster, Double> corr_limits;
    
    // Dimensions
    private int [] dim;
    
    // Pearson Correlation object
    private PearsonsCorrelation pc;
    
    // Need to keep track of which coordinates belong to which cluster
    private Hashtable<Cluster, ArrayList<Integer[]>> record;            
    
    @Override
    public ImagePlus process() {
        
        /*
         * Initial object creation.
         */
        
        dim = ip.getDimensions();
        ImagePlus res = IJ.createImage("Clusters", "16-bit", dim[0], dim[1],
                dim[3]);
        ImageStack is = res.getStack();
        
        pc = new PearsonsCorrelation();
        
        record = new Hashtable<Cluster, ArrayList<Integer[]>>();
        corr_limits = new Hashtable<Cluster, Double>();
        
        IJ.log(String.format("Leader-follower clustering started.\n" +
        		"Correlation limit: %f; increment: %f.",
        		threshold, t_inc));
        
        /*
         * Process all TACs               
         */
        
        for (int slice = 1; slice <= dim[3]; slice++) {
            
            // Show status message for every slice
            String status = String.format("Leader-follower. Slice %d, " +
            		"%d/%d clusters", slice, clusters.size(), max_clusters);
            IJ.showStatus(status);            
            
            for (int x = 0; x < dim[0]; x++) {
                for (int y = 0; y < dim[1]; y++) {
                    
                    // Get TAC
                    double [] tac = ip.getTAC(x, y, slice);
                    
                    // If is noise, skip
                    if (skip_noisy && isNoise(tac))
                        continue;
                                      
                    // Is it the first voxel? If so, just put it in a 
                    // new cluster
                    if (clusters.isEmpty()) {
                        Cluster c = new Cluster(tac, true);
                        clusters.add(c);
                        _addToRecord(c, x, y, slice);
                    }
                    // Are we within the max_clusters limit?
                    else if (clusters.size() < max_clusters) {
                        
                        // Get closest cluster
                        int cindex = _getClosestCluster(tac);
                        
                        if (cindex >= 0) {
                            // There is a cluster that can include this voxel
                            Cluster c = clusters.get(cindex);
                            // Add TAC modifying centroid
                            c.add(tac);
                            _addToRecord(c, x, y, slice);
                        } else {                            
                            // There is no cluster to include this voxel yet
                            Cluster c = new Cluster(tac, true);
                            clusters.add(c);
                            _addToRecord(c, x, y, slice);
                        }                        
                    }
                }
            }
        }
        
        /*
         * Get rid of the smallest clusters
         */
        
        // Sort clusters
        Collections.sort(clusters);
        
        // Build new list for inserting the selected clusters
        ArrayList<Cluster> good_clusters = new ArrayList<Cluster>();
        
        // Get last "keep_clusters" and set the corresponding voxel in the
        // ImageStack is
        int size = clusters.size();
        
        // Can't keep more clusters that the ones that have been created
        if (keep_clusters > size)
            keep_clusters = size;
        
        int cluster_index = 2;
        // Go backwards: biggest cluster is #1
        for (int i = size - 1; i >= size - keep_clusters; i--) {            
            Cluster c = clusters.get(i);
            good_clusters.add(c);
            
            // Get all coordinates
            ArrayList<Integer []> coords = record.get(c);
            // Set all coordinates to the cluster value
            for (Integer [] coord : coords) {
                setVoxel(is, coord[0], coord[1], coord[2], cluster_index);
            }
            
            // Increment cluster_index
            cluster_index++;            
        }
        
        // Change reference
        clusters = good_clusters;
        
        /*
         * Log result and return 
         */
                
        int nsize = clusters.size();
        IJ.log(String.format("Leader-follower finished. %d clusters " +
        		"created, %d kept.", size, nsize));
        
        // Expand image and return
        return expand(res, nsize);
    }
    
    public JPanel makeConfig() {
        
        JPanel jp = new JPanel(new GridLayout(4, 2, 5, 5));
        
        // Maximum number of clusters
        jp.add(new JLabel("Maximum clusters to form:"));
        JTextField jt_maxclust = createJTextField("jt_maxclust", max_clusters, 
                                 this);
        jp.add(jt_maxclust);
        
        // Number of clusters to keep
        String keep_clusters_help = "<html>If more than these clusters " +
        		"are formed, the cluster list will be ordered (biggest " +
        		"clusters first) and the ones exceeding this variable " +
        		"will be discarded.</html>";
        jp.add(createJLabel("Clusters to keep:*", keep_clusters_help));
        JTextField jt_keepclust = createJTextField("jt_keepclust", 
                                  keep_clusters, this);
        jp.add(jt_keepclust);
        
        // Initial correlation threshold
        jp.add(new JLabel("Initial correlation threshold:"));
        JTextField jt_thres = createJTextField("jt_thres", threshold, this);
        jp.add(jt_thres);
        
        // Correlation increment
        jp.add(new JLabel("Correlation increment:"));
        JTextField jt_inc = createJTextField("jt_inc", t_inc, this);
        jp.add(jt_inc);
        
        return jp;
        
    }
    
    /*
     * Get closest cluster to provided TAC.
     */
    private int _getClosestCluster(double [] tac) {
        
        int i = -1;
        double max_score = -Double.MAX_VALUE;
        int size = clusters.size();
        
        for (int j = 0; j < size; j++) {
            
            Cluster c = clusters.get(j);
            
            // Smooth the TAC only for correlation computing purposes, do
            // not use it afterwards.
            double score = pc.correlation(MathUtils.smooth(tac), 
                    c.getCentroid());
            
            // Get correlation value for that cluster
            Double d = corr_limits.get(c);
            double threshold = d != null ? d : this.threshold;
            
            // Compute peak value for this TAC and obtain peak threshold
            // for current cluster.
            double peak = MathUtils.getMax(tac);
            double peak_threshold = c.getPeakMean() - c.getPeakStdev();
            
            if (score > threshold && score > max_score && 
                    peak >= peak_threshold) {                
                max_score = score;
                i = j;                
            }            
        }        
        
        return i;
        
    }
    
    /*
     * Keeps track of which voxels have been added to which cluster and
     * increments the correlation limit.
     */
    private void _addToRecord(Cluster c, int x, int y, int slice) {
        
        Integer [] coordinates = new Integer[] {x, y, slice};
        ArrayList<Integer []> ints = record.get(c);
        
        // Use Double instead of double so the comparison cor != null
        // can be made.
        Double cor = corr_limits.get(c);
        // Compute updated score or use original threshold if no prior
        // score is found.
        double score = (cor != null) ? cor * t_inc : threshold;        
        
        // If the cluster has not been yet added, create it and add to record
        if (ints == null) {
            ints = new ArrayList<Integer[]>();
            record.put(c, ints);            
        }
        
        // Update the score and add the coordinates to record.
        corr_limits.put(c, score);
        ints.add(coordinates);
        
    }

    @Override
    public void focusGained(FocusEvent arg0) {
        
        Component c = arg0.getComponent();
        String s = c.getName();
        
        // Just select all the text in the JTextField, for usability.
        if (s.equals("jt_maxclust") || s.equals("jt_keepclust")
                || s.equals("jt_thres") || s.equals("jt_inc")) {
            ((JTextField)c).selectAll();
        }
        
    }

    @Override
    public void focusLost(FocusEvent arg0) {

        Component c = arg0.getComponent();
        String s = c.getName();
        
        // Just set each value to the corresponding JTextField
        if (s.equals("jt_maxclust")) {
            try {                
                max_clusters = Integer.parseInt(((JTextField)c).getText());
            } catch (NumberFormatException e) {
                max_clusters = DEF_MAX_CLUSTERS;                
            }
            ((JTextField)c).setText(Integer.toString(max_clusters));  
            
        } else if (s.equals("jt_keepclust")) {
            try {                
                keep_clusters = Integer.parseInt(((JTextField)c).getText());
            } catch (NumberFormatException e) {
                keep_clusters = DEF_KEEP_CLUSTERS;                
            }
            ((JTextField)c).setText(Integer.toString(keep_clusters));
            
        } else if (s.equals("jt_thres")) {
            try {                
                threshold = Double.parseDouble(((JTextField)c).getText());
            } catch (NumberFormatException e) {
                threshold = DEF_THRESHOLD;                
            }
            ((JTextField)c).setText(Double.toString(threshold));
            
        } else if (s.equals("jt_inc")) {
            try {                
                t_inc = Double.parseDouble(((JTextField)c).getText());
            } catch (NumberFormatException e) {
                t_inc = DEF_T_INC;                
            }
            ((JTextField)c).setText(Double.toString(t_inc));
        }        
    }

}
