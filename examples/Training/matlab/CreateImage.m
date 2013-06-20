% Copyright (C) 2011 University of Dundee & Open Microscopy Environment.
% All rights reversed.
% Use is subject to license terms supplied in LICENSE.txt


% Create an image from an existing image.
%
% The source image must have at least 2 channels.

try
    % Create a connection
    [client, session] = loadOmero();
    fprintf(1, 'Created connection to %s\n', char(client.getProperty('omero.host')));
    fprintf(1, 'Created session for user %s using group %s\n',...
        char(session.getAdminService().getEventContext().userName),...
        char(session.getAdminService().getEventContext().groupName));
    
    % Information to edit
    imageId = str2double(client.getProperty('image.id'));
    datasetId = str2double(client.getProperty('dataset.id'));
    
    % First retrieve the image.
    fprintf(1, 'Reading image: %g\n', imageId);
    image = getImages(session, imageId);
    assert(~isempty(image), 'OMERO:CreateImage', 'Image id not valid');
    
    % Read the dimensions
    pixels = image.getPrimaryPixels();
    sizeX = pixels.getSizeX().getValue();
    sizeY = pixels.getSizeY().getValue();
    sizeZ = pixels.getSizeZ().getValue(); % The number of z-sections.
    sizeT = pixels.getSizeT().getValue(); % The number of timepoints.
    sizeC = pixels.getSizeC().getValue(); % The number of channels.
    assert(sizeC > 1,'OMERO:CreateImage', 'Image must contain at least 2 channels');

    pixelsId = pixels.getId().getValue();
    store = session.createRawPixelsStore();
    store.setPixelsId(pixelsId, false);
    
    % REad the raw data
    disp('Reading planes');
    map = java.util.LinkedHashMap;
    for z = 0:sizeZ-1,
       for t = 0:sizeT-1,
             planeC1 = store.getPlane(z, 0, t);
             map.put(linearize(z, t, sizeZ), planeC1);
       end
    end
    
    % Close to free space.
    store.close();
    
    pixelsService = session.getPixelsService();
    l = pixelsService.getAllEnumerations(omero.model.PixelsType.class);
    original = pixels.getPixelsType().getValue().getValue();
    for j = 0:l.size()-1,
        type = l.get(j);
        if (type.getValue().getValue() == original)
            break;
        end
    end
    
    % Create a new image
    disp('Uploading image onto the server');
    description = char(['Source Image ID: ' int2str(image.getId().getValue())]);
    name = char(['newImageFrom' int2str(image.getId().getValue())]);
    idNew = pixelsService.createImage(sizeX, sizeY, sizeZ, sizeT, java.util.Arrays.asList(java.lang.Integer(0)), type, name, description);
    
    %load the image.
    disp('Checking the created image');
    imageNew = getImages(session, idNew.getValue());
    assert(~isempty(imageNew), 'OMERO:CreateImage', 'Image Id not valid');

    % load the dataset
    fprintf(1, 'Reading dataset: %g\n', datasetId);
    dataset = getDatasets(session, datasetId, false);
    assert(~isempty(dataset), 'OMERO:CreateImage', 'Dataset Id not valid');
    
    % Link the new image to the dataset
    fprintf(1, 'Linking image %g to dataset %g\n', idNew.getValue(), datasetId);
    link = omero.model.DatasetImageLinkI;
    link.setChild(omero.model.ImageI(imageNew.getId().getValue(), false));
    link.setParent(omero.model.DatasetI(dataset.getId().getValue(), false));
    session.getUpdateService().saveAndReturnObject(link);
    
    % Copy the data.
    fprintf(1, 'Copying data to image %g\n', idNew.getValue());
    pixelsNew = imageNew.getPrimaryPixels();
    pixelsNewId = pixelsNew.getId().getValue();
    store = session.createRawPixelsStore();
    store.setPixelsId(pixelsNewId, false);
    
    for z = 0:sizeZ-1,
       for t = 0:sizeT-1,
             index = linearize(z, t, sizeZ);
             store.setPlane(map.get(index), z, 0, t);
       end
    end
    store.save(); %save the data
    store.close(); %close
catch err
    disp(err.message);
end

%Close the session
client.closeSession();