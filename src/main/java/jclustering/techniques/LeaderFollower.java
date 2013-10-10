package jclustering.techniques;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import jclustering.Cluster;
import jclustering.MathUtils;
import jclustering.Voxel;
import static jclustering.GUIUtils.*;

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
    private final boolean DEF_DISCARD_SMALLEST = false;
    private final int DEF_KEEP_CLUSTERS = 50;
    private final double DEF_THRESHOLD = 0.4;
    private final double DEF_T_INC = 1.0;
    
    // Constants
    private final int CLUSTER_NOT_FOUND = -1;
    
    // Maximum number of clusters to compute
    private int max_clusters = DEF_MAX_CLUSTERS;
    
    // Discard smallest cluster when max_clusters is reached
    private boolean discard_smallest = DEF_DISCARD_SMALLEST;
    
    // Maximum number of clusters to keep
    private int keep_clusters = DEF_KEEP_CLUSTERS;
    
    // Threshold for cluster addition
    private double threshold = DEF_THRESHOLD;
    
    // Threshold increment
    private double t_inc = DEF_T_INC;
    private Hashtable<Cluster, Double> corr_limits;

    // Pearson Correlation object
    private PearsonsCorrelation pc;     
    
    // Cluster comparator
    private Comparator<Cluster> comp;
    
    @Override
    public void process() {
                
        //Initial object creation.                                
        pc = new PearsonsCorrelation();                
        corr_limits = new Hashtable<Cluster, Double>();        
        comp = new LeaderFollowerClusterComparator();
        
        IJ.log(String.format("Correlation limit: %f; increment: %f.",
        		threshold, t_inc));
        
        // Add all voxels to an array to order them by amplitude in the 
        // next step.
        IJ.showStatus("Leader-Follower. Building voxel array...");
        ArrayList<int []> ordered_voxels = new ArrayList<int []>();
        for (Voxel v : ip) {
            int [] coord = new int[] {v.x, v.y, v.slice};
            ordered_voxels.add(coord);
        }
        
        // Order voxels by peak amplitude.
        IJ.showStatus("Leader-Follower. Ordering voxels...");         
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
                _incrementCorrLimit(c);
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
                    _incrementCorrLimit(c);
                } else if (cindex == CLUSTER_NOT_FOUND) { 
                    // Need to create a new voxel, if there is room for it
                    // If too many clusters, throw away the smallest of
                    // them, if allowed by the settings.
                    if (size == max_clusters && discard_smallest) {
                        _discardSmallest();
                        Cluster c = new Cluster(v);
                        clusters.add(c);
                        _incrementCorrLimit(c);
                    } else if (size < max_clusters) {
                        Cluster c = new Cluster(v);
                        clusters.add(c);
                        _incrementCorrLimit(c);
                    }
                }
            }
        }
                
        
        /*
         * Get rid of the smallest clusters
         */
        
        // Sort clusters
        Collections.sort(clusters, comp);
        
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
        
        JPanel jp = new JPanel(new GridLayout(5, 2, 5, 5));
        
        // Maximum number of clusters
        jp.add(new JLabel("Maximum clusters to form:"));
        JTextField jt_maxclust = createJTextField("jt_maxclust", max_clusters, 
                                 this);
        jp.add(jt_maxclust);
        
        // Discard smallest cluster
        String discard_help = "<html>If a new cluster needs to be created<br>" +
        		"but the maximum permitted number has been reached,<br>" +
        		"discard the smallest one (the one containing less voxels<br>" +
        		"with lowest amplitudes).</html>";
        jp.add(createJLabel("Discard smallest cluster:*", discard_help));
        JCheckBox jcb_discard = new JCheckBox();
        jcb_discard.setSelected(discard_smallest);
        jcb_discard.setName("jcb_discard");
        jcb_discard.addItemListener(this);
        jp.add(jcb_discard);
        
        // Number of clusters to keep
        String keep_clusters_help = "<html>If more than these clusters<br>" +
        		"are formed, the cluster list will be ordered (biggest<br>" +
        		"clusters first) and the ones exceeding this variable<br>" +
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
     * Get closest cluster to provided TAC. Returns:
     * * CLUSTER_NOT_FOUND if no cluster with enough correlation has been found.
     * * The cluster index if one is found.     
     */
    private int _getClosestCluster(double [] tac) {
        
        int i = CLUSTER_NOT_FOUND;
        double max_score = this.threshold;
        int size = clusters.size();
        
        // Find the cluster with the highest correlation with this TAC        
        for (int j = 0; j < size; j++) {            
            Cluster c = clusters.get(j);            
            // Smooth the TAC only for correlation computing purposes, do
            // not use it afterwards.
            double score = pc.correlation(MathUtils.smooth(tac), 
                                          MathUtils.smooth(c.getCentroid()));
            if (score > max_score && score > corr_limits.get(c)) {
                i = j;
                max_score = score;
            }
        }

        return i;        
    }
    
    /*
     * Increments correlation limit for the given voxel
     */
    private void _incrementCorrLimit(Cluster c) {
                
        Double cor = corr_limits.get(c);
        double score = (cor != null) ? cor * t_inc : threshold;
        corr_limits.put(c, score);
        
    }
    
    /*
     * Discard smallest cluster. Uses the private Comparator.
     */    
    private void _discardSmallest() {

        Cluster min = Collections.min(clusters, comp);       
        clusters.remove(min);
        corr_limits.remove(min);
        
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
    
    
    public void itemStateChanged(ItemEvent arg0) {
                        
        // Check the checkbox for discard_smallest
        JCheckBox jcb = (JCheckBox)arg0.getSource();
        discard_smallest = jcb.isSelected();
        
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
                if (info0[1] < info1[1])      return -1;
                else if (info0[1] > info1[1]) return 1;                
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
