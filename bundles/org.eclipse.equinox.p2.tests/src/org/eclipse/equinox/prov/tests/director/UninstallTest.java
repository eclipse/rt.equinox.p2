/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.prov.tests.director;

import org.eclipse.equinox.prov.director.IDirector;
import org.eclipse.equinox.prov.engine.Profile;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.metadata.InstallableUnit;
import org.eclipse.equinox.prov.tests.AbstractProvisioningTest;
import org.osgi.framework.Version;

public class UninstallTest extends AbstractProvisioningTest {
	private InstallableUnit a1;
	private Profile profile;
	private IDirector director;

	protected void setUp() throws Exception {
		a1 = new InstallableUnit();
		a1.setId("A");
		a1.setVersion(new Version(1, 0, 0));
		a1.setSingleton(true);

		profile = new Profile("TestProfile." + getName());
		director = createDirector();
	}

	public void testUninstall() {
		System.out.println(director.install(new IInstallableUnit[] {a1}, profile, null, null));
		director.uninstall(new IInstallableUnit[] {a1}, profile, null);
		assertEmptyProfile(profile);
	}
}
