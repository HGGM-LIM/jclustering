package jclustering.metrics;

import java.util.Arrays;

/**
 * Spearmans correlation score between two given TACs (data type
 * {@code double[]}).
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 */
public class SpearmansCorrelation extends ClusteringMetric {

    private org.apache.commons.math3.stat.correlation.SpearmansCorrelation sc;

    @Override
    public double distance(double[] centroid, double[] data) {

        double corr;

        if (Arrays.equals(centroid, data)) {
            // Same contents, do not even try to compute the correlation score
            corr = 0.0;
        } else {
            // Turn a correlation score into a distance
            corr = 1.0 - sc.correlation(centroid, data);
        }

        if (!Double.isNaN(corr))
            return corr;
        else
            return Double.MAX_VALUE;

    }
    
    @Override
    public void init() {
        sc = new 
             org.apache.commons.math3.stat.correlation.SpearmansCorrelation();
    }

}
