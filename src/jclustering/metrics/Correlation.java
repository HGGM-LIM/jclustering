package jclustering.metrics;

import java.util.Arrays;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/**
 * Classical correlation score between two given TACs (data type
 * {@code double[]}).
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 */
public class Correlation extends ClusteringMetric {

    private PearsonsCorrelation pc = new PearsonsCorrelation();

    @Override
    public double distance(double[] centroid, double[] data) {

        double corr;

        if (Arrays.equals(centroid, data)) {
            // Same contents, do not even try to compute the correlation score
            corr = 0.0;
        } else {
            // Turn a correlation score into a distance
            corr = 1.0 - pc.correlation(centroid, data);
        }

        if (corr != Double.NaN)
            return corr;
        else
            return Double.MAX_VALUE;

    }

}
