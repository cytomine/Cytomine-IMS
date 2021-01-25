# Cytomine IMS

> Cytomine Image Management System (Cytomine-IMS) is a web application for image manipulation.

[![Build Status](https://travis-ci.com/Cytomine-ULiege/Cytomine-IMS.svg?branch=master)](https://travis-ci.com/Cytomine-ULiege/Cytomine-IMS)
[![GitHub release](https://img.shields.io/github/release/Cytomine-ULiege/Cytomine-IMS.svg)](https://github.com/Cytomine-ULiege/Cytomine-IMS/releases)
[![GitHub](https://img.shields.io/github/license/Cytomine-ULiege/Cytomine-IMS.svg)](https://github.com/Cytomine-ULiege/Cytomine-IMS/blob/master/LICENSE)

## Overview

The goal of this server is to carry image operations and to provide a simple REST API with multiple image formats support. In particular, IMS server provides services to:
* upload an image
* get image thumbs
* extract image tiles
* extract crops

### Supported formats
* Pyramidal TIFF (.tif)
* Planar TIFF (.tif)
* JP2000 (.jp2)
* JPEG (.jpg)
* PNG (.png)
* PGM (.pgm)
* DICOM (.dcm)
* BMP (.bmp)
* Aperio (.svs)
* Hamamatsu (.vms, .ndpi)
* Leica (.scn)
* MIRAX (.mrxs)
* Philips (.tiff)
* Sakura (.svslide)
* Ventana (.bif, .tif)
* Huron TIFF (.tif)
* Photoshop TIFF (.tif)
* CZI TIFF (.tif)
* Cell Sens VSI (.vsi)
* Olympus Dot slide
* OME-TIFF (.ome.tif)
* Zeiss (.czi)
* GeoJP2 (.jp2)
* GeoTIFF (.tif)
* Format supported by Bioformats


## Install

It is automatically installed with the [Cytomine-bootstrap](https://github.com/cytomine/Cytomine-bootstrap) procedure using Docker. See [Cytomine installation documentation](https://documentation.cytomine.org/How-to-install-Cytomine?structure=Admins) for more details.

## References
When using our software, we kindly ask you to cite our website url and related publications in all your work (publications, studies, oral presentations,...). In particular, we recommend to cite (Marée et al., Bioinformatics 2016) paper, and to use our logo when appropriate. See our license files for additional details.

- URL: http://www.cytomine.org/
- Logo: [Available here](https://doc.cytomine.org/img/logo_cyto_org.png)
- Scientific paper: Raphaël Marée, Loïc Rollus, Benjamin Stévens, Renaud Hoyoux, Gilles Louppe, Rémy Vandaele, Jean-Michel Begon, Philipp Kainz, Pierre Geurts and Louis Wehenkel. Collaborative analysis of multi-gigapixel imaging data using Cytomine, Bioinformatics, DOI: [10.1093/bioinformatics/btw013](http://dx.doi.org/10.1093/bioinformatics/btw013), 2016. 

## License

LGPL-2.1