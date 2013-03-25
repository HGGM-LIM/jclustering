package jclustering.metrics;

import java.util.Arrays;
import static jclustering.MathUtils.rmsd;

/**
 * Root-mean-square deviation between two given TACs (data type
 * {@code double[]}).
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 */
public class RMSD extends ClusteringMetric {

    @Override
    public double distance(double[] centroid, double[] data) {

        double rmsd;

        if (Arrays.equals(centroid, data)) {
            // Same contents, do not even try to compute the error
            rmsd = 0.0;
        } else {
            // Turn an error into a distance
            rmsd = rmsd(centroid, data);
        }

        if (!Double.isNaN(rmsd))
            return rmsd;
        else
            return Double.MAX_VALUE;

    }

}
