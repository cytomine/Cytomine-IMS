/*
 * Copyright (c) 2009-2018. Authors: see NOTICE file.
 *
 * Licensed under the GNU Lesser General Public License, Version 2.1 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



// locations to search for config files that get merged into the main config;
// config files can be ConfigSlurper scripts, Java properties files, or classes
// in the classpath in ConfigSlurper format

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.config.locations = ["file:${userHome}/.grails/imageserverconfig.properties"]

println grails.config.locations

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [
        all:           '*/*',
        atom:          'application/atom+xml',
        css:           'text/css',
        csv:           'text/csv',
        form:          'application/x-www-form-urlencoded',
        html:          ['text/html','application/xhtml+xml'],
        js:            'text/javascript',
        json:          ['application/json', 'text/json'],
        multipartForm: 'multipart/form-data',
        rss:           'application/rss+xml',
        text:          'text/plain',
        xml:           ['text/xml', 'application/xml'],
        jpg:           'image/jpeg',
        png:           'image/png',
        tiff:          'image/tiff'
]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']

// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

environments {
    development {
        grails.logging.jul.usebridge = "true"
        grails.serverURL = "http://localhost:9080"
    }
    production {
        grails.logging.jul.usebridge = false
        grails.serverURL = "http://image.cytomine.be"
    }
}
log4j = {
    // Example of changing the log pattern for the default console appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error  'org.codehaus.groovy.grails.web.servlet',        // controllers
            'org.codehaus.groovy.grails.web.pages',          // GSP
            'org.codehaus.groovy.grails.web.sitemesh',       // layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping',        // URL mapping
            'org.codehaus.groovy.grails.commons',            // core / classloading
            'org.codehaus.groovy.grails.plugins',            // plugins
            'org.codehaus.groovy.grails.orm.hibernate',      // hibernate integration
            'org.springframework',
            'org.hibernate',
            'net.sf.ehcache.hibernate'


    environments {
        production {
            root {
                info 'appLog',"logfile"
//               error  'mail'
                additivity = true
            }
        }
        development {
            root {
                info 'appLog',"logfile", 'stdout'
                additivity = true
            }
        }
        test {
            root {
                info 'appLog',"logfile", 'stdout'
                additivity = true
            }
        }
        perf {
            root {
                info 'appLog',"logfile", 'stdout'
                additivity = true
            }
        }
    }
}

// Rest API Doc plugin
grails.plugins.restapidoc.docVersion = "0.1"
grails.plugins.restapidoc.basePath = "demo-ims.cytomine.be"
grails.plugins.restapidoc.grailsDomainDefaultType = "int"

cytomine.coreURL="http://localhost-core:8080"
cytomine.storageBufferPath="/tmp/imageserver_buffer"

cytomine.iipImageServerBase="http://localhost-iip-cyto/fcgi-bin/iipsrv.fcgi"
cytomine.Jpeg2000Enabled=true
cytomine.iipImageServerJpeg2000="http://localhost-iip-jp2000/fcgi-bin/iipsrv.fcgi"
cytomine.iipImageServerCyto="http://localhost-iip-cyto/fcgi-bin/iipsrv.fcgi"

bioformat.application.enabled="true"
bioformat.application.location="bioformat"
bioformat.application.port="4321"

cytomine.imageServerPublicKey="DEF"
cytomine.imageServerPrivateKey="ABC"

//image manipulation executable
cytomine.vips = "vips"
cytomine.tiffinfo = "tiffinfo"
cytomine.identify = "identify"
cytomine.vipsthumbnail = "vipsthumbnail"
cytomine.gdalinfo = "gdalinfo"
cytomine.gdaltranslate = "gdal_translate"

cytomine.imageConversionAlgorithm = "lzw"

cytomine.maxAnnotationOnImageWidth = 200000

cytomine.charset = "UTF-8"

cytomine.maxCropSize = 15000

cytomine.hdf5.maxBurstSize = 512 // Mbytes
cytomine.hdf5.maxBlockSize = 15 // Mbytes

cytomine.deleteImageFilesFrequency = "600000"
