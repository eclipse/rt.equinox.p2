/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

// TODO need to merge this functionality with the FeaturesAction work on root files
public class RootFilesAction extends AbstractPublisherAction {
	private String idBase;
	private Version version;
	private String flavor;

	/**
	 * Returns the id of the top level IU published by this action for the given id and flavor.
	 * @param id the id of the application being published
	 * @param flavor the flavor being published
	 * @return the if for ius published by this action
	 */
	public static String computeIUId(String id, String flavor) {
		return flavor + id + ".rootfiles"; //$NON-NLS-1$
	}

	public RootFilesAction(IPublisherInfo info, String idBase, Version version, String flavor) {
		this.idBase = idBase == null ? "org.eclipse" : idBase; //$NON-NLS-1$
		this.version = version;
		this.flavor = flavor;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
		IPublisherResult innerResult = new PublisherResult();
		// we have N platforms, generate a CU for each
		// TODO try and find common properties across platforms
		String[] configSpecs = info.getConfigurations();
		for (int i = 0; i < configSpecs.length; i++) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			generateRootFileIUs(configSpecs[i], info, innerResult);
		}
		// merge the IUs  into the final result as non-roots and create a parent IU that captures them all
		results.merge(innerResult, IPublisherResult.MERGE_ALL_NON_ROOT);
		publishTopLevelRootFilesIU(innerResult.getIUs(null, IPublisherResult.ROOT), results);
		if (monitor.isCanceled())
			return Status.CANCEL_STATUS;
		return Status.OK_STATUS;
	}

	private void publishTopLevelRootFilesIU(Collection children, IPublisherResult result) {
		InstallableUnitDescription descriptor = createParentIU(children, computeIUId(idBase, flavor), version);
		descriptor.setSingleton(true);
		IInstallableUnit rootIU = MetadataFactory.createInstallableUnit(descriptor);
		if (rootIU == null)
			return;
		result.addIU(rootIU, IPublisherResult.ROOT);
	}

	/**
	 * Generates IUs and CUs for the files that make up the root files for a given
	 * ws/os/arch combination.
	 */
	private void generateRootFileIUs(String configSpec, IPublisherInfo info, IPublisherResult result) {
		// Create the IU for the executable
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(true);
		String idPrefix = idBase + ".rootfiles"; //$NON-NLS-1$
		String iuId = idPrefix + '.' + createIdString(configSpec);
		iu.setId(iuId);
		iu.setVersion(version);
		String filter = createFilterSpec(configSpec);
		iu.setFilter(filter);
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey(iuId, version);
		iu.setArtifacts(new IArtifactKey[] {key});
		iu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		IProvidedCapability launcherCapability = MetadataFactory.createProvidedCapability(flavor + idBase, idPrefix, version);
		iu.setCapabilities(new IProvidedCapability[] {PublisherHelper.createSelfCapability(iuId, version), launcherCapability});
		result.addIU(MetadataFactory.createInstallableUnit(iu), IPublisherResult.ROOT);

		// Create the CU that installs/configures the executable
		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = flavor + iuId;
		cu.setId(configUnitId);
		cu.setVersion(version);
		cu.setFilter(filter);
		cu.setHost(new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iuId, new VersionRange(version, true, version, true), null, false, false)});
		cu.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());

		//TODO bug 218890, would like the fragment to provide the launcher capability as well, but can't right now.
		cu.setCapabilities(new IProvidedCapability[] {PublisherHelper.createSelfCapability(configUnitId, version)});

		cu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		Map touchpointData = new HashMap();
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("install", configurationData); //$NON-NLS-1$
		String unConfigurationData = "cleanupzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("uninstall", unConfigurationData); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		IInstallableUnit unit = MetadataFactory.createInstallableUnit(cu);
		result.addIU(unit, IPublisherResult.ROOT);

		if ((info.getArtifactOptions() & (IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH)) > 0) {
			// Create the artifact descriptor.  we have several files so no path on disk
			IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, null);
			IRootFilesAdvice advice = getAdvice(configSpec, info);
			publishArtifact(descriptor, advice.getIncludedFiles(), advice.getExcludedFiles(), info, createPrefixComputer(advice.getRoot()));
		}
	}

	private IPathComputer createPrefixComputer(File root) {
		if (root == null)
			return createParentPrefixComputer(1);
		return createRootPrefixComputer(root);
	}

	/**
	 * Compiles the <class>IRootFilesAdvice</class> from the <code>info</code> into one <class>IRootFilesAdvice</class> 
	 * and returns the result.
	 * @param configSpec
	 * @param info - the publisher info holding the advice.
	 * @return a compilation of <class>IRootfilesAdvice</class> from the <code>info</code>.
	 */
	private IRootFilesAdvice getAdvice(String configSpec, IPublisherInfo info) {
		Collection advice = info.getAdvice(configSpec, true, null, null, IRootFilesAdvice.class);
		ArrayList inclusions = new ArrayList();
		ArrayList exclusions = new ArrayList();
		File root = null;
		for (Iterator i = advice.iterator(); i.hasNext();) {
			IRootFilesAdvice entry = (IRootFilesAdvice) i.next();
			// TODO for now we simply get root from the first advice that has one
			if (root == null)
				root = entry.getRoot();
			File[] list = entry.getIncludedFiles();
			if (list != null)
				inclusions.addAll(Arrays.asList(list));
			list = entry.getExcludedFiles();
			if (list != null)
				exclusions.addAll(Arrays.asList(list));
		}
		File[] includeList = (File[]) inclusions.toArray(new File[inclusions.size()]);
		File[] excludeList = (File[]) exclusions.toArray(new File[exclusions.size()]);
		return new RootFilesAdvice(root, includeList, excludeList, configSpec);
	}

}
