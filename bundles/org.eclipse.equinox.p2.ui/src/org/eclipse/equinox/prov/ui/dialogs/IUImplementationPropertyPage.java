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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.prov.metadata.InstallableUnit;
import org.eclipse.equinox.prov.ui.internal.ProvUIMessages;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * PropertyPage that shows an IU's properties
 * 
 * @since 3.4
 */
public class IUImplementationPropertyPage extends PropertyPage {

	private IUImplementationGroup iuGroup;

	protected Control createContents(Composite parent) {
		InstallableUnit iu = (InstallableUnit) getElement().getAdapter(InstallableUnit.class);
		if (iu == null) {
			Label label = new Label(parent, SWT.DEFAULT);
			label.setText(ProvUIMessages.IUPropertyPage_NoIUSelected);
		}
		iuGroup = new IUImplementationGroup(parent, iu, new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				verifyComplete();
			}
		});
		Dialog.applyDialogFont(iuGroup.getComposite());
		verifyComplete();
		return iuGroup.getComposite();
	}

	public boolean performOk() {
		return true;
	}

	void verifyComplete() {
		if (iuGroup == null) {
			return;
		}
		IStatus status = iuGroup.verify();
		setValid(status.isOK());
	}
}
