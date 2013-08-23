# Introduction

jClustering is a dynamic imaging clustering framework for ImageJ. It is not
only a development API for automatic segmentation algorithms, but also a 
platform aimed at centralizing different implementations that as today
are not available in either source code or binary download.

## Reference

Mateos-Pérez JM, García-Villalba C, Pascau J, Desco M, Vaquero JJ (2013)
jClustering, an Open Framework for the Development of 4D Clustering Algorithms.
PLoS ONE 8(8): e70797. doi:10.1371/journal.pone.0070797. [Link](http://www.plosone.org/article/info%3Adoi%2F10.1371%2Fjournal.pone.0070797). 

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

If you want to use jClustering as the platform to implement your own clustering
algorithms, please refer to the [developer
manual](https://github.com/HGGM-LIM/jclustering/blob/master/doc/developer_manual.pdf?raw=true)
for a brief development guide. If you have any questions, please do not
hesitate to ask.

# External tools

As an independent project that intends to add more advanced dynamic imaging capabilities
to ImageJ, you might want to try the [LIM Tools plugin](https://github.com/HGGM-LIM/limtools).
It adds several tools to mask dynamic studies, create average images and explore the 
time-activity curves, for example.

