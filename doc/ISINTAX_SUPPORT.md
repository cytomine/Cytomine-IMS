## ISintax support

As it is only a POC, there are some dirty part:
* The upload redirect the request to PIMS. The frontend shows an error in after the upload but the UploadedFile should appears "deployed" after some seconds.
* The crop in PIMS is a POST request while the IMS internal crop method only generate a crop url. The isintax format needed a `BufferedImage cropImage()` instead of a `String cropUrl()`
* At the some place, the format is identified by content type which is not supported for isyntax (so as a Hack I detect the format using the extension)

### IMS deployment modification

Test has been done on `ims:visyntax-support-20211210141012-SNAPSHOT`

These two lines must be included in ims-config.groovy file:
cytomine.ims.pims.url=$PIMS_URL
cytomine.ims.pims.pathPrefix=$PIMS_DIRECTORY

$PIMS_DIRECTORY is the directory mapped with /data/pims in the PIMS container.

Example:
cytomine.ims.pims.url="http://172.17.0.1:5000"
cytomine.ims.pims.pathPrefix="/home/lrollus/data/pims/"

So the final bloc in the bootstrap will be

docker create --name ims \
--link bioformat:bioformat \
-e IMS_STORAGE_PATH=/data/images \
-v /data/images:/data/images \
-v /data/images/_buffer:/tmp/uploaded \
-v /home/lrollus/data/pims:/home/lrollus/data/pims \
--restart=unless-stopped \
cytomine/ims:visyntax-support-20211210141012-SNAPSHOT > /dev/null


### CORE deployment modification

Branch pims-isyntax

### PIMS deployment modification

You need to clone:
* corporate-pims repository.
* pims repository.
* isyntax plugin repository.
* openslide plugin repository.
In order to make the script work, they have to be in the same parent directory (example: ~/cytomine/pims , ~/cytomine/corporate-pims , ...).

In corporate-pims directory:
Run ./scripts/ciLocalBuildDockerImage.sh
This will build pims-corporate:vlocal image

Then run:
./example/rebuild_and_launch.sh
This will create and start the container.



