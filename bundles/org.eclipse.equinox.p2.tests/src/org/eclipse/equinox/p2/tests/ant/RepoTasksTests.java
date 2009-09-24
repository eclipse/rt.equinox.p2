/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ant;

import java.io.File;
import java.net.URI;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractAntProvisioningTest;

public class RepoTasksTests extends AbstractAntProvisioningTest {
	private static final String MIRROR_TASK = "p2.mirror";
	private static final String REMOVE_IU_TASK = "p2.remove.iu";

	private URI destinationRepo;
	private URI sourceRepo;

	public void setUp() throws Exception {
		super.setUp();
		// Get a random location to create a repository
		destinationRepo = getTestFolder(getName()).toURI();
		sourceRepo = getTestData("error loading data", "testData/mirror/mirrorSourceRepo2").toURI();
	}

	public void tearDown() throws Exception {
		// Remove repository manager references
		getArtifactRepositoryManager().removeRepository(destinationRepo);
		getMetadataRepositoryManager().removeRepository(destinationRepo);
		getArtifactRepositoryManager().removeRepository(sourceRepo);
		getMetadataRepositoryManager().removeRepository(sourceRepo);
		// Cleanup disk
		delete(new File(destinationRepo).getParentFile());
		super.tearDown();
	}

	public void testRemoveIU() throws Exception {
		AntTaskElement mirror = new AntTaskElement(MIRROR_TASK);
		AntTaskElement source = new AntTaskElement("source");
		source.addElement(getRepositoryElement(sourceRepo, TYPE_BOTH));
		mirror.addElement(source);
		mirror.addElement(getRepositoryElement(destinationRepo, TYPE_BOTH));
		addTask(mirror);

		AntTaskElement removeIU = new AntTaskElement(REMOVE_IU_TASK);
		removeIU.addElement(getRepositoryElement(destinationRepo, TYPE_BOTH));
		removeIU.addElement(getIUElement("anotherplugin", null));
		addTask(removeIU);

		runAntTask();

		IMetadataRepository metadata = loadMetadataRepository(destinationRepo);
		IInstallableUnit iu = getIU(metadata, "anotherplugin");
		assertNull(iu);

		IArtifactRepository artifacts = getArtifactRepositoryManager().loadRepository(destinationRepo, null);
		IArtifactKey[] keys = artifacts.getArtifactKeys();
		for (int i = 0; i < keys.length; i++) {
			assertFalse(keys[i].getId().equals("anotherplugin"));
		}
		assertFalse(new File(getTestFolder(getName()), "plugins/anotherplugin_1.0.0.jar").exists());
	}
}
