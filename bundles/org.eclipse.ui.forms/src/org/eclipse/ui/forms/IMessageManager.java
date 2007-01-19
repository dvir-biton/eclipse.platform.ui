/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.ui.forms;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.Form;

/**
 * This interface provides for managing messages in a form. It is responsible
 * for:
 * <ul>
 * <li>Bridging the concept of typed messages and field decorations</li>
 * <li>Adding multiple messages per field in a form</li>
 * <li>Rolling up local messages to the form header</li>
 * <li>Adding multiple general messages to the form header</li>
 * </ul>
 * <p>
 * To use it in a form, do the following:
 * <ol>
 * <li>For each interactive control, add a listener to it to monitor user input</li>
 * <li>Every time the input changes, validate it. If there is a problem, add a
 * message with a unique ID to the manager. If there is already a message with
 * the same ID in the manager, its type and message text will be updated (no
 * duplicates). Note that you can messages with different ids to the same
 * control to track multiple problem with the user input.</li>
 * <li>If the problem has been cleared, remove the message using the id.</li>
 * <li>If something happens in the form that is not related to any control, us
 * the other <code>addMessage</code> method.</li>
 * </ol>
 * <p>
 * This interface should only be referenced. It must not be implemented or
 * extended.
 * </p>
 * 
 * @since 3.3
 * @see IMessageProvider
 * @see IManagedForm
 */

public interface IMessageManager {
	/**
	 * Adds a general message that is not associated with any decorated field.
	 * 
	 * @param key
	 *            a unique message key that will be used to look the message up
	 *            later
	 * 
	 * @param messageText
	 *            the message to add
	 * @param data
	 *            an object for application use (can be <code>null</code>)
	 * @param type
	 *            the message type as defined in <code>IMessageProvider</code>.
	 */
	void addMessage(Object key, String messageText, Object data, int type);

	/**
	 * Adds a message that should be associated with the provided control.
	 * 
	 * @param key
	 *            the unique message key
	 * @param messageText
	 *            the message to add
	 * @param data
	 *            an object for application use (can be <code>null</code>)
	 * @param type
	 *            the message type
	 * @param control
	 *            the control to associate the message with
	 */
	void addMessage(Object key, String messageText, Object data, int type,
			Control control);

	/**
	 * Removes the provided general message.
	 * 
	 * @param key
	 *            the key of the message to remove
	 */
	void removeMessage(Object key);

	/**
	 * Removes all the general messages. If there are local messages associated
	 * with controls, the replacement message may show up drawing user's
	 * attention to these local messages. Otherwise, the container will clear
	 * the message area.
	 */
	void removeMessages();

	/**
	 * Removes the message associated with the provided control.
	 * 
	 * @param key
	 *            the id of the message to remove
	 * @param control
	 *            the control the message is associated with
	 */
	void removeMessage(Object key, Control control);

	/**
	 * Removes all the messages associated with the provided control.
	 * 
	 * @param control
	 *            the control the messages are associated with
	 */
	void removeMessages(Control control);

	/**
	 * Removes all the local field messages and all the general container
	 * messages.
	 */
	void removeAllMessages();

	/**
	 * Updates the message container with the messages currently in the manager.
	 * Use this method when some of the controls previously managed have been
	 * disposed. Automatic update on control dispose is not done to avoid an
	 * attempt to update a container that is in the process of being disposed
	 * itself.
	 */
	void update();

	/**
	 * When message manager is used in context of a form, and there
	 * are hyperlink listeners for messages in the header, the hyperlink
	 * event will carry an object of type <code>IMessage[]</code> as an
	 * href. You can use this method to create a summary text from this
	 * array consistent with the tool tip used by the form header.
	 * 
	 * @param messages
	 *            an array of messages
	 * @return a textual representation of the messages with one message per
	 *         line.
	 * @see Form#addMessageHyperlinkListener(org.eclipse.ui.forms.events.IHyperlinkListener)
	 */
	String createSummary(IMessage[] messages);
}