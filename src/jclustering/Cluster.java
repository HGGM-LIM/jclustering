package jclustering;

import java.util.ArrayList;

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
    
    // Keep track of added coordinates
    private ArrayList<Integer []> coordinates;

    /**
     * Public constructor. Does nothing by default, just initializes the
     * internal containers.
     */
    public Cluster() {
        
        size = 0;
        peak_stats = new SummaryStatistics();
        coordinates = new ArrayList<Integer []>();
        
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
     * Public constructor with a pre-defined centroid and coordinates for it. 
     * If this method is used as a constructor, it is assumed that the the 
     * centroid for this cluster will be modified with each new TAC that is 
     * added to this object.
     * 
     * @param centroid Centroid vector.
     * @param x X-coordinate for the centroid TAC.
     * @param y Y-coordinate for the centroid TAC.
     * @param slice Slice (1-based) for the centroid TAC. TAC.
     */    
    public Cluster(double [] centroid, int x, int y, int slice) {
        
        this();
        this.centroid = centroid;
        this.modify_centroid = true;
        
        // If the centroid is to be modified, this method must also
        // start including data into the peak_stats object and increment
        // the size.
        double max = MathUtils.getMax(centroid);
        peak_stats.addValue(max);
        size++;
        
        // And add the coordinates
        coordinates.add(new Integer[] {x, y, slice});
        
    }
    
    /**
     * @return The coordinates of pixels added to this cluster
     */
    public ArrayList<Integer []> getCoordinates() {
        return coordinates;
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
     * @param centroid The new centroid.
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
     * {@code Cluster} constructor will cause this cluster to modify its 
     * centroid if the coordinates for the first voxel are provided. 
     * This method always uses the default behavior used at creation
     * time.
     * 
     * @param data Dynamic data to be added
     * @param x X-coordinate for added TAC.
     * @param y Y-coordinate for added TAC.
     * @param slice Slice (1-based) for added TAC. 
     */
    public void add(double[] data, int x, int y, int slice) {

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
            cluster_tac = data;
        }

        peak_stats.addValue(MathUtils.getMax(data));
        size++;
        coordinates.add(new Integer[] {x, y, slice});

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
        
        double this_score = score();
        double that_score = that.score();        
        
        if (this_score < that_score) {
            return -1;
        } else if (this_score > that_score) {
            return 1;
        } else {
            return 0;
        }
        
    }
    
    /**
     * @return A score for comparing clusters. Currently, size() * getPeakMean()
     * is used.
     */
    public double score() {
        return size() * getPeakMean();
    }

}
