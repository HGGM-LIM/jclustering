package jclustering.techniques;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.math3.util.FastMath;

import jclustering.Cluster;
import jclustering.MathUtils;
import jclustering.Voxel;
import static jclustering.GUIUtils.*;
import static jclustering.Utils.getClusteringMetric;

import ij.IJ;

/**
 * Implements a leader-follower clustering method using only correlation
 * as its main metric. It first sorts the image voxels to start analyzing
 * first those with earliest and highest peak values.  
 * <p>
 * Leader-follower works in an inverse way as K-means does. The number of
 * clusters is unknown, and a threshold is set so that clusters are formed
 * with TACs with a distance (correlation, in this case) that evaluates
 * above said threshold. An optional increment value can be set so that
 * every time a voxel is added to a cluster this becomes more restrictive. This
 * option must be used with care, as our tests have shown it to be quite
 * unstable.
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
    private final double DEF_THRESHOLD = 0.4;
    
    // Constants
    private final int CLUSTER_NOT_FOUND = -1;
    
    // Maximum number of clusters to compute
    private int max_clusters = DEF_MAX_CLUSTERS;
    
    // Maximum number of clusters to keep
    private int keep_clusters = DEF_KEEP_CLUSTERS;
    
    // Threshold for cluster addition
    private double threshold = DEF_THRESHOLD;  
    
    @Override
    public void process() {
        
        IJ.log(String.format("Correlation limit: %f. Metric: %s.", 
                threshold, metric.getName()));
        
        // Add all voxels to an array to order them by amplitude in the 
        // next step.
        IJ.showStatus("Leader-Follower. Building voxel array...");
        ArrayList<int []> ordered_voxels = new ArrayList<int []>();
        for (Voxel v : ip) {
            int [] coord = new int[] {v.x, v.y, v.slice};
            ordered_voxels.add(coord);
        }
        
        // Order voxels by peak amplitude.
        IJ.showStatus("Leader-Follower. Sorting voxels...");         
        Collections.sort(ordered_voxels, new AmplitudeComparator());        
                
        // Process all TACs.                       
        int vsize = ordered_voxels.size();
        int vprocessed = 0;
        IJ.showStatus("Leader-Follower. Building clusters...");
        
        for (int [] coord : ordered_voxels) {
            
            // Info update
            vprocessed++;      
            if((vprocessed % 5000) == 0) {
                String s = String.format("Leader-Follower. Processed " +
                                         "%d of %d voxels. %d clusters created", 
                                         vprocessed, vsize, clusters.size());
                IJ.showStatus(s);
            }
            
            Voxel v = new Voxel(coord[0], coord[1], coord[2], 
                                ip.getTAC(coord[0], coord[1], coord[2]));

            int size = clusters.size();

            // Is it the first voxel? If so, just put it in a
            // new cluster
            if (clusters.isEmpty()) {
                Cluster c = new Cluster(v);
                clusters.add(c);
            }
            // Else, let's include new voxels into their corresponding clusters
            // or create new ones if there still space
            else {
                // Get closest cluster
                int cindex = _getClosestCluster(v.tac);

                if (cindex >= 0) { // There is a cluster that can 
                                   // include this voxel                    
                    Cluster c = clusters.get(cindex);
                    // Add TAC modifying centroid
                    c.add(v);
                } else if (cindex == CLUSTER_NOT_FOUND) { 
                    // Create a new cluster if there is room for it.
                    if (size < max_clusters) {
                        Cluster c = new Cluster(v);
                        clusters.add(c);
                    }
                }
            }
        }

        // Sort clusters
        Collections.sort(clusters, new LeaderFollowerClusterComparator());
        
        // Build new list for inserting the selected clusters
        ArrayList<Cluster> good_clusters = new ArrayList<Cluster>();
        
        // Get last "keep_clusters" and set the corresponding voxel in the
        // ImageStack is
        int size = clusters.size();
        
        // Can't keep more clusters that the ones that have been created
        int kc;
        if (keep_clusters > size) kc = size;
        else kc = keep_clusters;
        
        // Go backwards: biggest cluster is #1
        for (int i = size - 1; i >= size - kc; i--) {            
            Cluster c = clusters.get(i);
            good_clusters.add(c);
        }
        
        // Change reference
        clusters = good_clusters;
        
        // Log result and return        
        int nsize = clusters.size();
        IJ.log(String.format("Leader-follower finished. %d clusters " +
        		"created, %d kept.", size, nsize));
    }
    
    public JPanel makeConfig() {
        
        JPanel jp = new JPanel(new GridLayout(4, 2, 5, 5));
        
        // Available metrics for this technique: 
        // * PearsonsCorrelation
        // * SpearmansCorrelation
        // * Cosine
        jp.add(new JLabel("Metric to use:"));
        String [] available_metrics = new String[]{"Cosine", 
                                                   "PearsonsCorrelation",
                                                   "SpearmansCorrelation"};
        JComboBox metrics = createChoices("metrics", available_metrics, this);
        this.setMetric(getClusteringMetric((String)metrics.getItemAt(0), ip));
        jp.add(metrics);
        
        // Maximum number of clusters
        jp.add(new JLabel("Maximum clusters to form:"));
        JTextField jt_maxclust = createJTextField("jt_maxclust", max_clusters, 
                                 this);
        jp.add(jt_maxclust);
        
        // Number of clusters to keep
        String keep_clusters_help = "<html>If more than these clusters<br>" +
        		"are formed, the cluster list will be ordered (biggest<br>" +
        		"clusters first) and the ones exceeding this variable<br>" +
        		"will be discarded.</html>";
        jp.add(createJLabel("Clusters to keep:*", keep_clusters_help));
        JTextField jt_keepclust = createJTextField("jt_keepclust", 
                                  keep_clusters, this);
        jp.add(jt_keepclust);
        
        // Initial threshold (not only correlation).
        jp.add(new JLabel("Initial threshold:"));
        JTextField jt_thres = createJTextField("jt_thres", threshold, this);
        jp.add(jt_thres);

        return jp;
        
    }
    
    /*
     * Get closest cluster to provided TAC. Returns:
     * * CLUSTER_NOT_FOUND if no cluster with enough correlation has been found.
     * * The cluster index if one is found.     
     */
    private int _getClosestCluster(double [] tac) {
                
        ArrayList<Integer> selected = new ArrayList<Integer>();        
        int size = clusters.size();
        
        // Find the cluster with the highest correlation with this TAC        
        for (int j = 0; j < size; j++) {            
            Cluster c = clusters.get(j);            
            // Smooth the TAC only for correlation computing purposes, do
            // not use it afterwards.
            // As the metrics that this technique may use return the
            // correlation / cosine values as a metric (1 - x), that
            // change needs to be undone because the actual value is needed
            // here. That explains the 1 - metric.distance() in the next line.
            double score = 1 - metric.distance(MathUtils.smooth(tac), 
                                          MathUtils.smooth(c.getCentroid()));            
            if (score > threshold) {
                selected.add(j);                
            }
        }
        
        // No cluster has been selected
        if (selected.isEmpty())
            return CLUSTER_NOT_FOUND;
        // Only one cluster has been selected
        else if (selected.size() == 1)
            return selected.get(0);
        // Several clusters have been selected. Get the closest one according
        // to the Euclidean distance.
        else { 
            double min_dist = Double.MAX_VALUE;
            int i = 0;
            for (int j : selected) {
                Cluster c = clusters.get(j);
                double euc = _euclidean(tac, c.getCentroid());
                if (euc < min_dist) {
                    min_dist = euc;
                    i = j;
                }
            }
            return i;            
        }        
    }
    
    /*
     * Computes the Euclidean distance between two given TACs.
     */
    private double _euclidean(double [] tac1, double [] tac2) {
        double distance = 0.0;
        for (int i = 0; i < tac1.length; i++) 
            distance += FastMath.pow(tac1[i] - tac2[i], 2);
        return FastMath.sqrt(distance);
    }

    @Override
    public void focusGained(FocusEvent arg0) {
        
        Component c = arg0.getComponent();
        String s = c.getName();
        
        // Just select all the text in the JTextField, for usability.
        if (s.equals("jt_maxclust") || s.equals("jt_keepclust")
                || s.equals("jt_thres")) {
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
        }        
    }
    
    /*
     * Comparator used to order clusters by size.
     */
    private class LeaderFollowerClusterComparator 
        implements Comparator<Cluster> {

        @Override
        public int compare(Cluster arg0, Cluster arg1) {
            double score0 = arg0.size();
            double score1 = arg1.size();
            if (score0 < score1) return -1;
            else if (score0 > score1) return 1;
            else return 0;
        }
    }
    
    private class AmplitudeComparator implements Comparator<int[]> {

        @Override
        public int compare(int[] arg0, int[] arg1) {
            // Get TACs
            double [] tac0 = ip.getTAC(arg0[0], arg0[1], arg0[2]);
            double [] tac1 = ip.getTAC(arg1[0], arg1[1], arg1[2]);
            // Get peak values
            double [] info0 = _getMaxInfo(tac0);
            double [] info1 = _getMaxInfo(tac1);
            // Return value according to the peak time
            if (info0[0] < info1[0])          return -1;
            else if (info0[0] > info1[0])     return 1;
            else { // Same peak time, test for amplitude
                if (info0[1] < info1[1])      return 1;
                else if (info0[1] > info1[1]) return -1;                
            }            
            return 0;            
        }
        
        /**
         * Returns the index for the maximum value ([0]) and the
         * maximum value ([1]) in a 2 position array.
         * @param data The TAC to inspect.
         * @return A double[] with 2 positions.
         */
        private double [] _getMaxInfo(double [] data) {
            int max_index = 0;
            double max = -Double.MAX_VALUE;
            double [] res = new double[2];
            for (int i = 0; i < data.length; i++) {
                if (data[i] > max) {
                    max = data[i];
                    max_index = i;
                }
            }
            res[0] = max_index;
            res[1] = max;
            return res;            
        }        
    }

}
