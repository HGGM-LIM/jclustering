package jclustering.techniques;

import java.awt.event.ItemEvent;
import javax.swing.JPanel;

import jclustering.Voxel;
import static jclustering.MathUtils.getMaxIndex;

/**
 * @author jmmateos
 * 
 */
public class SampleTechnique extends ClusteringTechnique {

    @Override
    public void process() {

        for (Voxel v : ip) {                    

                    // If is noise, skip
                    if (skip_noisy && isNoise(v))
                        continue;

                    // Else, set the corresponding result
                    int n = getMaxIndex(v.tac) + 1; // +1, min_cluster = 1.

                    // Set temporal result
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
