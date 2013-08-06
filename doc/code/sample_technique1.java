package jclustering.techniques;

import jclustering.Voxel;
import static jclustering.MathUtils.getMaxIndex;

public class SampleTechnique extends ClusteringTechnique {

    @Override
    public void process() {
        for (Voxel v : ip) {                    
            // Find the maximum index
            // +1, min_cluster = 1.
            int n = getMaxIndex(v.tac) + 1; 
            // Add this voxel to its cluster
            addTACtoCluster(v, n);
        }
    }
}