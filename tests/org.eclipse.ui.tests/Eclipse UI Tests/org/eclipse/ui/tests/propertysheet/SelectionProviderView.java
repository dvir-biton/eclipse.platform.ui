package org.eclipse.ui.tests.propertysheet;

import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
/**
 *  The view provides api to fire a selection event for its workbench page. 
 */

public class SelectionProviderView
	extends ViewPart
	implements ISelectionProvider {
	private ListenerList selectionChangedListeners = new ListenerList();
	private StructuredSelection selection = StructuredSelection.EMPTY;
	
	public final static String ID = "org.eclipse.ui.tests.propertysheet.SelectionProviderView";

	private Text text;
	public SelectionProviderView() {
		super();
	}
	/**
	 * @see WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		text.setFocus();
	}

	/**
	 * @see WorkbenchPart#createPartControl(Composite)
	 */
	public void createPartControl(Composite parent) {
		text = new Text(parent, SWT.MULTI | SWT.WRAP);
		text.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	/* (non-Javadoc)
	 * Method declared on IViewPart.
	 */
	public void init(IViewSite site) throws PartInitException {
		site.setSelectionProvider(this);
		super.init(site);
	}


	/**
	 * @see ISelectionProvider#addSelectionChangedListener(ISelectionChangedListener)
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.add(listener);
	}

	/**
	 * @see ISelectionProvider#getSelection()
	 */
	public ISelection getSelection() {
		return selection;
	}
	/**
	 * @see ISelectionProvider#removeSelectionChangedListener(ISelectionChangedListener)
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.remove(listener);
	}

	/**
	 * @see ISelectionProvider#setSelection(ISelection)
	 */
	public void setSelection(ISelection selection) {
		// create an event
		SelectionChangedEvent event = new SelectionChangedEvent(this, selection);

		// write text
		text.setText(selection.toString());

		// fire the event
		Object[] listeners = selectionChangedListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			((ISelectionChangedListener) listeners[i]).selectionChanged(event);
		}
	}
}