/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ltk.core.refactoring.participants;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.internal.core.refactoring.Assert;

/**
 * A generic move refactoring. The actual refactoring is done
 * by the move processor passed to the constructor.
 * 
 * @since 3.0
 */
public class MoveRefactoring extends ProcessorBasedRefactoring {

	private MoveProcessor fProcessor;
	private MoveParticipant[] fElementParticipants;

	/**
	 * Creates a new move refactoring with the given move processor.
	 * 
	 * @param processor the move processor
	 */
	public MoveRefactoring(MoveProcessor processor) {
		Assert.isNotNull(processor);
		fProcessor= processor; 
	}
	
	/**
	 * Returns the move processor associated with this move refactoring.
	 * 
	 * @return
	 */
	public MoveProcessor getMoveProcessor() {
		return fProcessor;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringProcessor getProcessor() {
		return fProcessor;
	}

	/**
	 * {@inheritDoc}
	 */
	protected RefactoringParticipant[] getElementParticipants(boolean setArguments) throws CoreException {
		if (fElementParticipants == null)
			fElementParticipants= fProcessor.loadElementParticipants();
		if (setArguments) {
			for (int i= 0; i < fElementParticipants.length; i++) {
				fProcessor.setArgumentsTo(fElementParticipants[i]);
			}
		}
		RefactoringParticipant[]result= new RefactoringParticipant[fElementParticipants.length];
		for (int i= 0; i < result.length; i++) {
			result[i]= fElementParticipants[i];
		}
		return result;
	}
}
