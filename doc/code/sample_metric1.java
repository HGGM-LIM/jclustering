package jclustering.metrics;
import java.util.Arrays;
import org.apache.commons.math3.stat.
        correlation.PearsonsCorrelation;

public class Correlation extends ClusteringMetric {

    private PearsonsCorrelation pc;

    @Override
    public double distance(double[] centroid, double[] data) 
    {

        double corr;

        if (Arrays.equals(centroid, data)) {
            // Same contents, do not compute the correlation
            corr = 0.0;
        } else {
            // Turn a correlation score into a distance
            corr = 1.0 - pc.correlation(centroid, data);
        }

        if (!Double.isNaN(corr)) return corr;
        else                     return Double.MAX_VALUE;

    }
    
    @Override
    public void init() {
        pc = new PearsonsCorrelation();
    }

}
