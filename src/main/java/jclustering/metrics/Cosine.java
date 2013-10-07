package jclustering.metrics;

import org.apache.commons.math3.util.FastMath;

/**
 * Computes the cosine between two given TACs (data type
 * {@code double[]}). The distance is then returned as {@code 1 - cos}, where
 * {@code cos} is the cosine value.
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 */
public class Cosine extends ClusteringMetric {

    @Override
    public double distance(double[] data, double[] centroid) {
        double cos = 0.0;
        
        double dotprod = 0.0;        
        for(int i = 0; i < data.length; i++) {
            dotprod += data[i] * centroid[i];
        }
        
        double normdata = 0.0;
        double normcentroid = 0.0;
        for(int i = 0; i < data.length; i++) {
            normdata += data[i] * data[i];
            normcentroid += centroid[i] * centroid[i];
        }
        
        normdata = FastMath.sqrt(normdata);
        normcentroid = FastMath.sqrt(normcentroid);
        
        cos = dotprod / (normdata * normcentroid);
        
        return 1 - cos;
        
    }

}
