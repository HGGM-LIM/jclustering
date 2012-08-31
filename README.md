# Introduction

This documentation explains what jClustering is, how to use it and how to
develop a new clustering technique or metric for the jClustering framework
using simple examples and code snippets. Several working examples will be
attached to this document annex for full reference.

# What is jClustering?

jClustering is an [ImageJ](http://rsbweb.nih.gov/ij/) plugin developed with the
purpose of becoming a general framework for dynamic imaging clustering, such as
dynamic PET segmentation. It consists of grouping together voxels with similar
time-activity curves (TACs). jClustering is written in Java and provides a very
simple API interface that will allow the implementation of new clustering
algorithms in a short time. The fact that it is implemented on top of ImageJ
gives it all the advantages of using an open-source imaging analysis platform.

# Installation

The installation of  jClustering within ImageJ is straightforward: download the
latest version available at the [download
page](https://github.com/HGGM-LIM/jclustering/downloads) and copy the
`jclustering/` folder to the `plugins/` folder of your ImageJ installation. Do
not care about the number of downloads that `jclustering-latest.zip` has, as it
resets every time a new version is uploaded.

jClustering uses Apache Commons Math version 3.0 for some mathematical
operations, so this library also needs to be installed. The process is simple:
go to [Apache Commons Math
webpage](http://commons.apache.org/math/download_math.cgi) and download the
file `commons-math3-3.0-bin.zip`, open it with any unzip program and copy the
file `commons-math3-3.0/commons-math3-3.0.jar` to the `plugins/jars` folder of
your ImageJ installation.

The minimum ImageJ version needed for jClustering to run is 1.47a. Minimum
Java version is JRE 6.

# How to use jClustering
Once jClustering has been installed, you can access it via the `Plugins ->
jclustering -> JClustering` menu on ImageJ. If no images are open and it is
executed, the following window appears:

![Error window] (https://raw.github.com/HGGM-LIM/jclustering/gh-pages/images/doc_error.jpg)

jClustering needs a HyperStack (a 4D image in ImageJ terminology) to work with.
It will not open the file for the user, as every format is different and
getting a structured HyperStack depends on every type of image.

Once the HyperStack has been correctly opened, jClustering will show the
following screen (or one very similar, depending on the exact version) upon
execution:

![Main window](https://raw.github.com/HGGM-LIM/jclustering/gh-pages/images/doc_main_window.jpg)

In this screen you can select which clustering technique you wish to use, and
then configure it (second tab) and the metric the technique uses, if any (third
tab). By convention, labels that have a "\*" symbol at the end have a helper
tooltip text that appears when the user places the mouse cursor over them; this
text is used to clarify the label text when some extended explanation is
needed.

After the clustering modality has been configured, the user can press the “OK”
button and, after the necessary processing time, the resulting image will
appear on screen.

![Results window](https://raw.github.com/HGGM-LIM/jclustering/gh-pages/images/doc_results.jpg)

The resulting image is another HyperStack that contains the results structured
the following way:

* The last _frame_ (not a real frame as it does not represent temporal data) is
  a 3D image in which every voxel contains the value of the cluster that voxel
  belongs to. For instance, a voxel with a grey value of 1 belongs to the first
  cluster, and so on.

* All the other frames, starting from the first one, represent the contents of
  each cluster.

If you wish to select a file for saving the TACs for each cluster, just select
a path by clicking on the provided button and the results will be saved using
the selected format. If you have vector time data, please specify it using the
appropriate button. This data contains the length of each image frame, in
seconds, in a two-column text file separated by spaces, as in the following
sample:

<pre>
1.0 2.0
2.0 3.0
3.0 4.0	
...
</pre>

The first column contains the starting time and the second column the ending
time; each row is a frame. This time vector will be used when saving the
kinetic data to file. If no time vector is provided, time data will be omitted
or automatically generated (n frames of 1.0 seconds length each), depending on
the format selected.

# Developing for jClustering

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
