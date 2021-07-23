#!/bin/bash

set -e

echo "Downloading dependencies..."

# Create an output directory if not one already
[ -d contrib ] || echo "Making contrib directory..." && mkdir -p contrib

# Download the dependencies if they do not already exist
([ -f contrib/objenesis.jar ] && echo "Objenesis already extracted, skipping download") \
    || ([ -f contrib/objenesis.zip ] && echo "Objenesis already downloaded, skipping download") \
    || (echo "Objenesis not downloaded yet, downloading it now..." \
        && wget https://bintray.com/artifact/download/easymock/distributions/objenesis-2.6-bin.zip \
        -O contrib/objenesis.zip)

([ -f contrib/javassist.jar ] && echo "Javassist already extracted, skipping download") \
    || ([ -f contrib/javassist.zip ] && echo "Javassist already downloaded, skipping download") \
    || (echo "Javassist not dowloaded yet, downloading it now..." \
        && wget https://github.com/jboss-javassist/javassist/zipball/master \
        -O contrib/javassist.zip)

([ -f contrib/asm.jar ] && echo "ASM already extracted, skipping download") \
    || (echo "ASM not downloaded yet, downloading it now..." \
        && wget http://central.maven.org/maven2/org/ow2/asm/asm/6.2/asm-6.2.jar \
        -O contrib/asm.jar)
        
([ -f contrib/asm-commons.jar ] && echo "ASM commons already extracted, skipping download") \
    || (echo "ASM not downloaded yet, downloading it now..." \
        && wget http://central.maven.org/maven2/org/ow2/asm/asm-commons/6.2/asm-commons-6.2.jar \
        -O contrib/asm-commons.jar)
                
                
 ([ -f contrib/asm-util.jar ] && echo "ASM util already extracted, skipping download") \
    || (echo "ASM not downloaded yet, downloading it now..." \
        && wget http://central.maven.org/maven2/org/ow2/asm/asm-util/6.2/asm-util-6.2.jar \
        -O contrib/asm-util.jar)       
    

([ -f contrib/asm-tree.jar ] && echo "ASM util already extracted, skipping download") \
    || (echo "ASM not downloaded yet, downloading it now..." \
        && wget http://central.maven.org/maven2/org/ow2/asm/asm-tree/6.2/asm-tree-6.2.jar \
        -O contrib/asm-tree.jar)        
                
([ -f contrib/asm-analysis.jar ] && echo "ASM analysis already extracted, skipping download") \
    || (echo "ASM not downloaded yet, downloading it now..." \
        && wget http://central.maven.org/maven2/org/ow2/asm/asm-analysis/6.2/asm-analysis-6.2.jar \
        -O contrib/asm-analysis.jar) 
        
([ -f contrib/retrolambda.jar ] && echo "Retrolambda already extracted, skipping download") \
    || (echo "Retrolambda not downloaded yet, downloading it now..." \
        && wget https://oss.sonatype.org/content/groups/public/net/orfjackal/retrolambda/retrolambda/2.5.4/retrolambda-2.5.4.jar \
        -O contrib/retrolambda.jar)


# Unzip the downloaded files
cd contrib
[ -f objenesis.zip ] && echo "Extracting objenesis.jar... " && \
    unzip -p objenesis.zip '*/objenesis-?.?.jar' > objenesis.jar
[ -f javassist.zip ] && echo "Extracting javassist.jar..." && \
    unzip -p javassist.zip '*/javassist.jar' > javassist.jar


    
