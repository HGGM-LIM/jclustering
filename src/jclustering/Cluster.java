package jclustering;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * Implements a cluster class. A cluster is defined by a centroid (a
 * {@code double []} array) and by the mean TAC of the voxels assigned to this
 * cluster. Every other elements from this class serve as helpers for latter
 * calculations.
 */
public class Cluster implements Comparable<Cluster> {

    // Centroid
    private double[] centroid;

    // Mean TAC of voxels attached to this cluster
    private double[] cluster_tac;

    // Need this to store all the peak amplitudes
    private SummaryStatistics peak_stats;

    // Size of the cluster
    private int size;
    
    // Modify centroid
    private boolean modify_centroid;

    /**
     * Public constructor. Does nothing by default, just initializes the
     * internal containers.
     */
    public Cluster() {
        size = 0;
        peak_stats = new SummaryStatistics();
    }

    /**
     * Public constructor with a pre-defined centroid.
     * 
     * @param centroid Centroid vector.
     */
    public Cluster(double[] centroid) {
        
        this(); // Call empty constructor
        this.centroid = centroid;
        this.modify_centroid = false;
        
    }
    
    /**
     * Public constructor with a pre-defined centroid an a boolean option
     * for centroid modification. If {@code modify_centroid} is set to true,
     * then the centroid for this cluster will be modified with each new
     * TAC that is added to this object.
     * @param centroid Centroid vector.
     * @param modify_centroid Should the centroid be modified with each new 
     * TAC.
     */    
    public Cluster(double [] centroid, boolean modify_centroid) {
        
        this();
        this.centroid = centroid;
        this.modify_centroid = modify_centroid;
        
        // If the centroid is to be modified, this method must also
        // start including data into the peak_stats object and increment
        // the size.
        if (modify_centroid) {
            double max = MathUtils.getMax(centroid);
            peak_stats.addValue(max);
            size++;
        }
        
    }

    /**
     * @return The current centroid data.
     */
    public double[] getCentroid() {
        return centroid;
    }

    /**
     * Sets this cluster's centroid.
     * 
     * @param centroid
     *            The new centroid.
     */
    public void setCentroid(double[] centroid) {
        this.centroid = centroid;
    }

    /**
     * @return The current cluster TAC.
     */
    public double[] getClusterTAC() {
        return cluster_tac;
    }

    /**
     * @return The number of pixels inside this cluster.
     */
    public int size() {
        return size;
    }

    /**
     * Adds given data to cluster. It can work in two different ways:
     * <ul>
     * <li>It can modify this cluster's centroid as new voxels are added.
     * <li>The centroid can remain static and the news voxels are added
     * independently, forming a new TAC.
     * </ul>
     * The second way is the standard way of creating a new cluster, where the
     * centroid serves as the reference for the distances. In any case, as there
     * are some approaches in which the centroid is built along the way, the
     * parameter {@code modify_centroid} is offered in the {@code Cluster} 
     * constructor. This method uses the default behavior used at creation
     * time.
     * 
     * @param data Dynamic data to be added
     */
    public void add(double[] data) {

        if (!isEmpty()) {

            // This is not the first voxel.
            double[] temp = new double[data.length];

            // Re-normalize the centroid: multiply it by the number of clusters
            // before this addition and then divide by one plus that amount.

            double[] d = modify_centroid ? centroid : cluster_tac;

            for (int i = 0; i < data.length; i++) {
                temp[i] = (d[i] * size + data[i]) / (double) (size + 1);
            }

            if (modify_centroid)
                centroid = temp;
            else
                cluster_tac = temp;

        } else {

            // This is the first voxel, so let's initialize everything
            // that will be needed later.
//            if (modify_centroid)
//                centroid = data;
//            else
                cluster_tac = data;

        }

        peak_stats.addValue(MathUtils.getMax(data));
        size++;

    }

    /**
     * @return A {@link SummaryStatistics} object with the peak amplitudes data.
     */
    public SummaryStatistics getPeakStats() {

        return peak_stats;

    }
    
    /**
     * @return Mean peak value for all TACs in cluster.
     */
    public double getPeakMean() {
        
        return peak_stats.getMean();
        
    }

    /**
     * @return Standard deviation for peak values for all TACs in cluster.
     */
    public double getPeakStdev() {
        
        if (size == 1) {
            return 0.0;
        } else {
            return peak_stats.getStandardDeviation();            
        }        
        
    }
    
    /**
     * @return true if the given cluster is empty
     */
    public boolean isEmpty() {

        return size == 0;

    }

    /**
     * Implements Comparable interface. Use size and mean peak as elements
     * to compute each score.
     */
    @Override
    public int compareTo(Cluster that) {
        
        double this_score = this.size() * this.getPeakMean();
        double that_score = that.size() * that.getPeakMean();        
        
        if (this_score < that_score) {
            return -1;
        } else if (this_score > that_score) {
            return 1;
        } else {
            return 0;
        }
        
    }

}
