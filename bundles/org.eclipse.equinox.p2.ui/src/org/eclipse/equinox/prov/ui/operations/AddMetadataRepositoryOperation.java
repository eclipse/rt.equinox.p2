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
package org.eclipse.equinox.prov.ui.operations;

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.prov.core.ProvisionException;
import org.eclipse.equinox.prov.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.prov.ui.ProvisioningUtil;

/**
 * Operation that adds a metadata repository given its URL.
 * 
 * @since 3.4
 */
public class AddMetadataRepositoryOperation extends RepositoryOperation {

	boolean added = false;

	public AddMetadataRepositoryOperation(String label, URL url, String name) {
		super(label, new URL[] {url}, new String[] {name});
	}

	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < urls.length; i++) {
			IMetadataRepository repo = ProvisioningUtil.addMetadataRepository(urls[i], monitor, uiInfo);
			if (repo == null) {
				return failureStatus();
			}
			if (names[i] != null) {
				ProvisioningUtil.setRepositoryName(repo, names[i]);
			}
		}
		added = true;
		return okStatus();
	}

	protected IStatus doUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < urls.length; i++) {
			ProvisioningUtil.removeMetadataRepository(urls[i], monitor, uiInfo);
		}
		added = false;
		return okStatus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canExecute()
	 */
	public boolean canExecute() {
		return super.canExecute() && !added;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canUndo()
	 */
	public boolean canUndo() {
		return super.canUndo() && added;
	}
}
