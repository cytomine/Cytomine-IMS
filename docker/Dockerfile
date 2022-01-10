#
# Copyright (c) 2009-2022. Authors: see NOTICE file.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM tomcat:9.0-jdk8-openjdk

MAINTAINER Cytomine SCRLFS "support@cytomine.coop"


# base librairies and configuration
RUN apt-get update -y && apt-get -y install \
      build-essential \
      curl \
      locate \
      logrotate \
      net-tools \
      unzip \
      wget

RUN sed -i "/su root syslog/c\su root root" /etc/logrotate.conf
ENV LANG C.UTF-8
ENV DEBIAN_FRONTEND noninteractive


#tomcat configuration

RUN apt-get -y update && apt-get install -y autoconf automake libpopt-dev libtool make xz-utils
RUN cd /tmp/ && wget https://github.com/logrotate/logrotate/releases/download/3.18.0/logrotate-3.18.0.tar.xz && tar -xJf logrotate-3.18.0.tar.xz
RUN cd /tmp/logrotate-3.18.0 && autoreconf -fiv && ./configure && make

RUN cp /tmp/logrotate-3.18.0/logrotate /usr/sbin/logrotate


# ims specificities

RUN apt-get -y update && apt-get install --no-install-recommends --no-install-suggests -y ant \
      automake \
      dnsutils \
      ffmpeg \
      git \
      gobject-introspection \
      gtk-doc-tools \
      libcairo2-dev \
      libfftw3-dev \
      libgdk-pixbuf2.0-dev \
      libgsf-1-dev \
      libglib2.0-dev \
      libimage-exiftool-perl \
      libjpeg62-turbo-dev \
      libopenexr-dev \
      libopenjp2-7-dev \
      liborc-0.4-0 \
      liborc-0.4-dev \
      libsqlite3-dev \
      libtiff5-dev \
      libtiff-tools \
      libtool \
      libxml2-dev \
      software-properties-common \
      swig && \
    rm -rf /var/lib/apt/lists/*

# openslide
ARG OPENSLIDE_VERSION=3.4.1
RUN cd /tmp && \
    wget https://github.com/openslide/openslide/releases/download/v${OPENSLIDE_VERSION}/openslide-${OPENSLIDE_VERSION}.tar.gz && \
    tar -zxvf ./openslide-${OPENSLIDE_VERSION}.tar.gz && \
    cd ./openslide-${OPENSLIDE_VERSION} && \
    autoreconf -i && \
    ./configure && \
    make && \
    make install && \
    ldconfig

# openslide-java
ARG OPENSLIDE_JAVA_VERSION=0.12.2
RUN export JAVA_HOME="/usr/local/openjdk-8" && \
    export CFLAGS="-I/usr/local/openjdk-8" && \
    cd /tmp && \
    wget https://github.com/openslide/openslide-java/releases/download/v${OPENSLIDE_JAVA_VERSION}/openslide-java-${OPENSLIDE_JAVA_VERSION}.tar.gz && \
    tar -zxvf ./openslide-java-${OPENSLIDE_JAVA_VERSION}.tar.gz && \
    cd ./openslide-java-${OPENSLIDE_JAVA_VERSION} && \
    autoreconf -i && \
    ./configure && \
    make && \
    make install && \
    ldconfig
ENV LD_LIBRARY_PATH=/usr/local/lib/openslide-java

# imagemagick 6.8.9-10 (fix problem with DICOM conversion by vips)
ARG IMAGE_MAGICK_VERSION=6.8.9-10
RUN cd /tmp && \
    wget https://www.imagemagick.org/download/releases/ImageMagick-${IMAGE_MAGICK_VERSION}.tar.xz && \
    tar xf ImageMagick-${IMAGE_MAGICK_VERSION}.tar.xz && \
    cd ImageMagick-${IMAGE_MAGICK_VERSION} && \
    ./configure && \
    make && \
    make install && \
    ldconfig /usr/local/lib

# vips
ARG VIPS_VERSION=8.11.2
RUN cd /tmp && \
    wget https://github.com/libvips/libvips/releases/download/v${VIPS_VERSION}/vips-${VIPS_VERSION}.tar.gz && \
    tar -zxvf ./vips-${VIPS_VERSION}.tar.gz && \
    cd ./vips-${VIPS_VERSION} && \
    LDFLAGS="-L/usr/local/lib -lopenslide" CPPFLAGS="-I/usr/local/include/openslide" ./configure && \
    make && \
    make install && \
    ldconfig

# gdal
RUN apt-get update -y && \
    apt-get install --no-install-recommends --no-install-suggests -y gdal-bin && \
    rm -rf /var/lib/apt/lists/*

RUN ln -s /usr/local/tomcat /var/lib/tomcat9 #for backward compatibility
RUN ln -s /usr/share/tomcat9/.grails /root/.grails #for backward compatibility

ARG RELEASE_PATH=https://github.com/cytomine/Cytomine-IMS/releases/download/v2.0.0
ADD ${RELEASE_PATH}/IMS.war /var/lib/tomcat9/webapps/ROOT.war
ADD IMS.war /var/lib/tomcat9/webapps/ROOT.war
ADD ${RELEASE_PATH}/restapidoc.json /var/lib/tomcat9/webapps/restapidoc.json

RUN mkdir -p /usr/share/tomcat9/.grails

RUN ln -s /usr/local/bin/vips /usr/bin/vips && ln -s /usr/local/bin/identify /usr/bin/identify

RUN touch /tmp/addHosts.sh
COPY setenv.sh /usr/share/tomcat7/bin/setenv.sh
RUN chmod +x /usr/share/tomcat7/bin/setenv.sh
COPY deploy.sh /tmp/deploy.sh
RUN chmod +x /tmp/deploy.sh

ENTRYPOINT ["/tmp/deploy.sh"]
