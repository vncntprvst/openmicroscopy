/*
 * org.openmicroscopy.shoola.agents.hiviewer.view.HiViewerComponent
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2004 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.agents.hiviewer.view;


//Java imports
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;
import javax.swing.JFrame;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.hiviewer.HiTranslator;
import org.openmicroscopy.shoola.agents.hiviewer.HiViewerAgent;
import org.openmicroscopy.shoola.agents.hiviewer.actions.SortByAction;
import org.openmicroscopy.shoola.agents.hiviewer.browser.Browser;
import org.openmicroscopy.shoola.agents.hiviewer.browser.ImageDisplay;
import org.openmicroscopy.shoola.agents.hiviewer.browser.ImageDisplayVisitor;
import org.openmicroscopy.shoola.agents.hiviewer.clipboard.ClipBoard;
import org.openmicroscopy.shoola.agents.hiviewer.cmd.FindAnnotatedVisitor;
import org.openmicroscopy.shoola.agents.hiviewer.layout.Layout;
import org.openmicroscopy.shoola.agents.hiviewer.layout.LayoutFactory;
import org.openmicroscopy.shoola.agents.hiviewer.treeview.TreeView;
import org.openmicroscopy.shoola.env.ui.UserNotifier;
import org.openmicroscopy.shoola.util.ui.component.AbstractComponent;
import pojos.DataObject;
import pojos.ExperimenterData;
import pojos.ImageData;

/** 
 * Implements the {@link HiViewer} interface to provide the functionality
 * required of the hierarchy viewer component.
 * This class is the component hub and embeds the component's MVC triad.
 * It manages the component's state machine and fires state change 
 * notifications as appropriate, but delegates actual functionality to the
 * MVC sub-components.
 *
 * @see org.openmicroscopy.shoola.agents.hiviewer.view.HiViewerModel
 * @see org.openmicroscopy.shoola.agents.hiviewer.view.HiViewerWin
 * @see org.openmicroscopy.shoola.agents.hiviewer.view.HiViewerControl
 * 
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author  <br>Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">
 * 					a.falconi@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
class HiViewerComponent
    extends AbstractComponent
    implements HiViewer
{

    /** The Model sub-component. */
    private HiViewerModel   model;
    
    /** The View sub-component. */
    private HiViewerWin     view;
    
    /** The Controller sub-component. */
    private HiViewerControl controller;
    
    
    /**
     * Creates a new instance.
     * The {@link #initialize() initialize} method should be called straigh 
     * after to complete the MVC set up.
     * 
     * @param model The Model sub-component.
     */
    HiViewerComponent(HiViewerModel model)
    {
        if (model == null) throw new NullPointerException("No model.");
        this.model = model;
        controller = new HiViewerControl(this);
        view = new HiViewerWin();
    }
    
    /** Links up the MVC triad. */
    void initialize()
    {
        model.initialize(this);
        controller.initialize(view);
        view.initialize(controller, model);
    }
    
    /**
     * Returns the Model sub-component.
     * 
     * @return See above.
     */
    HiViewerModel getModel() { return model; }
    
    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#getState()
     */
    public int getState() { return model.getState(); }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#getHierarchyType()
     */
    public int getHierarchyType() { return model.getHierarchyType(); }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#activate(Rectangle)
     */
    public void activate(Rectangle bounds)
    {
        switch (model.getState()) {
            case NEW:
                model.fireHierarchyLoading();
                view.setComponentBounds(bounds);
                //view.setOnScreen();
                fireStateChange();
                break;
            case DISCARDED:
                throw new IllegalStateException(
                        "This method can't be invoked in the DISCARDED state.");
            default:
                view.deIconify();
        }
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#setHierarchyRoots(Set, boolean)
     */
    public void setHierarchyRoots(Set roots, boolean flat)
    {
        if (model.getState() != LOADING_HIERARCHY)
            throw new IllegalStateException(
                    "This method can only be invoked in the LOADING_HIERARCHY "+
                    "state.");
        
        model.createBrowser(roots, flat);
        model.createClipBoard();
        model.getClipBoard().addPropertyChangeListener(controller);
        model.fireThumbnailLoading();
        //b/c fireThumbnailLoading() sets the state to READY if there is no
        //image.
        if (model.getBrowser().getImages().size() == 0) setStatus("Done", -1);
        else setStatus(HiViewer.PAINTING_TEXT, -1);
        fireStateChange();
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#setThumbnail(long, BufferedImage)
     */
    public void setThumbnail(long imageID, BufferedImage thumb)
    {
        int state = model.getState();
        switch (state) {
            case LOADING_THUMBNAILS:
                model.setThumbnail(imageID, thumb);
                //setThumbnail will set state to READY when done.
                if (model.getState() == READY) fireStateChange();
                break;
            case READY:
                model.setThumbnail(imageID, thumb);
                break;
            default:
                throw new IllegalStateException(
                   "This method can only be invoked in the LOADING_THUMBNAILS "+
                        "or READY state.");
        }
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#setStatus(String, int)
     */
    public void setStatus(String description, int perc)
    {
        int state = model.getState();
        if (state == LOADING_HIERARCHY || state == LOADING_THUMBNAILS)
            view.setStatus(description, false, perc);
        else view.setStatus(description, true, perc);
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#getBrowser()
     */
    public Browser getBrowser()
    {
        int state = model.getState();
        switch (state) {
            case LOADING_THUMBNAILS:
            case READY:
                return model.getBrowser();
            default:
                throw new IllegalStateException(
                   "This method can only be invoked in the LOADING_THUMBNAILS "+
                        "or READY state.");
        }
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#discard()
     */
    public void discard()
    { 
        if (model.getState() != DISCARDED) {
            model.discard();
            fireStateChange();
        }
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#getUI()
     */
    public JFrame getUI()
    { 
        if (model.getState() == DISCARDED)
            throw new IllegalStateException(
                    "This method cannot be invoked in the DISCARDED state.");
        return view;
    }
     
    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#getViewTitle()
     */
    public String getViewTitle()
    {
        int state = model.getState();
        switch (state) {
            case LOADING_THUMBNAILS:
            case READY:
                return view.getViewTitle();
            default:
                throw new IllegalStateException(
                   "This method can only be invoked in the LOADING_THUMBNAILS "+
                        "or READY state.");
        }       
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#getClipBoard()
     */
    public ClipBoard getClipBoard()
    {
        int state = model.getState();
        switch (state) {
            case LOADING_THUMBNAILS:
            case READY:
                return model.getClipBoard();
            default:
                throw new IllegalStateException(
                   "This method can only be invoked in the LOADING_THUMBNAILS "+
                        "or READY state.");
        }
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#getUserDetails()
     */
    public ExperimenterData getUserDetails()
    {
        return model.getUserDetails();
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#showTreeView(boolean)
     */
    public void showTreeView(boolean b)
    {
        int state = model.getState();
        if (state != LOADING_THUMBNAILS && state != READY)
            throw new IllegalStateException(
                   "This method can only be invoked in the LOADING_THUMBNAILS "+
                        "or READY state.");
        if (getBrowser() == null) return;
        TreeView treeView = model.getTreeView();
        if (treeView == null) {
            model.createTreeView();
            treeView = model.getTreeView();
            treeView.addPropertyChangeListener(controller);
        }
        if (treeView.isDisplay() == b) return;
        treeView.setDisplay(b);
        view.showTreeView(b);
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#showClipBoard(boolean)
     */
    public void showClipBoard(boolean b)
    {
        int state = model.getState();
        if (state != LOADING_THUMBNAILS && state != READY)
            throw new IllegalStateException(
                   "This method can only be invoked in the LOADING_THUMBNAILS "+
                        "or READY state.");
        ClipBoard cb = model.getClipBoard();
        if (cb == null) return;
        if (cb.isDisplay() == b) return;
        cb.setDisplay(b);
        view.showClipBoard(b);
    }
    
    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#getTreeView()
     */
    public TreeView getTreeView()
    {
        int state = model.getState();
        if (state != LOADING_THUMBNAILS && state != READY)
            throw new IllegalStateException(
                   "This method can only be invoked in the LOADING_THUMBNAILS "+
                        "or READY state.");
        TreeView treeView = model.getTreeView();
        if (treeView == null) {
            model.createTreeView();
            treeView = model.getTreeView();
            treeView.addPropertyChangeListener(controller);
        }
        return treeView;
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#getRootLevel()
     */
    public Class getRootLevel()
    {
        if (model.getState() == DISCARDED)
            throw new IllegalStateException(
                    "This method cannot be invoked in the DISCARDED state.");
        return model.getRootLevel();
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#getRootID()
     */
    public long getRootID()
    {
        if (model.getState() == DISCARDED)
            throw new IllegalStateException(
                    "This method cannot be invoked in the DISCARDED state.");
        return model.getRootID();
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#moveToBack()
     */
    public void moveToBack()
    {
        if (model.getState() == DISCARDED)
            throw new IllegalStateException(
                    "This method cannot be invoked in the DISCARDED state.");
        view.toBack();
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#getHierarchyObject()
     */
    public Object getHierarchyObject()
    {
        if (model.getState() == DISCARDED)
            throw new IllegalStateException(
                    "This method cannot be invoked in the DISCARDED state.");
        ImageDisplay node = model.getBrowser().getLastSelectedDisplay();
        if (node == null) return null;
        return node.getHierarchyObject();
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#setAnnotationEdition(DataObject)
     */
    public void setAnnotationEdition(DataObject ho)
    {
        if (model.getState() == DISCARDED)
            throw new IllegalStateException(
                    "This method cannot be invoked in the DISCARDED state.");
        if (ho == null) throw new IllegalArgumentException("No DataObject.");
        Browser browser = model.getBrowser();
        FindAnnotatedVisitor visitor = new FindAnnotatedVisitor(this, ho);
        if (ho instanceof ImageData)
            browser.accept(visitor, ImageDisplayVisitor.IMAGE_NODE_ONLY);
        else browser.accept(visitor, ImageDisplayVisitor.IMAGE_SET_ONLY);
        model.getTreeView().repaint();
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#scrollToNode(ImageDisplay)
     */
    public void scrollToNode(ImageDisplay node)
    {
        if (model.getState() == DISCARDED)
            throw new IllegalStateException(
                    "This method cannot be invoked in the DISCARDED state.");
        if (node == null) throw new IllegalArgumentException("No node.");
        controller.scrollToNode(node);
    }
    
    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#isObjectReadable(DataObject)
     */
    public boolean isObjectReadable(DataObject ho)
    {
        if (model.getState() == DISCARDED)
            throw new IllegalStateException(
            "This method cannot be invoked in the DISCARDED state.");
        return HiTranslator.isReadable(ho, getUserDetails().getId(), 
                                            getRootID());
    }
    
    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#isObjectWritable(DataObject)
     */
    public boolean isObjectWritable(DataObject ho)
    {
        if (model.getState() == DISCARDED)
            throw new IllegalStateException(
            "This method cannot be invoked in the DISCARDED state.");
        return HiTranslator.isWritable(ho, getUserDetails().getId(), 
                                        getRootID());
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#saveObject(DataObject)
     */
    public void saveObject(DataObject object)
    {
        if (object == null)
            throw new IllegalArgumentException("No object to update");
        //TODO check state.
        model.fireDataObjectUpdate(object);
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#showProperties(DataObject)
     */
    public void showProperties(DataObject object)
    {
        if (object == null)
            throw new IllegalArgumentException("No object to update");
        model.getClipBoard().showProperties(object);
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#setRollOver(boolean)
     */
    public void setRollOver(boolean b)
    {
        if (model.getState() == DISCARDED)
            throw new IllegalStateException(
            "This method cannot be invoked in the DISCARDED state.");
        model.setRollOver(b);
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#isRollOver()
     */
    public boolean isRollOver()
    {
        if (model.getState() == DISCARDED)
            throw new IllegalStateException(
            "This method cannot be invoked in the DISCARDED state.");
        return model.isRollOver();
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#removeObjects(List)
     */
    public void removeObjects(List toRemove)
    {
        //TODO: Check state
        if (toRemove == null || toRemove.size() == 0) {
            UserNotifier un = 
                HiViewerAgent.getRegistry().getUserNotifier();
            un.notifyInfo("Node remoal", "No nodes to remove.");
            return;
        }
        model.fireDataObjectsRemoval(toRemove);
        fireStateChange();
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#setLayout(int)
     */
    public void setLayout(int layoutIndex)
    {
        view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Browser browser = getBrowser();
        switch (layoutIndex) {
            case LayoutFactory.SQUARY_LAYOUT:
                browser.setSelectedLayout(layoutIndex);
                browser.resetChildDisplay();
                browser.accept(LayoutFactory.createLayout(
                        LayoutFactory.SQUARY_LAYOUT, model.getSorter()),
                        ImageDisplayVisitor.IMAGE_SET_ONLY);
                break;
            case LayoutFactory.FLAT_LAYOUT:
                browser.setSelectedLayout(layoutIndex);
                browser.resetChildDisplay();
                Layout l = LayoutFactory.createLayout(layoutIndex, 
                                                        model.getSorter());
                browser.accept(l);
                browser.setSelectedLayout(LayoutFactory.FLAT_LAYOUT);
                l.doLayout();
        }
        view.setCursor(
                Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#sortBy(int)
     */
    public void sortBy(int index)
    {
        model.getSorter().setByDate(SortByAction.BY_DATE == index);
        view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Browser browser = getBrowser();
        switch (browser.getSelectedLayout()) {
            case LayoutFactory.SQUARY_LAYOUT:
                browser.accept(LayoutFactory.createLayout(
                        LayoutFactory.SQUARY_LAYOUT, model.getSorter()),
                        ImageDisplayVisitor.IMAGE_SET_ONLY);
                break;
            case LayoutFactory.FLAT_LAYOUT:
                browser.resetChildDisplay();
                Layout l = LayoutFactory.createLayout(LayoutFactory.FLAT_LAYOUT, 
                                                        model.getSorter());
                browser.accept(l);
                l.doLayout();
        }
        TreeView tv = model.getTreeView();
        if (tv != null) 
            tv.sortNodes(index, (ImageDisplay) model.getBrowser().getUI());
        view.setCursor(
                Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    
    /**
     * Implemented as specified by the {@link HiViewer} interface.
     * @see HiViewer#layoutZoomedNodes()
     */
    public void layoutZoomedNodes()
    {
        view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Browser browser = getBrowser();
        ImageDisplay d = browser.getLastSelectedDisplay();
        switch (browser.getSelectedLayout()) {
            case LayoutFactory.SQUARY_LAYOUT:
                d.accept(LayoutFactory.createLayout(
                        LayoutFactory.SQUARY_LAYOUT, model.getSorter()),
                        ImageDisplayVisitor.IMAGE_SET_ONLY);
                break;
            case LayoutFactory.FLAT_LAYOUT:
            	browser.resetChildDisplay();
                Layout l = LayoutFactory.createLayout(LayoutFactory.FLAT_LAYOUT, 
                                                        model.getSorter());
                browser.accept(l);
                l.doLayout();
        }
        view.setCursor(
                Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    
}
