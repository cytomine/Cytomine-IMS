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

grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
grails.project.war.file = "IMS.war"
grails.project.dependency.resolver = "maven"
//grails.project.fork = [ test: false, run: false, war: false, console: false ]
grails.project.fork = [
        // configure settings for compilation JVM, note that if you alter the Groovy version forked compilation is required
        //  compile: [maxMemory: 256, minMemory: 64, debug: false, maxPerm: 256, daemon:true],

        // configure settings for the test-app JVM, uses the daemon by default
        test: [maxMemory: 768*5, minMemory: 64*5, debug: false, maxPerm: 256, daemon:true],
        // configure settings for the run-app JVM
        run: [maxMemory: 768*5, minMemory: 64*5, debug: false, maxPerm: 256, forkReserve:false],
        // configure settings for the run-war JVM
        war: [maxMemory: 768*5, minMemory: 64*5, debug: false, maxPerm: 256, forkReserve:false],
        // configure settings for the Console UI JVM
        console: [maxMemory: 768*5, minMemory: 64*5, debug: false, maxPerm: 256]
]


grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
        excludes 'httpclient'
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        mavenLocal()
        mavenCentral()
        mavenRepo "https://packagecloud.io/cytomine-uliege/Cytomine-java-client/maven2"

        grailsPlugins()
        grailsHome()
        grailsCentral()


    }
    dependencies {
        compile 'be.cytomine.client:cytomine-java-client:2.0-SNAPSHOT'
        compile 'com.vividsolutions:jts:1.13'
        compile 'net.imagej:ij:1.51h'
        compile 'com.github.jai-imageio:jai-imageio-core:1.4.0'
    }

    plugins {
        compile ":grails-melody:1.49.0"
        build ':tomcat:7.0.54'
        compile (':hibernate:3.6.10.17') {
            excludes('hibernate-ehcache')
        }
        runtime ":resources:1.2.8"
//        runtime ':background-thread:1.6'
        runtime ":executor:0.3"
        compile ":rest-api-doc:0.6.1"
        compile ":quartz:1.0.1"
    }
}


