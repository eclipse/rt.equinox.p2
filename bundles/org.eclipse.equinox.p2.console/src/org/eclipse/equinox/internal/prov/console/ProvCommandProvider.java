/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.prov.console;

import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.prov.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.prov.core.ProvisionException;
import org.eclipse.equinox.prov.engine.IProfileRegistry;
import org.eclipse.equinox.prov.engine.Profile;
import org.eclipse.equinox.prov.metadata.*;
import org.eclipse.equinox.prov.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.prov.query.Query;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.service.resolver.VersionRange;

/**
 * An OSGi console command provider that adds various commands for interacting
 * with the provisioning system.
 */
public class ProvCommandProvider implements CommandProvider {
	public static final String NEW_LINE = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
	private static final String IU_KIND_NAMESPACE = "org.eclipse.equinox.prov.type";

	//	private Profile profile;

	public ProvCommandProvider(String profileId, IProfileRegistry registry) {
		// look up the profile we are currently running and use it as the
		// default.
		// TODO define a way to spec the default profile to manage
		//		if (profileId != null) {
		//			profile = registry.getProfile(profileId);
		//			if (profile != null)
		//				return;
		//		}
		//		// A default was not defined so manage the first profile we can find
		//		Profile[] profiles = registry.getProfiles();
		//		if (profiles.length > 0)
		//			profile = profiles[0];
	}

	/**
	 * Adds a metadata repository.
	 */
	public void _provaddrepo(CommandInterpreter interpreter) {
		String urlString = interpreter.nextArgument();
		if (urlString == null) {
			interpreter.print("Repository location must be provided");
			interpreter.println();
			return;
		}
		URL repoURL = toURL(interpreter, urlString);
		if (repoURL == null)
			return;
		if (ProvisioningHelper.addMetadataRepository(repoURL) == null)
			interpreter.println("Unable to add repository: " + repoURL);
	}

	public void _provaddartifactrepo(CommandInterpreter interpreter) {
		String urlString = interpreter.nextArgument();
		if (urlString == null) {
			interpreter.print("Repository location must be provided");
			interpreter.println();
			return;
		}
		URL repoURL = toURL(interpreter, urlString);
		if (repoURL == null)
			return;
		if (ProvisioningHelper.addArtifactRepository(repoURL) == null)
			interpreter.println("Unable to add repository: " + repoURL);
	}

	/**
	 * Install a given IU to a given profile location.
	 */
	public void _provinstall(CommandInterpreter interpreter) {
		String iu = interpreter.nextArgument();
		String version = interpreter.nextArgument();
		String profileId = interpreter.nextArgument();
		if (profileId == null || profileId.equals("this"))
			profileId = IProfileRegistry.SELF;
		if (iu == null || version == null || profileId == null) {
			interpreter.println("Installable unit id, version, and profile Id must be provided");
			return;
		}
		IStatus s = null;
		try {
			s = ProvisioningHelper.install(iu, version, ProvisioningHelper.getProfile(profileId), new NullProgressMonitor());
		} catch (ProvisionException e) {
			interpreter.println("installation failed ");
			e.printStackTrace();
			return;
		}
		if (s.isOK())
			interpreter.println("installation complete");
		else
			interpreter.println("installation failed " + s.getMessage());
	}

	/**
	 * Creates a profile given an id, location, and flavor
	 */
	public void _provaddprofile(CommandInterpreter interpreter) {
		String profileId = interpreter.nextArgument();
		String location = interpreter.nextArgument();
		String flavor = interpreter.nextArgument();
		if (profileId == null || location == null || flavor == null) {
			interpreter.println("Id, location, and flavor must be provided");
			return;
		}
		String environments = interpreter.nextArgument();
		Properties props = new Properties();
		props.setProperty(Profile.PROP_INSTALL_FOLDER, location);
		props.setProperty(Profile.PROP_FLAVOR, flavor);
		if (environments != null)
			props.setProperty(Profile.PROP_ENVIRONMENTS, environments);

		ProvisioningHelper.addProfile(profileId, props);
	}

	/**
	 * Lists the known metadata repositories, or the contents of a given
	 * metadata repository.
	 * 
	 * @param interpreter
	 */
	public void _provliu(CommandInterpreter interpreter) {
		String urlString = processArgument(interpreter.nextArgument());
		String id = processArgument(interpreter.nextArgument());
		String version = processArgument(interpreter.nextArgument());
		URL repoURL = null;
		if (urlString != null && !urlString.equals("*"))
			repoURL = toURL(interpreter, urlString);
		IInstallableUnit[] units = sort(ProvisioningHelper.getInstallableUnits(repoURL, id, new VersionRange(version), null));
		for (int i = 0; i < units.length; i++)
			println(interpreter, units[i]);
	}

	/**
	 * Lists the known metadata repositories, or the contents of a given
	 * metadata repository.
	 * 
	 * @param interpreter
	 */
	public void _provlr(CommandInterpreter interpreter) {
		String urlString = processArgument(interpreter.nextArgument());
		String id = processArgument(interpreter.nextArgument());
		String version = processArgument(interpreter.nextArgument());
		if (urlString == null) {
			IMetadataRepository[] repositories = ProvisioningHelper.getMetadataRepositories();
			if (repositories == null)
				return;
			for (int i = 0; i < repositories.length; i++)
				interpreter.println(repositories[i].getLocation());
			return;
		}
		URL repoURL = toURL(interpreter, urlString);
		if (repoURL == null)
			return;
		IInstallableUnit[] units = sort(ProvisioningHelper.getInstallableUnits(repoURL, id, new VersionRange(version), null));
		for (int i = 0; i < units.length; i++)
			println(interpreter, units[i]);
	}

	/**
	 * Lists the group IUs in all known metadata repositories, or in the given
	 * metadata repository.
	 * 
	 * @param interpreter
	 */
	public void _provlg(CommandInterpreter interpreter) {
		String urlString = processArgument(interpreter.nextArgument());
		IMetadataRepository[] repositories;
		if (urlString == null) {
			repositories = ProvisioningHelper.getMetadataRepositories();
			if (repositories == null)
				return;
		} else {
			URL repoURL = toURL(interpreter, urlString);
			if (repoURL == null)
				return;
			IMetadataRepository repo = null;
			repo = ProvisioningHelper.getMetadataRepository(repoURL);
			if (repo == null)
				return;
			repositories = new IMetadataRepository[] {repo};
		}
		RequiredCapability requirement = new RequiredCapability(IU_KIND_NAMESPACE, "group", null, null, false, false);
		IInstallableUnit[] units = sort(Query.query(repositories, null, null, new RequiredCapability[] {requirement}, false, null));
		for (int i = 0; i < units.length; i++)
			println(interpreter, units[i]);
	}

	/**
	 * Lists the known artifact repositories, or the contents of a given
	 * artifact repository.
	 * 
	 * @param interpreter
	 */
	public void _provlar(CommandInterpreter interpreter) {
		String urlString = processArgument(interpreter.nextArgument());
		if (urlString == null) {
			IArtifactRepository[] repositories = ProvisioningHelper.getArtifactRepositories();
			if (repositories == null)
				return;
			for (int i = 0; i < repositories.length; i++)
				interpreter.println(repositories[i].getLocation());
			return;
		}
		URL repoURL = toURL(interpreter, urlString);
		if (repoURL == null)
			return;
		IArtifactRepository repo = ProvisioningHelper.getArtifactRepository(repoURL);
		IArtifactKey[] keys = null;
		try {
			keys = (repo != null) ? repo.getArtifactKeys() : null;
		} catch (UnsupportedOperationException e) {
			interpreter.println("Repository does not support list commands.");
			return;
		}
		if (keys == null || keys.length == 0) {
			interpreter.println("Repository has no artifacts");
			return;
		}
		for (int i = 0; i < keys.length; i++)
			println(interpreter, keys[i], repo.getArtifact(keys[i]));
	}

	/**
	 * Returns the given string as an URL, or <code>null</code> if the string
	 * could not be interpreted as an URL.
	 */
	private URL toURL(CommandInterpreter interpreter, String urlString) {
		try {
			return new URL(urlString);
		} catch (MalformedURLException e) {
			interpreter.print(e.getMessage());
			interpreter.println();
			return null;
		}
	}

	private String processArgument(String arg) {
		if (arg == null || arg.equals("*"))
			return null;
		return arg;
	}

	/**
	 * Lists the known profiles, or the contents of a given profile.
	 * 
	 * @param interpreter
	 */
	public void _provlp(CommandInterpreter interpreter) {
		String profileId = processArgument(interpreter.nextArgument());
		String id = processArgument(interpreter.nextArgument());
		String range = processArgument(interpreter.nextArgument());
		if (profileId == null) {
			Profile[] profiles = ProvisioningHelper.getProfiles();
			for (int i = 0; i < profiles.length; i++)
				interpreter.println(profiles[i].getProfileId());
			return;
		}
		// determine which profile is to be listed
		Profile target = null;
		if (profileId.equals("this"))
			profileId = IProfileRegistry.SELF;
		target = ProvisioningHelper.getProfile(profileId);
		if (target == null)
			return;

		// list the profile contents
		IInstallableUnit[] result = sort(Query.query(new Profile[] {target}, id, new VersionRange(range), null, false, null));
		for (int i = 0; i < result.length; i++)
			interpreter.println(result[i]);
	}

	private IInstallableUnit[] sort(IInstallableUnit[] units) {
		Arrays.sort(units, new Comparator() {
			public int compare(Object arg0, Object arg1) {
				return arg0.toString().compareTo(arg1.toString());
			}
		});
		return units;
	}

	public String getHelp() {
		StringBuffer help = new StringBuffer();
		help.append(NEW_LINE);
		help.append("---"); //$NON-NLS-1$
		help.append("Provisioning Commands");
		help.append("---"); //$NON-NLS-1$
		help.append(NEW_LINE);
		help.append("\tprovlr [<repository URL> <iu id | *> <version range | *>]   - Lists all metadata repositories, or the contents of a given metadata repository");
		help.append(NEW_LINE);
		help.append("\tprovlar [<repository URL>] - Lists all artifact repositories, or the contents of a given artifact repository");
		help.append(NEW_LINE);
		help.append("\tprovliu [<repository URL | *> <iu id | *> <version range | *>] - Lists the IUs that match the pattern in the given repo.  * matches all");
		help.append(NEW_LINE);
		help.append("\tprovlp [<profile id | *> - Lists all profiles, or the contents of the profile at the given profile");
		help.append(NEW_LINE);
		help.append("\tprovlg [<repository URL> <iu id | *> <version range | *>] - Lists all IUs with group capabilities in the given repo or in all repos if the URL is omitted");
		help.append(NEW_LINE);
		help.append("\tprovinstall <InstallableUnit> <version> <profileId> - Provisions an IU to the profile with the give id");
		help.append(NEW_LINE);
		help.append("\tprovaddrepo <repository URL> - Adds a metadata repository");
		help.append(NEW_LINE);
		help.append("\tprovaddartifactrepo <repository URL> - Adds an artifact repository");
		help.append(NEW_LINE);
		help.append("\tprovaddprofile <profileId> <location> <flavor> - Adds a profile with the given id, location and flavor");
		help.append(NEW_LINE);
		return help.toString();
	}

	/**
	 * Prints a string representation of an {@link InstallableUnit} to the
	 * iterpreter's output stream.
	 */
	public void print(CommandInterpreter interpreter, IInstallableUnit unit) {
		interpreter.print(unit.getId() + ' ' + unit.getVersion());
	}

	/**
	 * Prints a string representation of an {@link InstallableUnit} to the
	 * iterpreter's output stream, following by a line terminator
	 */
	public void println(CommandInterpreter interpreter, IInstallableUnit unit) {
		print(interpreter, unit);
		interpreter.println();
	}

	private void println(CommandInterpreter interpreter, IArtifactKey artifactKey, URI artifact) {
		interpreter.print(artifactKey.toString() + ' ' + artifact);
		interpreter.println();
	}
}