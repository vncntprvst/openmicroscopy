/*
*   $Id$
*
*   Copyright 2009 Glencoe Software, Inc. All rights reserved.
*   Use is subject to license terms supplied in LICENSE.txt
*
*/

#ifndef OMERO_REPOSITORY_ICE
#define OMERO_REPOSITORY_ICE

#include <omero/ModelF.ice>
#include <omero/ServicesF.ice>
#include <omero/Collections.ice>
#include <omero/ServerErrors.ice>

module omero {

    //
    // See README.ice for a description of this module.
    //
    module grid {
        
        class RepositoryListConfig 
        {
            int depth;
            bool files;
            bool dirs;
            bool system;
            bool registered;
        };

        class FileSet 
        {
            bool importableImage;
            string fileName;
	        string imageName;
            string reader;
	        int imageCount;
            omero::api::IObjectList usedFiles;
            omero::api::ImageList imageList;
        };

        ["java:type:java.util.ArrayList<FileSet>:java.util.List<FileSet>"]
            sequence<FileSet> FileSetList;
        
        /**
         * Client-accessible interface representing a single mount point on the server-side.
         **/
        ["ami"] interface Repository {

            /**
             * Return the OriginalFile descriptor for this Repository. It will have
             * the path "/"
             **/
            omero::model::OriginalFile root() throws ServerError;

            /*
             * Directory listing methods. 
             */

            // A list of all files and/or directories, registered or not depending on cnfig.
            omero::api::OriginalFileList list(string path, RepositoryListConfig config) 
                    throws ServerError;
            
            // A list of importable and non-importable file sets in a directory depending on config.
            FileSetList listObjects(string path, RepositoryListConfig config) 
                    throws ServerError;
            
            /**
             * Returns the best-guess of the [omero::model::Format] for the given path.
             * If the file is "known" (see [listKnown]), then the format returned will
             * be the stored value, rather than a newly calculated one.
             **/
            omero::model::Format format(string path) throws ServerError;

            /**
             * Create an entry in the database for the given IObject using the given omeName.
             *
             * If the given IObject is null a ValidationException is thrown. 
             * If the given IObject is not a recognised type ValidationException is thrown. 
             * If the given omeName is null or empty ValidationException is thrown. 
             * Otherwise, an entry is added and an unloaded IObject returned with id set.
             *
             * TODO should this final exception just return and not throw?
             *
             **/
             omero::model::IObject register(omero::model::IObject obj, string omeName)
                    throws ServerError;

            /**
             * Create an entry in the database for the given IObject.
             *
             * If the given IObject is null a ValidationException is thrown. 
             * If the given IObject is not a recognised type ValidationException is thrown. 
             * Otherwise, an entry is added and an unloaded IObject returned with id set.
             *
             * TODO should this final exception just return and not throw?
             *
             **/
            omero::model::IObject registerObject(omero::model::IObject obj) 
                    throws ServerError;
            
            /**
             * Load the OriginalFile at the given path with annotations and
             * associated Pixels (if present). If the path does not point to
             * an OriginalFile, a ValidationException exception is thrown.
             *
             * TODO should this just return null instead?
             **/
            omero::model::OriginalFile load(string path) throws ServerError;

            /**
             * Returns a special RawFileStore which permits only reading.
             * Any call to a write or configuration method will throw an
             * ApiUsageException.
             **/
            omero::api::RawFileStore* read(string path) throws ServerError;

            /**
             * Returns a special RawFileStore which permits only writing.
             * Any call to a read or configuraiton method will throw an
             * ApiUsageException. If a file exists at the given path, a
             * ValidationException will be thrown. Once writing is complete,
             * call close(), which will seal the file from all further writing.
             * The SHA1 of the OriginalFile should be checked against the local
             * value.
             **/
            omero::api::RawFileStore*    write(string path) throws ServerError;
            omero::api::RawPixelsStore*  pixels(string path) throws ServerError;
            omero::api::RenderingEngine* render(string path) throws ServerError;
            omero::api::ThumbnailStore*  thumbs(string path) throws ServerError;

            void rename(string path) throws ServerError;
            void delete(string path) throws ServerError;
            void transfer(string srcPath, Repository* target, string targetPath) 
                    throws ServerError;

            /* TODO for both methods: return binary data rather than paths to jpgs ? */
            /**
             * Return the full path of a jpg thumbnail of the image file
             * given in the path argument.
             **/
            string getThumbnail(string path) throws ServerError;
            
            /**
             * Return the full path of a jpg thumbnail of the image 
             * at the imageIndex in the file set represented by
             * the file given in the path argument.
             **/
            string getThumbnailByIndex(string path, int imageIndex) throws ServerError;
            
        };

        /**
         * Internal portion of the API used for management. Not available to clients.
         **/
        ["ami"] interface InternalRepository {

            //
            // Provides all the stateful services dealing with binary data
            //
            omero::api::RawFileStore*    createRawFileStore(omero::model::OriginalFile file) 
                    throws ServerError;
            omero::api::RawPixelsStore*  createRawPixelsStore(omero::model::OriginalFile file) 
                    throws ServerError;
            omero::api::RenderingEngine* createRenderingEngine(omero::model::OriginalFile file) 
                    throws ServerError;
            omero::api::ThumbnailStore*  createThumbnailStore(omero::model::OriginalFile file) 
                    throws ServerError;

            // Other repository methods
            omero::model::OriginalFile getDescription() throws ServerError;
            // If this returns null, user will have to wait
            Repository* getProxy() throws ServerError;  

            string getFilePath(omero::model::OriginalFile file) 
                    throws ServerError;

        };

        ["java:type:java.util.ArrayList<omero.grid.RepositoryPrx>:java.util.List<omero.grid.RepositoryPrx>"]
            sequence<Repository*> RepositoryProxyList;

        /**
         * Return value for [omero::grid::SharedResources].acquireRepositories()
         */
        struct RepositoryMap {
            omero::api::OriginalFileList descriptions;
            RepositoryProxyList proxies;
        };


};


};

#endif
