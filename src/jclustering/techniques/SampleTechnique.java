package jclustering.techniques;

import java.awt.event.ItemEvent;
import javax.swing.JPanel;

import jclustering.Voxel;

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
                    int n = _getMaxIndex(v.tac) + 1; // +1, min_cluster = 1.

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

    private int _getMaxIndex(double[] d) {

        int res = 0;
        double aux = d[0];

        for (int i = 0; i < d.length; i++) {
            if (d[i] > aux) {
                res = i;
                aux = d[i];
            }
        }

        return res;
    }
}
