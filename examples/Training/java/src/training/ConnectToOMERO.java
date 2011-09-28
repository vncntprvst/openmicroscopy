/*
 * training.ConnectToOMERO 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2011 University of Dundee & Open Microscopy Environment.
 *  All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package training;


//Java imports
import java.util.ArrayList;
import java.util.List;

//Third-party libraries

//Application-internal dependencies
import omero.client;
import omero.api.IContainerPrx;
import omero.api.ServiceFactoryPrx;
import omero.model.Image;
import omero.sys.ParametersI;
import pojos.ImageData;

/** 
 * Sample code showing how to connect to an OMERO server.
 *
 * @author Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since 3.0-Beta4
 */
public class ConnectToOMERO {

	/** The server address.*/
	private String hostName = "server";
	
	/** The port to use.*/
	private int port = 4064; //default port
	
	/** The username.*/
	private String userName = "name";
	
	/** The password.*/
	private String password = "password";
	
	/** Reference to the client.*/
	protected client client;
	
	/** The service factory.*/
	protected ServiceFactoryPrx entryUnencrypted;
	
	protected void connect()
		throws Exception
	{
		client = new client(hostName, port);
		ServiceFactoryPrx entry = client.createSession(userName, password);
		// if you want to have the data transfer encrypted then you can 
		// use the entry variable otherwise use the following 
		client unsecureClient = client.createClient(false);
		entryUnencrypted = unsecureClient.getSession();
		
		long userId = entryUnencrypted.getAdminService().getEventContext().userId;
		
		long groupId = entryUnencrypted.getAdminService().getEventContext().groupId;
	}
	
	/** Load the image.*/
	protected ImageData loadImage(long imageId)
		throws Exception
	{
		IContainerPrx proxy = entryUnencrypted.getContainerService();
		List<Long> ids = new ArrayList<Long>();
		ids.add(imageId);
		List<Image> results = proxy.getImages(Image.class.getName(), ids, 
				new ParametersI());
		//You can directly interact with the IObject or the Pojos object.
		//Follow interaction with the Pojos.
		if (results.size() == 0) return null;
		return new ImageData(results.get(0));
	}
	
	/**
	 * Shows how to connect to omero.
	 */
	ConnectToOMERO()
	{
		try {
			connect();
			client.closeSession();
		} catch (Exception e) {
			if (client != null) client.closeSession();
		}
		
		
	}
	
	public static void main(String[] args) 
	{
		new ConnectToOMERO();
	}
	
}
