/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ltk.core.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.internal.core.refactoring.Assert;
import org.eclipse.ltk.internal.core.refactoring.RefactoringCoreMessages;

/**
 * A refactoring change that does nothing. The reverse change of a
 * <code>NullChange</code> is a <code>NullChange</code>.
 * 
 * @since 3.0
 */
public class NullChange extends Change {

	private String fName;
	
	/**
	 * Creates a new <code>NullChange</code> with a default name.
	 */
	public NullChange() {
		this(RefactoringCoreMessages.getString("NullChange.name")); //$NON-NLS-1$
	}

	/**
	 * Creates a new <code>NullChange</code> with the given name.
	 * 
	 * @param name the human readable name of this change
	 */
	public NullChange(String name) {
		Assert.isNotNull(name);
		fName= name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return fName;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void initializeValidationData(IProgressMonitor pm) throws CoreException {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return new RefactoringStatus();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Change perform(IProgressMonitor pm) throws CoreException {
		return new NullChange();
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getModifiedElement() {
		return null;
	}
}
