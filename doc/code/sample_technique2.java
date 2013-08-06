package jclustering.techniques;

import java.awt.event.ItemEvent;
import javax.swing.JPanel;
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
    
    @Override
    protected JPanel makeConfig() {
        JPanel jp = new JPanel();
        addMetricsToJPanel(jp);
        return jp;
    }

    @Override
    public void itemStateChanged(ItemEvent arg0) {
        super.itemStateChanged(arg0);
    }
}