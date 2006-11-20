/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.texteditor;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import com.ibm.icu.text.MessageFormat;

import org.eclipse.osgi.util.NLS;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.commands.operations.IOperationApprover;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IFileBufferStatusCodes;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension6;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.revisions.IRevisionRulerColumn;
import org.eclipse.jface.text.revisions.IRevisionRulerColumnExtension;
import org.eclipse.jface.text.revisions.RevisionInformation;
import org.eclipse.jface.text.revisions.IRevisionRulerColumnExtension.RenderingMode;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationRulerColumn;
import org.eclipse.jface.text.source.ChangeRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationAccessExtension2;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IChangeRulerColumn;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension;
import org.eclipse.jface.text.source.ISourceViewerExtension3;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.text.source.LineChangeHover;
import org.eclipse.jface.text.source.LineNumberChangeRulerColumn;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.OverviewRuler;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.editors.text.DefaultEncodingSupport;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.ForwardingDocumentProvider;
import org.eclipse.ui.editors.text.IEncodingSupport;
import org.eclipse.ui.editors.text.ITextEditorHelpContextIds;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.internal.editors.quickdiff.CompositeRevertAction;
import org.eclipse.ui.internal.editors.quickdiff.RestoreAction;
import org.eclipse.ui.internal.editors.quickdiff.RevertBlockAction;
import org.eclipse.ui.internal.editors.quickdiff.RevertLineAction;
import org.eclipse.ui.internal.editors.quickdiff.RevertSelectionAction;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.internal.editors.text.JavaFileEditorInput;
import org.eclipse.ui.internal.editors.text.NLSUtility;
import org.eclipse.ui.internal.texteditor.AnnotationColumn;
import org.eclipse.ui.internal.texteditor.BooleanPreferenceToggleAction;
import org.eclipse.ui.internal.texteditor.LineNumberColumn;
import org.eclipse.ui.internal.texteditor.TextChangeHover;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.operations.NonLocalUndoUserApprover;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.rulers.IColumnSupport;
import org.eclipse.ui.texteditor.rulers.IContributedRulerColumn;
import org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor;
import org.eclipse.ui.texteditor.rulers.RulerColumnPreferenceAdapter;
import org.eclipse.ui.texteditor.rulers.RulerColumnRegistry;
import org.eclipse.ui.views.markers.MarkerViewUtil;

/**
 * An intermediate editor comprising functionality not present in the leaner <code>AbstractTextEditor</code>,
 * but used in many heavy weight (and especially source editing) editors, such as line numbers,
 * change ruler, overview ruler, print margins, current line highlighting, etc.
 *
 * @since 3.0
 */
public abstract class AbstractDecoratedTextEditor extends StatusTextEditor {

	/**
	 * Preference key for showing the line number ruler.
	 */
	private final static String LINE_NUMBER_RULER= AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER;
	/**
	 * Preference key for showing the overview ruler.
	 */
	private final static String OVERVIEW_RULER= AbstractDecoratedTextEditorPreferenceConstants.EDITOR_OVERVIEW_RULER;
	/**
	 * Preference key for highlighting current line.
	 */
	private final static String CURRENT_LINE= AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE;
	/**
	 * Preference key for highlight color of current line.
	 */
	private final static String CURRENT_LINE_COLOR= AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR;
	/**
	 * Preference key for showing print margin ruler.
	 */
	private final static String PRINT_MARGIN= AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN;
	/**
	 * Preference key for print margin ruler color.
	 */
	private final static String PRINT_MARGIN_COLOR= AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR;
	/**
	 * Preference key for print margin ruler column.
	 */
	private final static String PRINT_MARGIN_COLUMN= AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN;
	/**
	 * Preference key to get whether the overwrite mode is disabled.
	 * @since 3.1
	 */
	private final static String DISABLE_OVERWRITE_MODE= AbstractDecoratedTextEditorPreferenceConstants.EDITOR_DISABLE_OVERWRITE_MODE;

	/**
	 * Adapter class for <code>IGotoMarker</code>.
	 */
	private class GotoMarkerAdapter implements IGotoMarker {
		public void gotoMarker(IMarker marker) {
			AbstractDecoratedTextEditor.this.gotoMarker(marker);
		}
	}

	/**
	 * The annotation preferences.
	 */
	private MarkerAnnotationPreferences fAnnotationPreferences;
	/**
	 * The overview ruler of this editor.
	 *
	 * <p>This field should not be referenced by subclasses. It is <code>protected</code> for API
	 * compatibility reasons and will be made <code>private</code> soon. Use
	 * {@link #getOverviewRuler()} instead.</p>
	 */
	protected IOverviewRuler fOverviewRuler;
	/**
	 * Helper for accessing annotation from the perspective of this editor.
	 *
	 * <p>This field should not be referenced by subclasses. It is <code>protected</code> for API
	 * compatibility reasons and will be made <code>private</code> soon. Use
	 * {@link #getAnnotationAccess()} instead.</p>
	 */
	protected IAnnotationAccess fAnnotationAccess;
	/**
	 * Helper for managing the decoration support of this editor's viewer.
	 *
	 * <p>This field should not be referenced by subclasses. It is <code>protected</code> for API
	 * compatibility reasons and will be made <code>private</code> soon. Use
	 * {@link #getSourceViewerDecorationSupport(ISourceViewer)} instead.</p>
	 */
	protected SourceViewerDecorationSupport fSourceViewerDecorationSupport;
	/**
	 * The line number column.
	 *
	 * <p>This field should not be referenced by subclasses. It is <code>protected</code> for API
	 * compatibility reasons and will be made <code>private</code> soon. Use
	 * {@link AbstractTextEditor#getVerticalRuler()} to access the vertical bar instead.</p>
	 */
	protected LineNumberRulerColumn fLineNumberRulerColumn;
	/**
	 * The delegating line number ruler contribution.
	 * @since 3.3
	 */
	private LineNumberColumn fLineColumn;
	/**
	 * The editor's implicit document provider.
	 */
	private IDocumentProvider fImplicitDocumentProvider;
	/**
	 * The editor's goto marker adapter.
	 */
	private Object fGotoMarkerAdapter= new GotoMarkerAdapter();
	/**
	 * Indicates whether this editor is updating views that show markers.
	 * @see #updateMarkerViews(Annotation)
	 * @since 3.2
	 */
	protected boolean fIsUpdatingMarkerViews= false;
	/**
	 * Tells whether editing the current derived editor input is allowed.
	 * @since 3.3
	 */
	private boolean fIsEditingDerivedFileAllowed= true;
	/**
	 * Tells whether the derived state has been validated.
	 * @since 3.3
	 */
	private boolean fIsDerivedStateValidated= false;

	
	/*
	 * Workaround for IllegalAccessError thrown because we are accessing
	 * a protected method in a different bundle from an inner class.
	 */
	private IVerticalRuler internalGetVerticalRuler() {
		return getVerticalRuler();
	}
	
	/**
	 * Creates a new text editor.
	 */
	public AbstractDecoratedTextEditor() {
		super();
		fAnnotationPreferences= EditorsPlugin.getDefault().getMarkerAnnotationPreferences();
		setRangeIndicator(new DefaultRangeIndicator());
		initializeKeyBindingScopes();
		initializeEditor();
	}

	/**
	 * Initializes this editor. Subclasses may re-implement. If sub-classes do
	 * not change the contract, this method should not be extended, i.e. do not
	 * call <code>super.initializeEditor()</code> in order to avoid the
	 * temporary creation of objects that are immediately overwritten by
	 * subclasses.
	 */
	protected void initializeEditor() {
		setPreferenceStore(EditorsPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * Initializes the key binding scopes of this editor.
	 */
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "org.eclipse.ui.textEditorScope" });  //$NON-NLS-1$
	}

	/*
	 * @see IWorkbenchPart#dispose()
	 */
	public void dispose() {
		if (fSourceViewerDecorationSupport != null) {
			fSourceViewerDecorationSupport.dispose();
			fSourceViewerDecorationSupport= null;
		}

		fAnnotationAccess= null;
		fAnnotationPreferences= null;
		
		super.dispose();
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#createSourceViewer(Composite, IVerticalRuler, int)
	 */
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {

		fAnnotationAccess= getAnnotationAccess();
		fOverviewRuler= createOverviewRuler(getSharedColors());

		ISourceViewer viewer= new SourceViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
		// ensure decoration support has been created and configured.
		getSourceViewerDecorationSupport(viewer);

		return viewer;
	}

	protected ISharedTextColors getSharedColors() {
		ISharedTextColors sharedColors= EditorsPlugin.getDefault().getSharedTextColors();
		return sharedColors;
	}

	protected IOverviewRuler createOverviewRuler(ISharedTextColors sharedColors) {
		IOverviewRuler ruler= new OverviewRuler(getAnnotationAccess(), VERTICAL_RULER_WIDTH, sharedColors);
		Iterator e= fAnnotationPreferences.getAnnotationPreferences().iterator();
		while (e.hasNext()) {
			AnnotationPreference preference= (AnnotationPreference) e.next();
			if (preference.contributesToHeader())
				ruler.addHeaderAnnotationType(preference.getAnnotationType());
		}
		return ruler;
	}

	/**
	 * Creates the annotation access for this editor.
	 *
	 * @return the created annotation access
	 */
	protected IAnnotationAccess createAnnotationAccess() {
		return new DefaultMarkerAnnotationAccess();
	}

	/**
	 * Configures the decoration support for this editor's source viewer. Subclasses may override this
	 * method, but should call their superclass' implementation at some point.
	 *
	 * @param support the decoration support to configure
	 */
	protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {

		Iterator e= fAnnotationPreferences.getAnnotationPreferences().iterator();
		while (e.hasNext())
			support.setAnnotationPreference((AnnotationPreference) e.next());

		support.setCursorLinePainterPreferenceKeys(CURRENT_LINE, CURRENT_LINE_COLOR);
		support.setMarginPainterPreferenceKeys(PRINT_MARGIN, PRINT_MARGIN_COLOR, PRINT_MARGIN_COLUMN);
		support.setSymbolicFontName(getFontPropertyPreferenceKey());
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		if (fSourceViewerDecorationSupport != null)
			fSourceViewerDecorationSupport.install(getPreferenceStore());

		IColumnSupport columnSupport= (IColumnSupport)getAdapter(IColumnSupport.class);
		
		if (isLineNumberRulerVisible()) {
			RulerColumnDescriptor lineNumberColumnDescriptor= RulerColumnRegistry.getDefault().getColumnDescriptor(LineNumberColumn.ID);
			if (lineNumberColumnDescriptor != null)
				columnSupport.setColumnVisible(lineNumberColumnDescriptor, true);
		}

		if (isPrefQuickDiffAlwaysOn())
			showChangeInformation(true);

		if (!isOverwriteModeEnabled())
			enableOverwriteMode(false);

		if (!isRangeIndicatorEnabled()) {
			getSourceViewer().removeRangeIndication();
			getSourceViewer().setRangeIndicator(null);
		}

		// Assign the quick assist assistant to the annotation access.
		ISourceViewer viewer= getSourceViewer();
		if (fAnnotationAccess instanceof IAnnotationAccessExtension2 && viewer instanceof ISourceViewerExtension3)
			((IAnnotationAccessExtension2)fAnnotationAccess).setQuickAssistAssistant(((ISourceViewerExtension3)viewer).getQuickAssistAssistant());
	}

	/*
	 * @see org.eclipse.ui.texteditor.StatusTextEditor#createStatusControl(org.eclipse.swt.widgets.Composite, org.eclipse.core.runtime.IStatus)
	 * @since 3.1
	 */
	protected Control createStatusControl(Composite parent, final IStatus status) {
		Object adapter= getAdapter(IEncodingSupport.class);
		DefaultEncodingSupport encodingSupport= null;
		if (adapter instanceof DefaultEncodingSupport)
			encodingSupport= (DefaultEncodingSupport)adapter;

		if (encodingSupport == null || !encodingSupport.isEncodingError(status))
			return super.createStatusControl(parent, status);

		Shell shell= getSite().getShell();
		Display display= shell.getDisplay();
		Color bgColor= display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		Color fgColor= display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setBackground(bgColor);
		composite.setForeground(fgColor);

		Control control= super.createStatusControl(composite, status);
		control.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite buttonComposite= new Composite(composite, SWT.NONE);
		buttonComposite.setLayout(new GridLayout());
		buttonComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		buttonComposite.setBackground(bgColor);
		buttonComposite.setForeground(fgColor);

		encodingSupport.createStatusEncodingChangeControl(buttonComposite, status);
//
//		Button button= new Button(buttonComposite, SWT.PUSH | SWT.FLAT);
//		button.setText(action.getText());
//		button.addSelectionListener(new SelectionAdapter() {
//			/*
//			 * @see org.eclipse.swt.events.SelectionAdapter#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
//			 */
//			public void widgetSelected(SelectionEvent e) {
//				action.run();
//			}
//		});
//
//		Label filler= new Label(buttonComposite, SWT.NONE);
//		filler.setLayoutData(new GridData(GridData.FILL_BOTH));
//		filler.setBackground(bgColor);
//
		return composite;
	}

	/**
	 * Tells whether the overview ruler is visible.
	 *
	 * @return whether the overview ruler is visible
	 */
	protected boolean isOverviewRulerVisible() {
		IPreferenceStore store= getPreferenceStore();
		return store != null ? store.getBoolean(OVERVIEW_RULER) : false;
	}

	/*
	 * @see org.eclipse.ui.texteditor.ITextEditorExtension3#showChangeInformation(boolean)
	 */
	public void showChangeInformation(boolean show) {
		if (show == isChangeInformationShowing())
			return;

		IColumnSupport columnSupport= (IColumnSupport)getAdapter(IColumnSupport.class);
		
		// only handle visibility of the combined column, but not the number/change only state
		if (show && fLineColumn == null) {
			RulerColumnDescriptor lineNumberColumnDescriptor= RulerColumnRegistry.getDefault().getColumnDescriptor(LineNumberColumn.ID);
			if (lineNumberColumnDescriptor != null)
				columnSupport.setColumnVisible(lineNumberColumnDescriptor, true);
		} else if (!show && fLineColumn != null && !isLineNumberRulerVisible()) {
			columnSupport.setColumnVisible(fLineColumn.getDescriptor(), false);
			fLineColumn= null;
		}
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.ITextEditorExtension3#isChangeInformationShowing()
	 */
	public boolean isChangeInformationShowing() {
		return fLineColumn != null && fLineColumn.isShowingChangeInformation();
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.ITextEditorExtension4#showRevisionInformation(org.eclipse.jface.text.revisions.RevisionInformation, java.lang.String)
	 * @since 3.2
	 */
	public void showRevisionInformation(RevisionInformation info, String quickDiffProviderId) {
		showChangeInformation(true);
		if (fLineColumn != null)
			fLineColumn.showRevisionInformation(info, quickDiffProviderId);
	}

	/**
	 * Returns whether the line number ruler column should be
	 * visible according to the preference store settings. Subclasses may override this
	 * method to provide a custom preference setting.
	 *
	 * @return <code>true</code> if the line numbers should be visible
	 */
	protected boolean isLineNumberRulerVisible() {
		IPreferenceStore store= getPreferenceStore();
		return store != null ? store.getBoolean(LINE_NUMBER_RULER) : false;
	}

	/**
	 * Returns whether the overwrite mode is enabled according to the preference
	 * store settings. Subclasses may override this method to provide a custom
	 * preference setting.
	 *
	 * @return <code>true</code> if overwrite mode is enabled
	 * @since 3.1
	 */
	protected boolean isOverwriteModeEnabled() {
		IPreferenceStore store= getPreferenceStore();
		return store != null ? !store.getBoolean(DISABLE_OVERWRITE_MODE) : true;
	}

	/**
	 * Returns whether the range indicator is enabled according to the preference
	 * store settings. Subclasses may override this method to provide a custom
	 * preference setting.
	 *
	 * @return <code>true</code> if overwrite mode is enabled
	 * @since 3.1
	 */
	private boolean isRangeIndicatorEnabled() {
		IPreferenceStore store= getPreferenceStore();
		return store != null ? store.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.SHOW_RANGE_INDICATOR) : true;
	}
	
	/**
	 * Returns whether quick diff info should be visible upon opening an editor
	 * according to the preference store settings.
	 *
	 * @return <code>true</code> if the line numbers should be visible
	 */
	protected boolean isPrefQuickDiffAlwaysOn() {
		IPreferenceStore store= getPreferenceStore();
		boolean setting= store != null ? store.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.QUICK_DIFF_ALWAYS_ON) : false;
		return setting && isEditorInputModifiable();
	}

	/**
	 * Initializes the given line number ruler column from the preference store.
	 *
	 * @param rulerColumn the ruler column to be initialized
	 */
	protected void initializeLineNumberRulerColumn(LineNumberRulerColumn rulerColumn) {
		/*
		 * Left for compatibility. See LineNumberColumn.
		 */
		if (fLineColumn != null)
			fLineColumn.initializeLineNumberRulerColumn(rulerColumn);
	}

	/**
	 * Creates a new line number ruler column that is appropriately initialized.
	 *
	 * @return the created line number column
	 */
	protected IVerticalRulerColumn createLineNumberRulerColumn() {
		/*
		 * Left for compatibility. See LineNumberColumn.
		 */
		fLineNumberRulerColumn= new LineNumberChangeRulerColumn(getSharedColors());
		((IChangeRulerColumn) fLineNumberRulerColumn).setHover(createChangeHover());
		initializeLineNumberRulerColumn(fLineNumberRulerColumn);
		return fLineNumberRulerColumn;
	}

	/**
	 * Creates and returns a <code>LineChangeHover</code> to be used on this editor's change
	 * ruler column. This default implementation returns a plain <code>LineChangeHover</code>.
	 * Subclasses may override.
	 *
	 * @return the change hover to be used by this editors quick diff display
	 */
	protected LineChangeHover createChangeHover() {
		return new TextChangeHover();
	}

	/**
	 * Creates a new change ruler column for quick diff display independent of the
	 * line number ruler column
	 *
	 * @return a new change ruler column
	 * @deprecated as of 3.3. Not called any longer, replaced by {@link #createLineNumberRulerColumn()}
	 */
	protected IChangeRulerColumn createChangeRulerColumn() {
		/*
		 * Left for compatibility. See LineNumberColumn.
		 */
		return new ChangeRulerColumn(getSharedColors());
	}

	/**
	 * Returns {@link #createCompositeRuler()}. Subclasses should not override this method, but
	 * rather <code>createCompositeRuler</code> if they want to contribute their own vertical ruler
	 * implementation. If not an instance of {@link CompositeRuler} is returned, the built-in ruler
	 * columns (line numbers, annotations) will not work.
	 *
	 * <p>May become <code>final</code> in the future.</p>
	 *
	 * @see AbstractTextEditor#createVerticalRuler()
	 */
	protected IVerticalRuler createVerticalRuler() {
		return createCompositeRuler();
	}
	

	/**
	 * Creates a composite ruler to be used as the vertical ruler by this editor.
	 * Subclasses may re-implement this method.
	 *
	 * @return the vertical ruler
	 */
	protected CompositeRuler createCompositeRuler() {
		return new CompositeRuler();
	}

	
	/**
	 * Creates the annotation ruler column. Subclasses may re-implement or extend.
	 * 
	 * @param ruler the composite ruler that the column will be added
	 * @return an annotation ruler column
	 * @since 3.2
	 */
	protected IVerticalRulerColumn createAnnotationRulerColumn(CompositeRuler ruler) {
		return new AnnotationRulerColumn(VERTICAL_RULER_WIDTH, getAnnotationAccess());
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#createColumnSupport()
	 * @since 3.3
	 */
	protected final IColumnSupport createColumnSupport() {
		return new ColumnSupport(this, RulerColumnRegistry.getDefault()) {
			/*
			 * @see org.eclipse.ui.texteditor.rulers.ColumnSupport#initializeColumn(org.eclipse.ui.texteditor.rulers.AbstractContributedRulerColumn)
			 */
			protected void initializeColumn(IContributedRulerColumn column) {
				super.initializeColumn(column);
				RulerColumnDescriptor descriptor= column.getDescriptor();
				IVerticalRuler ruler= internalGetVerticalRuler();
				if (ruler instanceof CompositeRuler) {
					if (AnnotationColumn.ID.equals(descriptor.getId())) {
						((AnnotationColumn)column).setDelegate(createAnnotationRulerColumn((CompositeRuler) ruler));
					} else if (LineNumberColumn.ID.equals(descriptor.getId())) {
						fLineColumn= ((LineNumberColumn) column);
						fLineColumn.setForwarder(new LineNumberColumn.ICompatibilityForwarder() {
							public IVerticalRulerColumn createLineNumberRulerColumn() {
								return AbstractDecoratedTextEditor.this.createLineNumberRulerColumn();
							}
							public boolean isQuickDiffEnabled() {
								return AbstractDecoratedTextEditor.this.isPrefQuickDiffAlwaysOn();
							}
							public boolean isLineNumberRulerVisible() {
								return AbstractDecoratedTextEditor.this.isLineNumberRulerVisible();
							}
						});
					}
				}
			}

			/*
			 * @see org.eclipse.ui.texteditor.AbstractTextEditor.ColumnSupport#dispose()
			 */
			public void dispose() {
				fLineColumn= null;
				super.dispose();
			}
		};
	}

	/*
	 * @see AbstractTextEditor#handlePreferenceStoreChanged(PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {

		try {

			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer == null)
				return;

			String property= event.getProperty();

			if (fSourceViewerDecorationSupport != null && fOverviewRuler != null && OVERVIEW_RULER.equals(property))  {
				if (isOverviewRulerVisible())
					showOverviewRuler();
				else
					hideOverviewRuler();
				return;
			}

			if (DISABLE_OVERWRITE_MODE.equals(property)) {
				enableOverwriteMode(isOverwriteModeEnabled());
				return;
			}

			if (LINE_NUMBER_RULER.equals(property)) {
				// only handle visibility of the combined column, but not the number/change only state
				IColumnSupport columnSupport= (IColumnSupport)getAdapter(IColumnSupport.class);
				if (isLineNumberRulerVisible() && fLineColumn == null) {
					RulerColumnDescriptor lineNumberColumnDescriptor= RulerColumnRegistry.getDefault().getColumnDescriptor(LineNumberColumn.ID);
					if (lineNumberColumnDescriptor != null)
						columnSupport.setColumnVisible(lineNumberColumnDescriptor, true);
				} else if (!isLineNumberRulerVisible() && fLineColumn != null && !fLineColumn.isShowingChangeInformation()) {
					columnSupport.setColumnVisible(fLineColumn.getDescriptor(), false);
					fLineColumn= null;
				}
				return;
			}

			if (AbstractDecoratedTextEditorPreferenceConstants.QUICK_DIFF_ALWAYS_ON.equals(property)) {
				showChangeInformation(isPrefQuickDiffAlwaysOn());
			}

			if (AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH.equals(property)) {
				IPreferenceStore store= getPreferenceStore();
				if (store != null)
					sourceViewer.getTextWidget().setTabs(store.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH));
				return;
			}

			if (AbstractDecoratedTextEditorPreferenceConstants.EDITOR_UNDO_HISTORY_SIZE.equals(property) && sourceViewer instanceof ITextViewerExtension6) {
				IPreferenceStore store= getPreferenceStore();
				if (store != null)
					((ITextViewerExtension6)sourceViewer).getUndoManager().setMaximalUndoLevel(store.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_UNDO_HISTORY_SIZE));
				return;
			}

			if (AbstractDecoratedTextEditorPreferenceConstants.SHOW_RANGE_INDICATOR.equals(property)) {
				if (isRangeIndicatorEnabled()) {
					getSourceViewer().setRangeIndicator(getRangeIndicator());
				} else {
					getSourceViewer().removeRangeIndication();
					getSourceViewer().setRangeIndicator(null);
				}
			}

		} finally {
			super.handlePreferenceStoreChanged(event);
		}
	}

	/**
	 * Shows the overview ruler.
	 */
	protected void showOverviewRuler() {
		if (fOverviewRuler != null) {
			if (getSourceViewer() instanceof ISourceViewerExtension) {
				((ISourceViewerExtension) getSourceViewer()).showAnnotationsOverview(true);
				fSourceViewerDecorationSupport.updateOverviewDecorations();
			}
		}
	}

	/**
	 * Hides the overview ruler.
	 */
	protected void hideOverviewRuler() {
		if (getSourceViewer() instanceof ISourceViewerExtension) {
			fSourceViewerDecorationSupport.hideAnnotationOverview();
			((ISourceViewerExtension) getSourceViewer()).showAnnotationsOverview(false);
		}
	}

	/**
	 * Returns the annotation access.
	 *
	 * @return the annotation access
	 */
	protected IAnnotationAccess getAnnotationAccess() {
		if (fAnnotationAccess == null)
			fAnnotationAccess= createAnnotationAccess();
		return fAnnotationAccess;
	}

	/**
	 * Returns the annotation preference lookup.
	 *
	 * @return the annotation preference lookup
	 */
	protected AnnotationPreferenceLookup getAnnotationPreferenceLookup() {
		return EditorsPlugin.getDefault().getAnnotationPreferenceLookup();
	}

	/**
	 * Returns the overview ruler.
	 *
	 * @return the overview ruler
	 */
	protected IOverviewRuler getOverviewRuler() {
		if (fOverviewRuler == null)
			fOverviewRuler= createOverviewRuler(getSharedColors());
		return fOverviewRuler;
	}

	/**
	 * Returns the source viewer decoration support.
	 *
	 * @param viewer the viewer for which to return a decoration support
	 * @return the source viewer decoration support
	 */
	protected SourceViewerDecorationSupport getSourceViewerDecorationSupport(ISourceViewer viewer) {
		if (fSourceViewerDecorationSupport == null) {
			fSourceViewerDecorationSupport= new SourceViewerDecorationSupport(viewer, getOverviewRuler(), getAnnotationAccess(), getSharedColors());
			configureSourceViewerDecorationSupport(fSourceViewerDecorationSupport);
		}
		return fSourceViewerDecorationSupport;
	}

	/**
	 * Returns the annotation preferences.
	 *
	 * @return the annotation preferences
	 */
	protected MarkerAnnotationPreferences getAnnotationPreferences() {
		return fAnnotationPreferences;
	}


	/**
	 * If the editor can be saved all marker ranges have been changed according to
	 * the text manipulations. However, those changes are not yet propagated to the
	 * marker manager. Thus, when opening a marker, the marker's position in the editor
	 * must be determined as it might differ from the position stated in the marker.
	 *
	 * @param marker the marker to go to
	 * @deprecated visibility will be reduced, use <code>getAdapter(IGotoMarker.class) for accessing this method</code>
	 */
	public void gotoMarker(IMarker marker) {
		if (fIsUpdatingMarkerViews)
			return;
		
		if (getSourceViewer() == null)
			return;

		int start= MarkerUtilities.getCharStart(marker);
		int end= MarkerUtilities.getCharEnd(marker);
		
		boolean selectLine= start < 0 || end < 0; 

		// look up the current range of the marker when the document has been edited
		IAnnotationModel model= getDocumentProvider().getAnnotationModel(getEditorInput());
		if (model instanceof AbstractMarkerAnnotationModel) {

			AbstractMarkerAnnotationModel markerModel= (AbstractMarkerAnnotationModel) model;
			Position pos= markerModel.getMarkerPosition(marker);
			if (pos != null && !pos.isDeleted()) {
				// use position instead of marker values
				start= pos.getOffset();
				end= pos.getOffset() + pos.getLength();
			}

			if (pos != null && pos.isDeleted()) {
				// do nothing if position has been deleted
				return;
			}
		}

		IDocument document= getDocumentProvider().getDocument(getEditorInput());

		if (selectLine) {
			int line;
			try {
				if (start >= 0)
					line= document.getLineOfOffset(start);
				else {
					line= MarkerUtilities.getLineNumber(marker);
					// Marker line numbers are 1-based
					-- line;
					start= document.getLineOffset(line);
				}
				end= start + document.getLineLength(line) - 1;
			} catch (BadLocationException e) {
				return;
			}
		}

		int length= document.getLength();
		if (end - 1 < length && start < length)
			selectAndReveal(start, end - start);
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#isEditable()
	 * @since 3.3
	 */
	public boolean isEditable() {
		if (!super.isEditable())
			return false;
		return fIsEditingDerivedFileAllowed;
	}

	/*
	 * @see org.eclipse.ui.texteditor.StatusTextEditor#validateEditorInputState()
	 * @since 3.3
	 */
	public boolean validateEditorInputState() {
		if (!super.validateEditorInputState())
			return false;

		return validateEditorInputDerived();
	}

	/**
	 * Validates the editor input for derived state.
	 * If the given input is derived then this method
	 * can show a dialog asking whether to edit the
	 * derived file.
	 * 
	 * @return <code>true</code> if the input is OK for editing, <code>false</code> otherwise
	 * @since 3.3
	 */
	private boolean validateEditorInputDerived() {
		if (fIsDerivedStateValidated)
			return fIsEditingDerivedFileAllowed;

		if (getDocumentProvider() instanceof IDocumentProviderExtension) {
			IDocumentProviderExtension extension= (IDocumentProviderExtension)getDocumentProvider();
			IStatus status= extension.getStatus(getEditorInput());
			String pluginId= status.getPlugin();
			boolean isDerivedStatus= status.getCode() == IFileBufferStatusCodes.DERIVED_FILE && (FileBuffers.PLUGIN_ID.equals(pluginId) || EditorsUI.PLUGIN_ID.equals(pluginId));
			if (!isDerivedStatus)
				return true;
		}

		final String warnKey= AbstractDecoratedTextEditorPreferenceConstants.EDITOR_WARN_IF_INPUT_DERIVED;
		IPreferenceStore store= getPreferenceStore();
		if (!store.getBoolean(warnKey))
			return true;
		
		MessageDialogWithToggle toggleDialog= MessageDialogWithToggle.openYesNoQuestion(
				getSite().getShell(),
				TextEditorMessages.AbstractDecoratedTextEditor_warning_derived_title,
				TextEditorMessages.AbstractDecoratedTextEditor_warning_derived_message,
				TextEditorMessages.AbstractDecoratedTextEditor_warning_derived_dontShowAgain,
				false,
				null,
				null);
		
		EditorsUI.getPreferenceStore().setValue(warnKey, !toggleDialog.getToggleState());
		fIsDerivedStateValidated= true;
		return fIsEditingDerivedFileAllowed= toggleDialog.getReturnCode() == IDialogConstants.YES_ID;
	}

	/*
	 * For an explanation why we override this method see:
	 * bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=42230
	 * 
	 * @see org.eclipse.ui.texteditor.StatusTextEditor#isErrorStatus(org.eclipse.core.runtime.IStatus)
	 */
	protected boolean isErrorStatus(IStatus status) {
		if (!super.isErrorStatus(status))
			return false;
		
		if (!status.isMultiStatus())
			return !isReadOnlyLocalStatus(status);
		
		IStatus[] childrenStatus= status.getChildren();
		for (int i= 0; i < childrenStatus.length; i++) {
			if (childrenStatus[i].getSeverity() == IStatus.ERROR && !isReadOnlyLocalStatus(childrenStatus[i]))
				return true;
		}
		
		return false;
	}

	/**
	 * Check whether the given status is a <code>IResourceStatus.READ_ONLY_LOCAL</code>
	 * error.
	 * 
	 * @param status the status to be checked
	 * @return <code>true</code> if the given status is a <code>IResourceStatus.READ_ONLY_LOCAL</code> error 
	 * @since 3.3
	 */
	private boolean isReadOnlyLocalStatus(IStatus status) {
		return status.getCode() == IResourceStatus.READ_ONLY_LOCAL;
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#createActions()
	 */
	protected void createActions() {
		super.createActions();

		ResourceAction action= new AddMarkerAction(TextEditorMessages.getBundleForConstructedKeys(), "Editor.AddBookmark.", this, IMarker.BOOKMARK, true); //$NON-NLS-1$
		action.setHelpContextId(ITextEditorHelpContextIds.BOOKMARK_ACTION);
		action.setActionDefinitionId(IWorkbenchActionDefinitionIds.ADD_BOOKMARK);
		setAction(IDEActionFactory.BOOKMARK.getId(), action);

		action= new AddTaskAction(TextEditorMessages.getBundleForConstructedKeys(), "Editor.AddTask.", this); //$NON-NLS-1$
		action.setHelpContextId(ITextEditorHelpContextIds.ADD_TASK_ACTION);
		action.setActionDefinitionId(IWorkbenchActionDefinitionIds.ADD_TASK);
		setAction(IDEActionFactory.ADD_TASK.getId(), action);

		action= new ChangeEncodingAction(TextEditorMessages.getBundleForConstructedKeys(), "Editor.ChangeEncodingAction.", this); //$NON-NLS-1$
		action.setHelpContextId(ITextEditorHelpContextIds.CHANGE_ENCODING);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CHANGE_ENCODING);
		setAction(ITextEditorActionConstants.CHANGE_ENCODING, action);
		markAsPropertyDependentAction(ITextEditorActionConstants.CHANGE_ENCODING, true);

		action= new ResourceAction(TextEditorMessages.getBundleForConstructedKeys(), "Editor.ToggleLineNumbersAction.", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
			public void run() {
				toggleLineNumberRuler();
			}
		};
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.LINENUMBER_TOGGLE);
		setAction(ITextEditorActionConstants.LINENUMBERS_TOGGLE, action);

		action= new ResourceAction(TextEditorMessages.getBundleForConstructedKeys(), "Editor.ToggleQuickDiffAction.", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
			public void run() {
				toggleQuickDiffRuler();
			}
		};
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.QUICKDIFF_TOGGLE);
		setAction(ITextEditorActionConstants.QUICKDIFF_TOGGLE, action);

		action= new RevertLineAction(this, false);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.QUICKDIFF_REVERTLINE);
		setAction(ITextEditorActionConstants.QUICKDIFF_REVERTLINE, action);

		action= new RevertSelectionAction(this, false);
		setAction(ITextEditorActionConstants.QUICKDIFF_REVERTSELECTION, action);

		action= new RevertBlockAction(this, false);
		setAction(ITextEditorActionConstants.QUICKDIFF_REVERTBLOCK, action);

		action= new RestoreAction(this, false);
		setAction(ITextEditorActionConstants.QUICKDIFF_REVERTDELETION, action);

		IAction action2= new CompositeRevertAction(new IAction[] {
		                                       getAction(ITextEditorActionConstants.QUICKDIFF_REVERTSELECTION),
		                                       getAction(ITextEditorActionConstants.QUICKDIFF_REVERTBLOCK),
										       getAction(ITextEditorActionConstants.QUICKDIFF_REVERTDELETION),
										       getAction(ITextEditorActionConstants.QUICKDIFF_REVERTLINE)});
		action2.setActionDefinitionId(ITextEditorActionDefinitionIds.QUICKDIFF_REVERT);
		setAction(ITextEditorActionConstants.QUICKDIFF_REVERT, action2);
		
		action= new ResourceAction(TextEditorMessages.getBundleForConstructedKeys(), "Editor.HideRevisionInformationAction.") { //$NON-NLS-1$
			public void run() {
				if (fLineColumn != null)
					fLineColumn.hideRevisionInformation();
			}
		};
		setAction(ITextEditorActionConstants.REVISION_HIDE_INFO, action);

		action= new ResourceAction(TextEditorMessages.getBundleForConstructedKeys(), "Editor.CycleRevisionRenderingAction.") { //$NON-NLS-1$
			public void run() {
				final RenderingMode[] modes= { IRevisionRulerColumnExtension.AGE, IRevisionRulerColumnExtension.COMMITTER, IRevisionRulerColumnExtension.COMMITTER_SHADED_BY_AGE};
				IPreferenceStore store= EditorsUI.getPreferenceStore();
				String current= store.getString(AbstractDecoratedTextEditorPreferenceConstants.REVISION_RULER_RENDERING_MODE);
				for (int i= 0; i < modes.length; i++) {
					String mode= modes[i].name();
					if (mode.equals(current)) {
						int nextIndex= (i + 1) % modes.length;
						RenderingMode nextMode= modes[nextIndex];
						store.setValue(AbstractDecoratedTextEditorPreferenceConstants.REVISION_RULER_RENDERING_MODE, nextMode.name());
					}
				}
			}
		};
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.REVISION_RENDERING_CYCLE);
		setAction(ITextEditorActionConstants.REVISION_RENDERING_CYCLE, action);
		
		action= new BooleanPreferenceToggleAction(TextEditorMessages.getBundleForConstructedKeys(), "Editor.ToggleRevisionAuthorAction.", IAction.AS_CHECK_BOX, EditorsUI.getPreferenceStore(), AbstractDecoratedTextEditorPreferenceConstants.REVISION_RULER_SHOW_AUTHOR); //$NON-NLS-1$
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.REVISION_AUTHOR_TOGGLE);
		setAction(ITextEditorActionConstants.REVISION_SHOW_AUTHOR_TOGGLE, action);
		
		action= new BooleanPreferenceToggleAction(TextEditorMessages.getBundleForConstructedKeys(), "Editor.ToggleRevisionIdAction.", IAction.AS_CHECK_BOX, EditorsUI.getPreferenceStore(), AbstractDecoratedTextEditorPreferenceConstants.REVISION_RULER_SHOW_REVISION); //$NON-NLS-1$
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.REVISION_ID_TOGGLE);
		setAction(ITextEditorActionConstants.REVISION_SHOW_ID_TOGGLE, action);

		final Shell shell;
		if (getSourceViewer() != null)
			shell= getSourceViewer().getTextWidget().getShell();
		else
			shell= null;
		action= new ResourceAction(TextEditorMessages.getBundleForConstructedKeys(), "Editor.RulerPreferencesAction.") { //$NON-NLS-1$
			public void run() {
				String[] preferencePages= collectRulerMenuPreferencePages();
				if (preferencePages.length > 0 && (shell == null || !shell.isDisposed()))
					PreferencesUtil.createPreferenceDialogOn(shell, preferencePages[0], preferencePages, null).open();
			}

		};
		setAction(ITextEditorActionConstants.RULER_PREFERENCES, action);

		action= new ResourceAction(TextEditorMessages.getBundleForConstructedKeys(), "Editor.ContextPreferencesAction.") { //$NON-NLS-1$
			public void run() {
				String[] preferencePages= collectContextMenuPreferencePages();
				if (preferencePages.length > 0 && (shell == null || !shell.isDisposed()))
					PreferencesUtil.createPreferenceDialogOn(shell, preferencePages[0], preferencePages, null).open();
			}
		};
		action.setId(ITextEditorActionConstants.CONTEXT_PREFERENCES);
		setAction(ITextEditorActionConstants.CONTEXT_PREFERENCES, action);
	}

	public Object getAdapter(Class adapter) {
		if (IGotoMarker.class.equals(adapter))
			return fGotoMarkerAdapter;

		if (IAnnotationAccess.class.equals(adapter))
			return getAnnotationAccess();

		if (adapter == IShowInSource.class) {
			return new IShowInSource() {
				public ShowInContext getShowInContext() {
					ISelection selection= null;
					ISelectionProvider selectionProvider= getSelectionProvider();
					if (selectionProvider != null)
						selection= selectionProvider.getSelection();
					return new ShowInContext(getEditorInput(), selection);
				}
			};
		}

		if (IRevisionRulerColumn.class.equals(adapter)) {
			if (fLineNumberRulerColumn instanceof IRevisionRulerColumn)
				return fLineNumberRulerColumn;
		}

		return super.getAdapter(adapter);
	
	}

	/*
	 * If there is no explicit document provider set, the implicit one is
	 * re-initialized based on the given editor input.
	 *
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#setDocumentProvider(org.eclipse.ui.IEditorInput)
	 */
	protected void setDocumentProvider(IEditorInput input) {
		fImplicitDocumentProvider= DocumentProviderRegistry.getDefault().getDocumentProvider(input);
		IDocumentProvider provider= super.getDocumentProvider();
		if (provider instanceof ForwardingDocumentProvider) {
			ForwardingDocumentProvider forwarder= (ForwardingDocumentProvider) provider;
			forwarder.setParentProvider(fImplicitDocumentProvider);
		}
	}

	/*
	 * @see org.eclipse.ui.texteditor.ITextEditor#getDocumentProvider()
	 */
	public IDocumentProvider getDocumentProvider() {
		IDocumentProvider provider= super.getDocumentProvider();
		if (provider == null)
			return fImplicitDocumentProvider;
		return provider;
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#disposeDocumentProvider()
	 */
	protected void disposeDocumentProvider() {
		super.disposeDocumentProvider();
		fImplicitDocumentProvider= null;
	}

	/*
	 * @see AbstractTextEditor#doSetInput(IEditorInput)
	 *
	 * This implementation also updates change information in the quick diff
	 * ruler.
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {
		fIsDerivedStateValidated= false;
		fIsEditingDerivedFileAllowed= true;
		
		if (fLineColumn != null)
			fLineColumn.hideRevisionInformation();

		super.doSetInput(input);
		
		RulerColumnDescriptor lineNumberColumnDescriptor= RulerColumnRegistry.getDefault().getColumnDescriptor(LineNumberColumn.ID);
		if (lineNumberColumnDescriptor != null) {
			IColumnSupport columnSupport= (IColumnSupport)getAdapter(IColumnSupport.class);
			columnSupport.setColumnVisible(lineNumberColumnDescriptor, isLineNumberRulerVisible() || isPrefQuickDiffAlwaysOn());
		}
	}
	
	/**
	 * This implementation asks the user for the workspace path of a file resource and saves the document there.
	 *
	 * @param progressMonitor the progress monitor to be used
	 * @since 3.2
	 */
	protected void performSaveAs(IProgressMonitor progressMonitor) {
		Shell shell= getSite().getShell();
		final IEditorInput input= getEditorInput();

		IDocumentProvider provider= getDocumentProvider();
		final IEditorInput newInput;
		
		if (input instanceof JavaFileEditorInput) {
			FileDialog dialog= new FileDialog(shell, SWT.SAVE);
			dialog.setFileName(((JavaFileEditorInput)input).getName());
			dialog.setFilterPath(((JavaFileEditorInput)input).getPath().toOSString());
			
			String path= dialog.open();
			if (path == null) {
				if (progressMonitor != null)
					progressMonitor.setCanceled(true);
				return;
			}

			// Check whether file exists and if so, confirm overwrite
			final File externalFile= new File(path);
			if (externalFile.exists()) {
		        MessageDialog overwriteDialog= new MessageDialog(
		        		shell,
		        		TextEditorMessages.AbstractDecoratedTextEditor_saveAs_overwrite_title,
		        		null,
		        		NLSUtility.format(TextEditorMessages.AbstractDecoratedTextEditor_saveAs_overwrite_message, path),
		        		MessageDialog.WARNING,
		        		new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
		        		1); // 'No' is the default
				if (overwriteDialog.open() != Window.OK) {
					if (progressMonitor != null) {
						progressMonitor.setCanceled(true);
						return;
					}
				}
			}

			IFileStore fileStore= EFS.getLocalFileSystem().getStore(externalFile.toURI());
			IFile file= getWorkspaceFile(fileStore);
			if (file != null)
				newInput= new FileEditorInput(file);
			else
				newInput= new JavaFileEditorInput(fileStore);
			
		} else {
			SaveAsDialog dialog= new SaveAsDialog(shell);

			IFile original= (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
			if (original != null)
				dialog.setOriginalFile(original);

			dialog.create();

			if (provider.isDeleted(input) && original != null) {
				String message= NLSUtility.format(TextEditorMessages.AbstractDecoratedTextEditor_warning_saveAs_deleted, original.getName());
				dialog.setErrorMessage(null);
				dialog.setMessage(message, IMessageProvider.WARNING);
			}

			if (dialog.open() == Window.CANCEL) {
				if (progressMonitor != null)
					progressMonitor.setCanceled(true);
				return;
			}
			
			IPath filePath= dialog.getResult();
			if (filePath == null) {
				if (progressMonitor != null)
					progressMonitor.setCanceled(true);
				return;
			}
			
			IWorkspace workspace= ResourcesPlugin.getWorkspace();
			IFile file= workspace.getRoot().getFile(filePath);
			newInput= new FileEditorInput(file);

		}
		
		if (provider == null) {
			// editor has programmatically been  closed while the dialog was open
			return;
		}

		boolean success= false;
		try {

			provider.aboutToChange(newInput);
			provider.saveDocument(progressMonitor, newInput, provider.getDocument(input), true);
			success= true;

		} catch (CoreException x) {
			final IStatus status= x.getStatus();
			if (status == null || status.getSeverity() != IStatus.CANCEL) {
				String title= TextEditorMessages.AbstractDecoratedTextEditor_error_saveAs_title;
				String msg= NLSUtility.format(TextEditorMessages.AbstractDecoratedTextEditor_error_saveAs_message, x.getMessage());
				MessageDialog.openError(shell, title, msg);
			}
		} finally {
			provider.changed(newInput);
			if (success)
				setInput(newInput);
		}

		if (progressMonitor != null)
			progressMonitor.setCanceled(!success);
	}

	/**
	 * Checks whether there given file store points
	 * to a file in the workspace. Only returns a
	 * workspace file if there's a single match.
	 * 
	 * @param fileStore the file store
	 * @return the <code>IFile</code> that matches the given file store
	 * @since 3.2
	 */
	private IFile getWorkspaceFile(IFileStore fileStore) {
		IWorkspaceRoot workspaceRoot= ResourcesPlugin.getWorkspace().getRoot();
		IFile[] files= workspaceRoot.findFilesForLocation(new Path(fileStore.toURI().getPath()));
		if (files != null && files.length == 1)
			return files[0];
		return null;
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#rulerContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 * @since 3.1
	 */
	protected void rulerContextMenuAboutToShow(IMenuManager menu) {
		/*
		 * XXX: workaround for reliable menu item ordering.
		 * This can be changed once the action contribution story converges,
		 * see http://dev.eclipse.org/viewcvs/index.cgi/~checkout~/platform-ui-home/R3_1/dynamic_teams/dynamic_teams.html#actionContributions
		 */
		// pre-install menus for contributions and call super
		menu.add(new Separator("debug")); //$NON-NLS-1$
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(new GroupMarker(ITextEditorActionConstants.GROUP_RESTORE));
		menu.add(new Separator("add")); //$NON-NLS-1$
		menu.add(new Separator(ITextEditorActionConstants.GROUP_RULERS));
		menu.add(new Separator(ITextEditorActionConstants.GROUP_REST));

		super.rulerContextMenuAboutToShow(menu);
		
		addRulerContributionActions(menu);

		/* quick diff */
		if (isEditorInputModifiable()) {
			IAction quickdiffAction= getAction(ITextEditorActionConstants.QUICKDIFF_TOGGLE);
			quickdiffAction.setChecked(isChangeInformationShowing());
			menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, quickdiffAction);

			if (isChangeInformationShowing()) {
				TextEditorAction revertLine= new RevertLineAction(this, true);
				TextEditorAction revertSelection= new RevertSelectionAction(this, true);
				TextEditorAction revertBlock= new RevertBlockAction(this, true);
				TextEditorAction revertDeletion= new RestoreAction(this, true);

				revertSelection.update();
				revertBlock.update();
				revertLine.update();
				revertDeletion.update();

				// only add block action if selection action is not enabled
				if (revertSelection.isEnabled())
					menu.appendToGroup(ITextEditorActionConstants.GROUP_RESTORE, revertSelection);
				else if (revertBlock.isEnabled())
					menu.appendToGroup(ITextEditorActionConstants.GROUP_RESTORE, revertBlock);
				if (revertLine.isEnabled())
					menu.appendToGroup(ITextEditorActionConstants.GROUP_RESTORE, revertLine);
				if (revertDeletion.isEnabled())
					menu.appendToGroup(ITextEditorActionConstants.GROUP_RESTORE, revertDeletion);
			}
		}
		
		// revision info
		if (fLineColumn != null && fLineColumn.isShowingRevisionInformation()) {
			IMenuManager revisionMenu= new MenuManager(TextEditorMessages.AbstractDecoratedTextEditor_revisions_menu);
			menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, revisionMenu);
			
			IAction hideRevisionInfoAction= getAction(ITextEditorActionConstants.REVISION_HIDE_INFO);
			revisionMenu.add(hideRevisionInfoAction);
			revisionMenu.add(new Separator());
			
			String[] labels= { TextEditorMessages.AbstractDecoratedTextEditor_revision_colors_option_by_date, TextEditorMessages.AbstractDecoratedTextEditor_revision_colors_option_by_committer, TextEditorMessages.AbstractDecoratedTextEditor_revision_colors_option_by_committer_and_date };
			final RenderingMode[] modes= { IRevisionRulerColumnExtension.AGE, IRevisionRulerColumnExtension.COMMITTER, IRevisionRulerColumnExtension.COMMITTER_SHADED_BY_AGE};
			final IPreferenceStore uiStore= EditorsUI.getPreferenceStore();
			String current= uiStore.getString(AbstractDecoratedTextEditorPreferenceConstants.REVISION_RULER_RENDERING_MODE);
			for (int i= 0; i < modes.length; i++) {
				final String mode= modes[i].name();
				IAction action= new Action(labels[i], IAction.AS_RADIO_BUTTON) {
					public void run() {
						// set preference globally, LineNumberColumn reacts on preference change
						uiStore.setValue(AbstractDecoratedTextEditorPreferenceConstants.REVISION_RULER_RENDERING_MODE, mode);
					}
				};
				action.setChecked(mode.equals(current));
				revisionMenu.add(action);
			}
			
			revisionMenu.add(new Separator());

			revisionMenu.add(getAction(ITextEditorActionConstants.REVISION_SHOW_AUTHOR_TOGGLE));
			revisionMenu.add(getAction(ITextEditorActionConstants.REVISION_SHOW_ID_TOGGLE));
		}

		IAction lineNumberAction= getAction(ITextEditorActionConstants.LINENUMBERS_TOGGLE);
		lineNumberAction.setChecked(fLineColumn != null && fLineColumn.isShowingLineNumbers());
		menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, lineNumberAction);

		IAction preferencesAction= getAction(ITextEditorActionConstants.RULER_PREFERENCES);
		menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, new Separator(ITextEditorActionConstants.GROUP_SETTINGS));
		menu.appendToGroup(ITextEditorActionConstants.GROUP_SETTINGS, preferencesAction);
	}
	
	/**
	 * Adds "show" actions for all contributed rulers that support it.
	 * 
	 * @param menu the ruler context menu
	 * @since 3.3
	 */
	private void addRulerContributionActions(IMenuManager menu) {
		// store directly in generic editor preferences
		final IColumnSupport support= (IColumnSupport) getAdapter(IColumnSupport.class);
		IPreferenceStore store= EditorsUI.getPreferenceStore();
		final RulerColumnPreferenceAdapter adapter= new RulerColumnPreferenceAdapter(store, AbstractTextEditor.PREFERENCE_RULER_CONTRIBUTIONS);
		List descriptors= RulerColumnRegistry.getDefault().getColumnDescriptors();
		for (Iterator t= descriptors.iterator(); t.hasNext();) {
			final RulerColumnDescriptor descriptor= (RulerColumnDescriptor) t.next();
			if (!descriptor.isIncludedInMenu() || !support.isColumnSupported(descriptor))
				continue;
			final boolean isVisible= support.isColumnVisible(descriptor);
			IAction action= new Action(MessageFormat.format(TextEditorMessages.AbstractDecoratedTextEditor_show_ruler_label, new Object[] {descriptor.getName()}), IAction.AS_CHECK_BOX) {
				public void run() {
					if (descriptor.isGlobal())
						// column state is modified via preference listener of AbstractTextEditor
						adapter.setEnabled(descriptor, !isVisible);
					else
						// directly modify column for this editor instance
						support.setColumnVisible(descriptor, !isVisible);
				}
			};
			action.setChecked(isVisible);
			action.setImageDescriptor(descriptor.getIcon());
			menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, action);
		}
	}
	
	/**
	 * Toggles the line number global preference and shows the line number ruler
	 * accordingly.
	 *
	 * @since 3.1
	 */
	private void toggleLineNumberRuler() {
		// globally
		IPreferenceStore store= EditorsUI.getPreferenceStore();
		store.setValue(LINE_NUMBER_RULER, !isLineNumberRulerVisible());
	}

	/**
	 * Toggles the quick diff global preference and shows the quick diff ruler
	 * accordingly.
	 *
	 * @since 3.1
	 */
	private void toggleQuickDiffRuler() {
		// change the visibility locally if this editor is not in sync with the global preference
		// toggle the preference if we are in sync.
		IPreferenceStore store= EditorsUI.getPreferenceStore();
		boolean current= store.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.QUICK_DIFF_ALWAYS_ON);
		if (current == isChangeInformationShowing())
			store.setValue(AbstractDecoratedTextEditorPreferenceConstants.QUICK_DIFF_ALWAYS_ON, !current);
		else
			showChangeInformation(current);
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#editorContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 * @since 3.1
	 */
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);

		IAction preferencesAction= getAction(ITextEditorActionConstants.CONTEXT_PREFERENCES);
		menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator(ITextEditorActionConstants.GROUP_SETTINGS));
		menu.appendToGroup(ITextEditorActionConstants.GROUP_SETTINGS, preferencesAction);
		
		menu.appendToGroup(ITextEditorActionConstants.GROUP_SAVE, new Separator(ITextEditorActionConstants.GROUP_OPEN));
		MenuManager showInSubMenu= new MenuManager(getShowInMenuLabel());
		showInSubMenu.add(ContributionItemFactory.VIEWS_SHOW_IN.create(getEditorSite().getWorkbenchWindow()));
		menu.appendToGroup(ITextEditorActionConstants.GROUP_OPEN, showInSubMenu); 
	}

	/**
	 * Returns the menu label for 'Show In' together with its key binding string.
	 * 
	 * @return the 'Show In' menu label
	 * @since 3.2
	 */
	private String getShowInMenuLabel() {
		String keyBinding= null;
		
		IBindingService bindingService= (IBindingService)PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		if (bindingService != null)
			keyBinding= bindingService.getBestActiveBindingFormattedFor("org.eclipse.ui.navigate.showInQuickMenu"); //$NON-NLS-1$
		
		if (keyBinding == null)
			keyBinding= ""; //$NON-NLS-1$
		
		return NLS.bind(TextEditorMessages.AbstractDecoratedTextEditor_showIn_menu, keyBinding);
	}

	/**
	 * Returns the preference page ids of the preference pages to be shown
	 * when executing the preferences action from the editor context menu.
	 * <p>
	 * Subclasses may extend or replace.
	 * </p>
	 *
	 * @return the preference page ids to show, may be empty
	 * @since 3.1
	 */
	protected String[] collectContextMenuPreferencePages() {
		return new String[] {
			"org.eclipse.ui.preferencePages.GeneralTextEditor", //$NON-NLS-1$
			"org.eclipse.ui.editors.preferencePages.Annotations", //$NON-NLS-1$
			"org.eclipse.ui.editors.preferencePages.QuickDiff", //$NON-NLS-1$
			"org.eclipse.ui.editors.preferencePages.Accessibility", //$NON-NLS-1$
			"org.eclipse.ui.editors.preferencePages.Spelling", //$NON-NLS-1$
			"org.eclipse.ui.editors.preferencePages.LinkedModePreferencePage", //$NON-NLS-1$
		};
	}

	/**
	 * Returns the preference page ids of the preference pages to be shown when
	 * executing the preferences action from the editor ruler context menu.
	 * <p>
	 * The default is to return the same list as
	 * <code>collectContextMenuPreferencePages</code>.
	 * </p>
	 * <p>
	 * Subclasses may extend or replace.
	 * </p>
	 *
	 * @return the preference page ids to show, may be empty
	 * @since 3.1
	 */
	protected String[] collectRulerMenuPreferencePages() {
		return collectContextMenuPreferencePages();
	}
	
	/*
	 * @see AbstractTextEditor#getUndoRedoOperationApprover(IUndoContext)
	 * @since 3.1
	 */
	protected IOperationApprover getUndoRedoOperationApprover(IUndoContext undoContext) {
		IEditorInput input= getEditorInput();
		if (input != null && input.getAdapter(IResource.class) != null)
			return new NonLocalUndoUserApprover(undoContext, this, new Object [] { input }, IResource.class);
		return super.getUndoRedoOperationApprover(undoContext);
	}
	
	/**
	 * Returns whether the given annotation is configured as a target for the
	 * "Go to Next/Previous Annotation" actions.
	 * <p>
	 * The annotation is a target if their annotation type is configured to be
	 * in the Next/Previous tool bar drop down menu and if it is checked.
	 * </p>
	 *
	 * @param annotation the annotation
	 * @return <code>true</code> if this is a target, <code>false</code> otherwise
	 * @since 3.2
	 */
	protected boolean isNavigationTarget(Annotation annotation) {
		AnnotationPreference preference= getAnnotationPreferenceLookup().getAnnotationPreference(annotation);
//		See bug 41689
//		String key= forward ? preference.getIsGoToNextNavigationTargetKey() : preference.getIsGoToPreviousNavigationTargetKey();
		String key= preference == null ? null : preference.getIsGoToNextNavigationTargetKey();
		return (key != null && getPreferenceStore().getBoolean(key));
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * This extended implementation updates views that also show the
	 * select marker annotation.
	 * </p>
	 * @since 3.2
	 */
	public Annotation gotoAnnotation(boolean forward) {
		Annotation annotation= super.gotoAnnotation(forward);
		if (annotation != null)
			updateMarkerViews(annotation);
		return annotation;
	}
	
	/**
	 * Updates visible views that show markers.
	 * <p>
	 * If the given annotation can be associated with a marker then
	 * this method tries select the this marker in views that show
	 * markers.
	 * </p> 
	 * @param annotation
	 * @since 3.2
	 */
	protected void updateMarkerViews(Annotation annotation) {
		IMarker marker= null;
		if (annotation instanceof MarkerAnnotation)
			marker= ((MarkerAnnotation)annotation).getMarker();
		
		if (marker != null) {
			try {
				fIsUpdatingMarkerViews= true;
				IWorkbenchPage page= getSite().getPage();
				MarkerViewUtil.showMarker(page, marker, false);
			} finally {
				fIsUpdatingMarkerViews= false;
			}
		}
	}

}
