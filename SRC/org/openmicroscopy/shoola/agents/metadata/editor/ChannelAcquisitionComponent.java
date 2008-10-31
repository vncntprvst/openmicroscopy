/*
 * org.openmicroscopy.shoola.agents.metadata.editor.ChannelAcquisitionComponent 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2008 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.agents.metadata.editor;


//Java imports
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;


//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.util.EditorUtil;
import org.openmicroscopy.shoola.env.data.model.Mapper;
import org.openmicroscopy.shoola.util.ui.NumericalTextField;
import org.openmicroscopy.shoola.util.ui.OMETextArea;
import org.openmicroscopy.shoola.util.ui.UIUtilities;
import pojos.ChannelData;

/** 
 * Displays the metadata related to the channel.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since 3.0-Beta4
 */
class ChannelAcquisitionComponent
	extends JPanel
{

	/** The channel data. */
	private ChannelData 			channel;
	
	/** The component displaying the illumination's options. */
	private JComboBox				illuminationBox;
	
	/** The component displaying the contrast Method options. */
	private JComboBox				contrastMethodBox;
	
	/** The component displaying the contrast Method options. */
	private JComboBox				modeBox;
	
	/** The component displaying the binning options. */
	private JComboBox				binningBox;
	
	/** The component displaying the binning options. */
	private JComboBox				detectorBox;
	
	/** The fields displaying the metadata. */
	private Map<String, JComponent> fields;
	
	/** Initializes the components */
	private void initComponents()
	{
		illuminationBox = EditorUtil.createComboBox(Mapper.ILLUMINATION);
		contrastMethodBox = EditorUtil.createComboBox(Mapper.CONSTRAST_METHOD);
		modeBox = EditorUtil.createComboBox(Mapper.MODE);
		binningBox = EditorUtil.createComboBox(Mapper.BINNING);
		detectorBox = EditorUtil.createComboBox(Mapper.DETECTOR_TYPE);
		fields = new HashMap<String, JComponent>();
	}
	
	/**
	 * Builds and lays out the body displaying the channel info.
	 * 
	 * @param details The data to lay out.
	 * @return See above.
	 */
    private JPanel buildDetector(Map<String, Object> details)
    {
        JPanel content = new JPanel();
        content.setBorder(BorderFactory.createTitledBorder("Detector"));
        content.setBackground(UIUtilities.BACKGROUND_COLOR);
        content.setLayout(new GridBagLayout());
        //content.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 2, 2, 0);
        Iterator i = details.keySet().iterator();
        JLabel label;
        JComponent area;
        String key;
        Object value;
        label = new JLabel();
        Font font = label.getFont();
        int sizeLabel = font.getSize()-2;
        while (i.hasNext()) {
            ++c.gridy;
            c.gridx = 0;
            key = (String) i.next();
            value = details.get(key);
            label = UIUtilities.setTextFont(key, Font.BOLD, sizeLabel);
            label.setBackground(UIUtilities.BACKGROUND_COLOR);
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE;      //reset to default
            c.weightx = 0.0;  
            content.add(label, c);
            if (key.equals(EditorUtil.BINNING)) {
            	area = binningBox;
            } else if (key.equals(EditorUtil.TYPE)) {
            	area = detectorBox;
            } else if (value instanceof Number) {
            	area = UIUtilities.createComponent(NumericalTextField.class, 
            			null);
            	if (value instanceof Double) 
            		((NumericalTextField) area).setNumberType(Double.class);
            	else if (value instanceof Float) 
            		((NumericalTextField) area).setNumberType(Float.class);
            	((NumericalTextField) area).setText(""+value);
            	((NumericalTextField) area).setEditedColor(
            			UIUtilities.EDITED_COLOR);
            } else {
            	area = UIUtilities.createComponent(OMETextArea.class, null);
            	if (value == null || value.equals("")) 
                	value = AnnotationUI.DEFAULT_TEXT;
            	 ((OMETextArea) area).setEditable(false);
            	 ((OMETextArea) area).setText((String) value);
            	 ((OMETextArea) area).setEditedColor(UIUtilities.EDITED_COLOR);
            }
            
            label.setLabelFor(area);
            c.gridx = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;     //end row
            //c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            content.add(area, c);  
            fields.put(key, area);
        }
        return content;
    }
    
	 /**
     * Builds and lays out the body displaying the channel info.
     * 
     * @param details The data to lay out.
     * @return See above.
     */
    private JPanel buildChannelInfo(Map<String, Object> details)
    {
        JPanel content = new JPanel();
        content.setBorder(BorderFactory.createTitledBorder("Info"));
        content.setBackground(UIUtilities.BACKGROUND_COLOR);
        content.setLayout(new GridBagLayout());
        //content.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 2, 2, 0);
        Iterator i = details.keySet().iterator();
        JLabel label;
        JComponent area;
        String key;
        Object value;
        label = new JLabel();
        Font font = label.getFont();
        int sizeLabel = font.getSize()-2;
        while (i.hasNext()) {
            ++c.gridy;
            c.gridx = 0;
            key = (String) i.next();
            value = details.get(key);
            label = UIUtilities.setTextFont(key, Font.BOLD, sizeLabel);
            label.setBackground(UIUtilities.BACKGROUND_COLOR);
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE;      //reset to default
            c.weightx = 0.0;  
            content.add(label, c);
            
            if (key.equals(EditorUtil.ILLUMINATION)) {
            	area = illuminationBox;
            } else if (key.equals(EditorUtil.CONTRAST_METHOD)) {
            	area = contrastMethodBox;
            } else if (key.equals(EditorUtil.MODE)) {
            	area = modeBox;
            } else {
            	if (value instanceof Number) {
            		area = UIUtilities.createComponent(
             				 NumericalTextField.class, null);
            		if (value instanceof Double) 
                		((NumericalTextField) area).setNumberType(Double.class);
                	else if (value instanceof Float) 
            			((NumericalTextField) area).setNumberType(Float.class);
             		 ((NumericalTextField) area).setText(""+value);
             		((NumericalTextField) area).setEditedColor(
             				UIUtilities.EDITED_COLOR);
            	} else {
            		area = UIUtilities.createComponent(OMETextArea.class, null);
                	if (value == null || value.equals(""))
                    	value = AnnotationUI.DEFAULT_TEXT;
                	((OMETextArea) area).setEditable(false);
                	((OMETextArea) area).setText((String) value);
                	((OMETextArea) area).setEditedColor(
                			UIUtilities.EDITED_COLOR);
            	}
            }
            
            label.setLabelFor(area);
            c.gridx = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;     //end row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            content.add(area, c);  
            fields.put(key, area);
        }
        return content;
    }
    
    /** Builds and lays out the UI. */
    private void buildGUI()
    {
    	fields.clear();
    	setBackground(UIUtilities.BACKGROUND_COLOR);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    	add(buildChannelInfo(EditorUtil.transformChannelData(channel)));
    	add(buildDetector(EditorUtil.transformDectector(null, null)));
    }
    
	/**
	 * Creates a new instance.
	 * 
	 * @param channel The channel to display.
	 */
	ChannelAcquisitionComponent(ChannelData channel)
	{
		this.channel = channel;
		initComponents();
		buildGUI();
	}
	
}
