# jClustering

## Introduction

This documentation explains how to use jClustering and how to develop a new
clustering technique or metric for the jClustering framework using simple
examples and code snippets. Several working examples will be attached to this
document annex for full reference.

## What is jClustering?

jClustering is an [ImageJ](http://rsbweb.nih.gov/ij/) plugin developed with the
purpose of becoming a general framework for dynamic imaging clustering, such as
dynamic PET segmentation. jClustering is written in Java and provides a very
simple API interface that will allow the implementation of new clustering
algorithms in a short time. The fact that it is implemented on top of ImageJ
gives it all the advantages of using an open-source imaging analysis platform.

## Installation

The installation of  jClustering within ImageJ is straightforward: simply copy
the folder jclustering/ to the plugins/ folder of your ImageJ installation.

jClustering uses Apache Commons Math version 3.0 for some mathematical
operations, so this library also needs to be installed into the system. The
process is simple: go to [Apache Commons Math
wegpage](http://commons.apache.org/math/download_math.cgi) and download the
file commons-math3-3.0-bin.zip, open it with any unzip program and copy the
file commons-math3-3.0/commons-math3-3.0.jar to the plugins/jars folder of your
ImageJ installation.

## How to use jClustering
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

The resulting image is another HyperStack that ideally contains the results
structured the following way, though specific implementations may vary and this
is only a convention:

* The last _frame_ (not a real frame, shown on the image above these lines) is
  a 3D image in which every voxel contains the value of the cluster that voxel
  belongs to. For instance, a voxel with a grey value of 1 belongs to the
  first cluster, and so on.

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
public ImagePlus process() {
}
```

This method should do two things. 

* First, it must return a `ImagePlus` object with the structure recommended on a
  previous chapter.
* Set an internal `clusters` object of the type `ArrayList<Cluster>` that
  contains the formed cluster TACs.

As said before, the structure for the ImagePlus object that is returned by the
`process()` method is just a recommendation. Also, there are several helper
functions that will help the developer set the appropriate voxels in a
`ImageStack` object or in the `clusters` object. Please refer to the javadoc
documents for more information on the `Cluster` object, if needed.

The `ImagePlus` that needs to be returned might be built in a relatively simple
way. Consider this `process()` method (and the private function below it):

```java
public ImagePlus process() {
	
	// Return a stack with the value of the frame of maximum intensity
	// for each voxel
	int [] dim = ip.getDimensions();
	ImagePlus res = IJ.createImage("Clusters", "16-bit",	
					dim[0], dim[1], dim[3]);		
	ImageStack is = res.getStack();
	for (int slice = 1; slice <= dim[3]; slice++) {
		for (int x = 0; x < dim[0]; x++) {
			for (int y = 0; y < dim[1]; y++) {

				// Get TAC
				double [] tac = ip.getTAC(x, y, slice);

				// If is noise, skip
				if (skip_noisy && isNoise(tac)) continue;

				// Else, set the corresponding result
				int n = getMaxIndex(tac) + 1; // +1, min_cluster = 1.					
				// Set temporal result
				addTACtoCluster(tac, n);			
				// Set visual result
				setVoxel(is, x, y, slice, n);
			}
		}
	}
	return expand(res, clusters.size());
}


private int getMaxIndex(double [] d) {
	
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

The first lines of the method just prepare an empty `ImagePlus` image to set
the result and get a reference its inner `ImageStack`. The object `ip` is
internal to the `ClusteringTechnique` superclass and contains a reference to
the image that is being analyzed. See the documentation for `ImagePlusHyp` in
the Javadoc documentation for more details. This objects inherits all the
methods from ImageJ's `ImagePlus`, plus several others.

The three nested for loops iterate through all the TACs of the image. The
`slice` variable is 1-based in ImageJ, so it has to go from `1` to `dim[3]`.

The TAC is easily accessed using the public method `getTAC(int x, int y, int
slice)` from the `ImagePlusHyp` object. If the obtained TAC is noise according
to the `isNoise(double [] tac)` method, it is skipped and the next one is
obtained.  This step improves algorithm performance by not analyzing noisy
voxels. This option can be disabled on the first tab of the GUI.

The methods `addTACToCluster(double [] tac, Cluster c)` (from the
`ClusteringTechnique` superclass) and `setVoxel(ImageStack is, int x, int y,
int slice, int value)` (from the `jclustering.Utils` package) provide an easy
way to set the final cluster values to the resulting cluster and image,
respectively.  The user that wishes to implement its own technique can use
these methods if desired, but they are in no way mandatory, as long as the
results are set in some way.

Finally, a call to `expand(ImagePlus ip, int size)` is made. This function
expands the ImagePlus that has been built along the way and separates it in the
appropriate frames to return a result that conforms to the structure defined at
the end of _How to use jClustering_.

