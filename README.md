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
Once jClustering has been installed, you can access it via the Plugins ->
jclustering -> JClustering menu on ImageJ. If no images are open and it is
executed, the following window appears:

[IMAGE STILL MISSING]

jClustering needs a HyperStack (a 4D image in ImageJ terminology) to work with.
It will not open the file for the user, as every format is different and
getting a structured HyperStack depends on every type of image.

Once the HyperStack has been correctly opened, jClustering will show the
following screen (or one very similar, depending on the exact version) upon
execution:

[IMAGE STILL MISSING]

In this screen you can select which clustering technique you wish to use, and
then configure it (second tab) and the metric the technique uses, if any (third
tab). By convention, labels that have a "\*" symbol at the end have a helper
tooltip text that appears when the user places the mouse cursor over them; this
text is used to clarify the label text when some extended explanation is
needed.

After the clustering modality has been configured, the user can press the “OK”
button and, after the necessary processing time, the resulting image will
appear on screen.

