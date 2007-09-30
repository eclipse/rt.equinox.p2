/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.prov.director;

import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.metadata.RequiredCapability;

public class RequirementBasedFilter extends IUFilter {
	private RequiredCapability[] reqs;

	public RequirementBasedFilter(RequiredCapability[] toFilterOn) {
		reqs = toFilterOn;
	}

	public boolean accept(IInstallableUnit iu) {
		for (int i = 0; i < reqs.length; i++) {
			if (reqs[i].getNamespace().equals(IInstallableUnit.IU_NAMESPACE) && reqs[i].getName().equals(iu.getId()) && reqs[i].getRange().isIncluded(iu.getVersion()))
				return true;
		}
		return false;
	}
}
