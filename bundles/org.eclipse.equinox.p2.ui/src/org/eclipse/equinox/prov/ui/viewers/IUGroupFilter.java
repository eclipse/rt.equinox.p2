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

package org.eclipse.equinox.prov.ui.viewers;

import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.metadata.RequiredCapability;

/**
 * Viewer filter which filters IU's so that only those with the kind "group" are
 * included.
 * 
 * @since 3.4
 */
public class IUGroupFilter extends IUCapabilityFilter {

	public IUGroupFilter() {
		super(new RequiredCapability[] {new RequiredCapability(IInstallableUnit.IU_KIND_NAMESPACE, "group", null, null, false, false)}); //$NON-NLS-1$

	}
}
