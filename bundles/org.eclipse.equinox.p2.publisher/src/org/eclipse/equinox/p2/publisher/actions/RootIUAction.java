/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - Progress monitor handling and error handling
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.internal.p2.publisher.Messages;
import org.eclipse.equinox.internal.provisional.p2.core.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.util.NLS;

/**
 * Create a top level IU that lists all the current roots as well as any explicitly identified
 * top level IUs.
 */
public class RootIUAction extends AbstractPublisherAction {

	private Version version;
	private String id;
	private String name;
	private Collection versionAdvice;

	public RootIUAction(String id, Version version, String name) {
		this.id = id;
		this.version = version;
		this.name = name;
	}

	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		this.info = publisherInfo;
		return generateRootIU(results);
	}

	protected IStatus generateRootIU(IPublisherResult result) {
		Collection children = getChildren(result);
		InstallableUnitDescription descriptor = createTopLevelIUDescription(children, null, false);
		processCapabilityAdvice(descriptor, info);
		processTouchpointAdvice(descriptor, null, info);
		processInstallableUnitPropertiesAdvice(descriptor, info);
		processLicense(descriptor, info);
		IInstallableUnit rootIU = MetadataFactory.createInstallableUnit(descriptor);
		if (rootIU == null)
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.error_rootIU_generation, new Object[] {name, id, version}));
		result.addIU(rootIU, IPublisherResult.NON_ROOT);

		InstallableUnitDescription[] others = processAdditionalInstallableUnitsAdvice(rootIU, info);
		for (int iuIndex = 0; others != null && iuIndex < others.length; iuIndex++) {
			result.addIU(MetadataFactory.createInstallableUnit(others[iuIndex]), IPublisherResult.ROOT);
		}

		return Status.OK_STATUS;
		// TODO why do we create a category here?
		//		result.addIU(generateDefaultCategory(rootIU, rootCategory), IPublisherResult.NON_ROOT);
	}

	protected static void processLicense(InstallableUnitDescription iu, IPublisherInfo info) {
		Collection advice = info.getAdvice(null, true, iu.getId(), iu.getVersion(), ILicenseAdvice.class);
		if (advice.size() > 0) {
			// Only process the first license we find for this IU.
			ILicenseAdvice entry = (ILicenseAdvice) advice.iterator().next();
			String licenseText = entry.getLicenseText() == null ? "" : entry.getLicenseText(); //$NON-NLS-1$
			String licenseUrl = entry.getLicenseURL() == null ? "" : entry.getLicenseURL(); //$NON-NLS-1$
			if (licenseText.length() > 0 || licenseUrl.length() > 0)
				iu.setLicense(MetadataFactory.createLicense(toURIOrNull(licenseUrl), licenseText));
		}
	}

	private static URI toURIOrNull(String url) {
		if (url == null)
			return null;
		try {
			return URIUtil.fromString(url);
		} catch (URISyntaxException e) {
			return null;
		}
	}

	/**
	 * This was copied over from Generator to match up with the call from generateRootIU (above).
	 * It is entirely unclear why it was needed.  Should review.
	 * Short term fix to ensure IUs that have no corresponding category are not lost.
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=211521.
	 */
	//	private IInstallableUnit generateDefaultCategory(IInstallableUnit rootIU) {
	//		rootCategory.add(rootIU);
	//
	//		InstallableUnitDescription cat = new MetadataFactory.InstallableUnitDescription();
	//		cat.setSingleton(true);
	//		String categoryId = rootIU.getId() + ".categoryIU"; //$NON-NLS-1$
	//		cat.setId(categoryId);
	//		cat.setVersion(Version.emptyVersion);
	//		cat.setProperty(IInstallableUnit.PROP_NAME, rootIU.getProperty(IInstallableUnit.PROP_NAME));
	//		cat.setProperty(IInstallableUnit.PROP_DESCRIPTION, rootIU.getProperty(IInstallableUnit.PROP_DESCRIPTION));
	//
	//		ArrayList required = new ArrayList(rootCategory.size());
	//		for (Iterator iterator = rootCategory.iterator(); iterator.hasNext();) {
	//			IInstallableUnit iu = (IInstallableUnit) iterator.next();
	//			required.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), VersionRange.emptyRange, iu.getFilter(), false, false));
	//		}
	//		cat.setRequiredCapabilities((RequiredCapability[]) required.toArray(new RequiredCapability[required.size()]));
	//		cat.setCapabilities(new ProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, categoryId, Version.emptyVersion)});
	//		cat.setArtifacts(new IArtifactKey[0]);
	//		cat.setProperty(IInstallableUnit.PROP_TYPE_CATEGORY, "true"); //$NON-NLS-1$
	//		return MetadataFactory.createInstallableUnit(cat);
	//	}
	private Collection getChildren(IPublisherResult result) {
		// get any roots that we have accummulated so far and search for
		// children from the advice.
		HashSet children = new HashSet();
		Collection rootAdvice = info.getAdvice(null, true, null, null, IRootIUAdvice.class);
		if (rootAdvice == null)
			return children;
		for (Iterator i = rootAdvice.iterator(); i.hasNext();) {
			IRootIUAdvice advice = (IRootIUAdvice) i.next();
			Collection list = advice.getChildren(result);
			if (list != null)
				for (Iterator j = list.iterator(); j.hasNext();) {
					Object object = j.next();
					// if the advice is a string, look it up in the result.  if not there then 
					// query the known metadata repos
					if (object instanceof String) {
						String childId = (String) object;
						IInstallableUnit iu = queryForIU(result, childId, getVersionAdvice(childId));
						if (iu != null)
							children.add(iu);
					} else if (object instanceof VersionedName) {
						children.add(object);
					} else if (object instanceof IInstallableUnit)
						children.add(object);
				}
		}
		return children;
	}

	private InstallableUnitDescription createTopLevelIUDescription(Collection children, Collection requires, boolean configureLauncherData) {
		InstallableUnitDescription root = new MetadataFactory.InstallableUnitDescription();
		root.setSingleton(true);
		root.setId(id);
		root.setVersion(version);
		root.setProperty(IInstallableUnit.PROP_NAME, name);

		Collection requiredCapabilities = createIURequirements(children);
		if (requires != null)
			requiredCapabilities.addAll(requires);
		root.setRequiredCapabilities((IRequiredCapability[]) requiredCapabilities.toArray(new IRequiredCapability[requiredCapabilities.size()]));
		root.setArtifacts(new IArtifactKey[0]);

		root.setProperty("lineUp", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		root.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(id, VersionRange.emptyRange, IUpdateDescriptor.NORMAL, null));
		root.setProperty(IInstallableUnit.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		root.setCapabilities(new IProvidedCapability[] {createSelfCapability(id, version)});
		// TODO why is the type OSGI?
		root.setTouchpointType(PublisherHelper.TOUCHPOINT_OSGI);
		return root;
	}

	private Version getVersionAdvice(String iuID) {
		if (versionAdvice == null) {
			versionAdvice = info.getAdvice(null, true, null, null, IVersionAdvice.class);
			if (versionAdvice == null)
				return null;
		}
		for (Iterator i = versionAdvice.iterator(); i.hasNext();) {
			IVersionAdvice advice = (IVersionAdvice) i.next();
			// TODO have to figure a way to know the namespace here.  for now just look everywhere
			Version result = advice.getVersion(IInstallableUnit.NAMESPACE_IU_ID, iuID);
			if (result == null)
				result = advice.getVersion(IVersionAdvice.NS_BUNDLE, iuID);
			if (result == null)
				result = advice.getVersion(IVersionAdvice.NS_FEATURE, iuID);
			if (result != null)
				return result;
		}
		return null;
	}
}
