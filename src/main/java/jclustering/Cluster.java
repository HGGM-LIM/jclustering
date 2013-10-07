package jclustering;

import java.util.ArrayList;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * Implements a cluster class. A cluster is defined by a centroid (a
 * {@code double []} array) and by the mean TAC of the voxels assigned to this
 * cluster. Every other elements from this class serve as helpers for latter
 * calculations.
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 */
public class Cluster {

    // Centroid
    private double[] centroid;
    
    // Spatial centroid (centroid mean coordinates)
    private double[] centroid_spatial;

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
     * internal containers. A cluster created this way modifies its centroid.
     */
    public Cluster() {
        
        size = 0;
        peak_stats = new SummaryStatistics();
        coordinates = new ArrayList<Integer []>();
        modify_centroid = true;
        centroid_spatial = new double[3];
        
    }

    /**
     * Public constructor with a pre-defined centroid. A cluster initialized
     * this way does not modify its centroid.
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
        double max = StatUtils.max(centroid);
        peak_stats.addValue(max);
        size++;
        
        // Add the coordinates
        coordinates.add(new Integer[] {x, y, slice});
        
        // Spatial centroid
        centroid_spatial[0] = x;
        centroid_spatial[1] = y;
        centroid_spatial[2] = slice;
        
    }
    
    /**
     * Provides a shortcut for the public constructor with parameters using a
     * {@link Voxel} to initialize the parameters. A cluster initialized this
     * way modifies its centroid.
     * @param v The voxel to be used as initial centroid.
     */
    public Cluster(Voxel v) {
        
        this(v.tac, v.x, v.y, v.slice);
        
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
            if (modify_centroid)
                centroid = data;
            else
                cluster_tac = data;
        }

        peak_stats.addValue(StatUtils.max(data));
        size++;
        coordinates.add(new Integer[] {x, y, slice});
        
        // Also update the spatial centroid
        double old_x = centroid_spatial[0] * size;
        centroid_spatial[0] = (old_x + x) / (size + 1);
        double old_y = centroid_spatial[1] * size;
        centroid_spatial[1] = (old_y + y) / (size + 1);
        double old_slice = centroid_spatial[2] * size;
        centroid_spatial[2] = (old_slice + slice) / (size + 1);

    }
    
    /**
     * Provides a shortcut to the {@link Cluster#add(double[], int, int, int)}
     * method using a {@link Voxel}.
     * @param v The Voxel to be added.
     */
    public void add(Voxel v) {
        add(v.tac, v.x, v.y, v.slice);
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
        
        long n = peak_stats.getN();
        if (n == 1) return getPeakMean();
        else        return peak_stats.getStandardDeviation();                        
        
    }
    
    /**
     * @return true if the given cluster is empty
     */
    public boolean isEmpty() {

        return size == 0;

    }
    
    /**
     * @return The spatial centroid.
     */
    public double[] getSpatialCentroid() {
        
        return centroid_spatial;
        
    }

}
