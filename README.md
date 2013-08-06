# Introduction

jClustering is a dynamic imaging clustering framework for ImageJ. It is not
only a development API for automatic segmentation algorithms, but also a 
platform aimed at centralizing different implementations that as today
are not available in either source code or binary download.

# Contact

If you wish to develop your own techniques/metrics for jClustering and need
help, or you wish to contribute your own code to this project, please do not
hesitate to contact me at jmmateos@mce.hggm.es.

# What is jClustering?

jClustering is an [ImageJ](http://rsbweb.nih.gov/ij/) plugin developed with the
purpose of becoming a general framework for dynamic imaging clustering, such as
dynamic PET segmentation. It consists of grouping together voxels with similar
time-activity curves (TACs). jClustering is written in Java and provides a very
simple API interface that will allow the implementation of new clustering
algorithms in a short time. The fact that it is implemented on top of ImageJ
gives it all the advantages of using an open-source imaging analysis platform.

# Using jClustering

Please refer to the [user
manual](https://github.com/HGGM-LIM/jclustering/blob/master/doc/user_manual.pdf?raw=true)
for download, installation and usage instructions.

# Developing for jClustering

## How to compile jClustering_.jar

Use your IDE of choice and import it as a Maven project. For example, in Eclipse
click _File>Import>Existing Maven Project..._ and select the _jclustering/_
directory. Eclipse will automatically retrieve the correct commons-math and
ImageJ versions, build the project, and put the resulting jClustering\_.jar
file into the _target/_ directory.

## How to develop a new clustering technique

Developing a new `ClusteringTechnique` is extremely easy. For starters, we need
to create a new class inside the `jclustering.techniques` package that extends
the `ClusteringTechnique` class:

```java
package jclustering.techniques;

public class ExampleTechnique extends ClusteringTechnique {
}
```

Now, the appropriate methods from the superclass have to be implemented.
Currently, there is only one method that must be implemented:

```java
public void process() {
}
```

This method should do two things. Set an internal `clusters` object of the type
`ArrayList<Cluster>` that contains the formed cluster TACs. Developers should
read the documentation for the `Cluster` object to know how to correctly
add new voxels to it or refer to the examples provided below these lines.

As an example, please consider this `process()` method (and the private
function below it):

```java
public void process() {

    // Return a stack with the value of the frame of maximum intensity
    // for each voxel
    int[] dim = ip.getDimensions();

    for (int slice = 1; slice <= dim[3]; slice++) {
        for (int x = 0; x < dim[0]; x++) {
            for (int y = 0; y < dim[1]; y++) {

                // Get TAC
                double[] tac = ip.getTAC(x, y, slice);

                // If is noise, skip
                if (skip_noisy && isNoise(tac))
                    continue;

                // Else, set the corresponding result
                int n = getMaxIndex(tac) + 1; // +1, min_cluster = 1.

                // Set temporal result
                addTACtoCluster(tac, x, y, slice, n);
            }
        }
    }
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
```

The method `getMaxIndex(double [] d)` simply returns the index for the
maximum value of the array. The presented implementation for `process()` simply
clusters the TACs by looking at their peak times.

The first lines of the method just gest the image dimensions so the for loops
can be built.

The three nested for loops iterate through all the TACs of the image. The
`slice` variable is 1-based in ImageJ, so it has to go from `1` to `dim[3]`.

The TAC is easily accessed using the public method `getTAC(int x, int y, int
slice)` from the `ImagePlusHyp` object. If the obtained TAC is noise according
to the `isNoise(double [] tac)` method, it is skipped and the next one is
obtained.  This step improves algorithm performance by not analyzing noisy
voxels. This option can be disabled on the first tab of the GUI.

The method `addTACtoCluster(double [] tac, int x, int y, int slice Cluster c)`
(from the `ClusteringTechnique` superclass) provides an easy way to set the
final cluster values to the resulting cluster.  The user that wishes to
implement its own technique can use this method if desired, but this is in
no way mandatory, as long as the results are set in some way. Sometimes it can
be easier to access directly the `Cluster` methods of the `ArrayList<Cluster>
clusters` object.

As `ImagePlusHyp` implements the `Iterable` interface, the three nested `for`
loops can be avoided. The previous code would have the following aspect:

```java
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
```

The `Voxel` object is simply a data transfer object with `public final` 
references to the `x`, `y` and `slice` coordinates and the `tac` array. Most
(or possibly all) the necessary methods for voxel addition work with `Voxel`
objects.  Please refer to the full API for more information.

## How to develop a new metric

New metric development is very similar to the previous step. All that is needed
is a class inside the `jclustering.metrics` package that extends
`ClusteringMetric` and implements a `distance(double [] tac1, double [] tac2)`
method. Consider the following code:

```java
public class TestMetric extends ClusteringMetric {

    public double distance(double[] tac1, double[] tac2) {

        double result = 0.0;

        for (int i = 0; i < tac1.length; i++) {
            double d = tac1[i] – tac2[i];
            result += d * d;
        }

        return Math.sqrt(result);
    }
}
```

That code above creates a new `TestMetric` object that computes the Euclidean
distance between two given TACs.

Metrics are thought a as way for a clustering technique to use different
distances within the same algorithm. It is not necessary for a clustering
technique to use any or all the available metrics. If a new metric is
implemented, all the clustering techniques that allow metrics usage will have
it at their disposal. For instance, the K-Means technique implemented in
jClustering uses metrics, so it is very straightforward to get it to use a new
one: just implement it and it will appear in the metrics choice box next time
jClustering is executed.

## GUI development

It is possible for the techniques and metrics developed to have a graphical
user interface (GUI) for configuration purposes; this GUI will be accessed
using the appropriate tab on the main window. This is done by implementing the
`makeConfig()` method that returns a `JPanel` object with the necessary
components. The developer must implement all the necessary logic and listeners
to ensure that the configuration parameters entered by the users are correctly
set.

Certain listeners have already been implemented in the `ClusteringTechnique`
superclass to ease the process of showing the user the available metrics, in
case they are necessary. Also, the function `addMetricsToJPanel(JPanel jp)`
takes care of the whole process. These two methods alone implement a panel with
a selector for choosing a metric: 

```java
protected JPanel makeConfig() {

    JPanel jp = new JPanel();
    addMetricsToJPanel(jp);

    return jp;

}

public void itemStateChanged(ItemEvent arg0) {
    super.itemStateChanged(arg0);
}
```

# Annex: full sample classes

```java
package jclustering.techniques;

import ij.IJ;
import java.awt.event.ItemEvent;
import javax.swing.JPanel;

/**
 * Sample class. Uses the peak time to build the cluster, as in the 
 * previously shown examples.
 */
public class SampleTechnique extends ClusteringTechnique {

    @Override
    public ImagePlus process() {

        // Return a stack with the value of the frame of maximum intensity
        // for each voxel
        int[] dim = ip.getDimensions();

        for (int slice = 1; slice <= dim[3]; slice++) {
            for (int x = 0; x < dim[0]; x++) {
                for (int y = 0; y < dim[1]; y++) {

                    // Get TAC
                    double[] tac = ip.getTAC(x, y, slice);

                    // If is noise, skip
                    if (skip_noisy && isNoise(tac))
                        continue;

                    // Else, set the corresponding result
                    int n = _getMaxIndex(tac) + 1; // +1, min_cluster = 1.

                    // Set temporal result
                    addTACtoCluster(tac, n);

                }
            }
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
```

-----
```java
package jclustering.metrics;

import java.awt.Label;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;

import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Generic p-norm metric. Includes a small configuration method that
 * is implemented using a FocusListener (implemented in the class)
 * so that there is no necessity for a “OK” button to assign the values.
 */
public class PNorm extends ClusteringMetric implements FocusListener {

    private double p = 2.0;
    private JTextField jt;

    @Override
    /**
     * Computes the p-norm between the {@code centroid} and {@code data} 
     * vectors.
     */
    public double distance(double[] centroid, double[] data) {

        if (Arrays.equals(centroid, data) || centroid.length != data.length)
            return 0.0;

        double result = 0.0;

        for (int i = 0; i < data.length; i++) {
            result += Math.pow(Math.abs(centroid[i] - data[i]), p);
        }

        return Math.pow(result, 1.0 / p);
    }

    @Override
    /**
     * Build a JTextField to introduce the new value for p.
     * <p>
     * p is set when the JTextField loses the focus thanks to
     * this class implementing the FocusListener interface.
     */
    public JPanel makeConfig() {

        JPanel jp = new JPanel();
        jp.add(new Label("Insert new p value:"));
        jt = new JTextField(Double.toString(p));
        jt.addFocusListener(this);
        jp.add(jt);
        return jp;

    }

    @Override
    /**
     * Assigns the p value when the JTextField loses the focus.
     */
    public void focusLost(FocusEvent e) {

        try {
            p = Double.parseDouble(((JTextField) e.getComponent()).getText());
        } catch (NumberFormatException ex) {
            // Wrong input. Back to default value.
            p = 2.0;
        }

        jt.setText(Double.toString(p));

    }

    @Override
    /**
     * Just selects all the text inside the JTextField, for usability purposes.
     */
    public void focusGained(FocusEvent e) {

        jt.selectAll();

    }

}
```
