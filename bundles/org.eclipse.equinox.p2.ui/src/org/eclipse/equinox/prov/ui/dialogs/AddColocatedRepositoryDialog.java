/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.prov.ui.dialogs;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.equinox.prov.core.repository.IRepositoryInfo;
import org.eclipse.equinox.prov.ui.ProvUI;
import org.eclipse.equinox.prov.ui.internal.ProvUIMessages;
import org.eclipse.equinox.prov.ui.operations.AddColocatedRepositoryOperation;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog that allows colocated metadata and artifact repositories
 * to be defined and added.
 * 
 * @since 3.4
 * 
 */
public class AddColocatedRepositoryDialog extends AddRepositoryDialog {

	public AddColocatedRepositoryDialog(Shell parentShell, IRepositoryInfo[] knownRepositories) {
		super(parentShell, knownRepositories);

	}

	protected IUndoableOperation getOperation(URL url, String name) {
		return new AddColocatedRepositoryOperation(getShell().getText(), url, name);
	}

	protected URL makeRepositoryURL(String urlString) {
		URL newURL;
		try {
			newURL = new URL(urlString);
		} catch (MalformedURLException e) {
			// TODO need friendlier user message rather than just reporting exception
			ProvUI.handleException(e, ProvUIMessages.AddColocatedRepositoryDialog_InvalidURL);
			return null;
		}
		String urlSpec = newURL.toExternalForm();
		try {
			if (!urlSpec.endsWith("/")) //$NON-NLS-1$
				urlSpec += "/"; //$NON-NLS-1$
			newURL = new URL(urlSpec);
		} catch (MalformedURLException e) {
			return null;
		}
		return newURL;
	}

	protected String repositoryFileName() {
		return null;
	}

	protected boolean repositoryIsFile() {
		return false;
	}
}
