/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.prov.metadata;

import org.eclipse.osgi.service.resolver.VersionRange;

public class ResolvedInstallableUnitFragment extends ResolvedInstallableUnit implements IResolvedInstallableUnitFragment {

	public ResolvedInstallableUnitFragment(InstallableUnitFragment resolved) {
		super(resolved);
	}

	public String getHostId() {
		return ((InstallableUnitFragment) resolved).getHostId();
	}

	public VersionRange getHostVersionRange() {
		return ((InstallableUnitFragment) resolved).getHostVersionRange();
	}
}
