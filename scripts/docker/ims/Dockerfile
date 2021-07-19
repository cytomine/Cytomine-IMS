#
# Copyright (c) 2009-2020. Authors: see NOTICE file.
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

FROM cytomine/tomcat7:v1.2.1

MAINTAINER Cytomine Team "support@cytomine.be"

RUN apt-get update -y && apt-get install -y \
      ant \
      dnsutils \
      git \
      gobject-introspection \
      gtk-doc-tools \
      imagemagick \
      libgirepository1.0-dev \
      libglib2.0-dev \
      libgsf-1-dev \
      libjpeg8-dev \
      libmemcached-dev \
      libopenjpeg-dev \
      libtiff5-dev \
      libtiff-tools \
      libxml2-dev \
      swig && \
    apt-get -y build-dep openslide iipimage-server libvips-dev

#openslide
RUN cd /tmp && git clone https://github.com/cytomine/openslide && cd /tmp/openslide && git checkout 3a284f9ab4fb39f5081f61f1b0169cdda8dde962 && autoreconf -i && ./configure && make && make install

# use previous lib of lipopenjpeg-dev due to the bug https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=734238
RUN cd /tmp && wget http://launchpadlibrarian.net/147357170/libopenjpeg2_1.3%2Bdfsg-4.6ubuntu2_amd64.deb && dpkg -i libopenjpeg2_1.3+dfsg-4.6ubuntu2_amd64.deb
RUN cd /tmp && wget http://launchpadlibrarian.net/147357169/libopenjpeg-dev_1.3%2Bdfsg-4.6ubuntu2_amd64.deb && dpkg -i libopenjpeg-dev_1.3+dfsg-4.6ubuntu2_amd64.deb

# vips
RUN cd /tmp && wget https://github.com/libvips/libvips/releases/download/v8.5.1/vips-8.5.1.tar.gz && tar -zxvf ./vips-8.5.1.tar.gz && cd ./vips-8.5.1 && LDFLAGS="-L/usr/local/lib -lopenslide" CPPFLAGS="-I/usr/local/include/openslide"  ./configure && make && make install

#openslide-java
RUN update-java-alternatives -s java-8-oracle
RUN export JAVA_HOME=/usr/lib/jvm/java-8-oracle && export CFLAGS="-I/usr/lib/jvm/java-8-oracle" && cd /tmp && git clone https://github.com/cytomine/openslide-java && cd /tmp/openslide-java && git checkout 93af97eeca8f0b63e9f71a6916af474ecc474dc8 && autoreconf -i && ./configure && make && make install


EXPOSE 8081
EXPOSE 8082

RUN ldconfig -v

ARG IMS_URL
ENV IMS_URL=$IMS_URL


RUN chmod -R 777 /var/lib/tomcat7
RUN rm -r /var/lib/tomcat7/webapps/* && cd /var/lib/tomcat7/webapps/ && wget $IMS_URL -O ROOT.war
RUN cd /var/lib/tomcat7/  && wget -q https://github.com/cytomine/Cytomine-IMS/releases/download/v1.2.6/restapidoc.json -O restapidoc.json

RUN mkdir -p /usr/share/tomcat7/.grails

RUN touch /tmp/addHosts.sh
ADD deploy.sh /tmp/deploy.sh
RUN chmod +x /tmp/deploy.sh
ADD setenv.sh /tmp/setenv.sh
RUN chmod +x /tmp/setenv.sh

ENTRYPOINT ["/tmp/deploy.sh"]

