/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.frameworkadmin.equinox.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.internal.adaptor.EclipseEnvironmentInfo;
import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.frameworkadmin.equinox.internal.utils.*;
import org.eclipse.equinox.internal.frameworkadmin.utils.SimpleBundlesState;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;

class EclipseVersion implements Comparable {
	int major = 0;
	int minor = 0;
	int service = 0;
	String qualifier = null;

	EclipseVersion(String version) {
		StringTokenizer tok = new StringTokenizer(version, ".");
		if (!tok.hasMoreTokens())
			return;
		this.major = Integer.parseInt(tok.nextToken());
		if (!tok.hasMoreTokens())
			return;
		this.minor = Integer.parseInt(tok.nextToken());
		if (!tok.hasMoreTokens())
			return;
		this.service = Integer.parseInt(tok.nextToken());
		if (!tok.hasMoreTokens())
			return;
		this.qualifier = tok.nextToken();
	}

	public int compareTo(Object obj) {
		EclipseVersion target = (EclipseVersion) obj;
		if (target.major > this.major)
			return -1;
		if (target.major < this.major)
			return 1;
		if (target.minor > this.minor)
			return -1;
		if (target.minor < this.minor)
			return 1;
		if (target.service > this.service)
			return -1;
		if (target.service < this.service)
			return 1;
		return 0;
	}

}

public class EquinoxBundlesState implements BundlesState {
	private static final boolean DEBUG = false;
	// While we recognize the amd64 architecture, we change
	// this internally to be x86_64.
	private static final String INTERNAL_AMD64 = "amd64"; //$NON-NLS-1$
	private static final String INTERNAL_ARCH_I386 = "i386"; //$NON-NLS-1$
	public static final String[] PROPS = {"osgi.os", "osgi.ws", "osgi.nl", "osgi.arch", Constants.FRAMEWORK_SYSTEMPACKAGES, "osgi.resolverMode", Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "osgi.resolveOptional", "osgi.genericAliases"};
	private static final int MAX_COUNT_LOOP = 10;
	private static final long PERIOD_TO_SLEEP = 10000;// in msec.

	static boolean checkFullySupported() {
		try {
			BundleHelper.getDefault();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * eclipse.exe will launch a fw where plugins/org.eclipse.osgi_*.*.*.*.jar is an implementation of fw.
	 * 
	 * @param launcherData
	 * @param configData
	 * @return File of fwJar to be used.
	 */
	static File getFwJar(LauncherData launcherData, ConfigData configData) {

		//		EclipseLauncherParser launcherParser = new EclipseLauncherParser(launcherData);
		//		launcherParser.read();
		if (launcherData.getFwJar() != null)
			return launcherData.getFwJar();

		// check -D arguments of jvmArgs ?
		String[] jvmArgs = launcherData.getJvmArgs();
		String location = null;
		for (int i = 0; i < jvmArgs.length; i++) {
			if (jvmArgs[i].endsWith("-D" + "osgi.framework=")) {
				location = jvmArgs[i].substring(("-D" + "osgi.framework=").length());
			}
		}
		if (location != null)
			return new File(location);

		BundleInfo[] bundleInfos = configData.getBundles();
		for (int i = 0; i < bundleInfos.length; i++) {
			if (bundleInfos[i].getLocation().startsWith("file:")) {
				String[] clauses = Utils.getClausesManifestMainAttributes(bundleInfos[i].getLocation(), Constants.BUNDLE_SYMBOLICNAME);
				if (bundleInfos[i].getLocation().indexOf(EquinoxConstants.FW_JAR_PLUGIN_NAME) > 0) {
					if ("org.eclipse.osgi".equals(Utils.getPathFromClause(clauses[0]))) {
						return new File(bundleInfos[i].getLocation().substring("file:".length()));
					}
				}
			}
		}

		File pluginsDir;
		if (launcherData.getLauncher() == null) {
			if (launcherData.getHome() == null)
				return null;
			else {
				pluginsDir = new File(launcherData.getHome(), "plugins");
			}
		} else
			pluginsDir = new File(launcherData.getLauncher().getParentFile(), "plugins");

		String fullLocation = Utils.getBundleFullLocation(EquinoxConstants.FW_JAR_PLUGIN_NAME, pluginsDir);
		if (fullLocation == null)
			return null;
		URL url = null;
		try {
			url = new URL(fullLocation);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			Log.log(LogService.LOG_WARNING, "fullLocation is not in proper format:" + fullLocation);
		}
		return url == null ? null : new File(url.getFile());
		//		File[] files = pluginsDir.listFiles();
		//		File ret = null;
		//		EclipseVersion maxVersion = null;
		//		for (int i = 0; i < files.length; i++)
		//			if (files[i].getName().startsWith("org.eclipse.osgi_")) {
		//				String version = files[i].getName().substring("org.eclipse.osgi_".length(), files[i].getName().lastIndexOf(".jar"));
		//				if (ret == null || ((new EclipseVersion(version)).compareTo(maxVersion) > 0)) {
		//					ret = files[i];
		//					maxVersion = new EclipseVersion(version);
		//					continue;
		//				}
		//			}
		//		return ret;
	}

	private static long getMaxId(State state) {
		BundleDescription[] bundleDescriptions = state.getBundles();
		long maxId = -1;
		for (int i = 0; i < bundleDescriptions.length; i++)
			if (maxId < bundleDescriptions[i].getBundleId())
				maxId = bundleDescriptions[i].getBundleId();
		return maxId;
	}

	public static String getStateString(State state) {
		BundleDescription[] descriptions = state.getBundles();
		StringBuffer sb = new StringBuffer();
		sb.append("# state=\n");
		for (int i = 0; i < descriptions.length; i++)
			sb.append("# " + descriptions[i].toString() + "\n");
		return sb.toString();
	}

	// "osgi.os", "osgi.ws", "osgi.nl", "osgi.arch", Constants.FRAMEWORK_SYSTEMPACKAGES, "osgi.resolverMode", 
	// Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "osgi.resolveOptional"
	static Properties setDefaultPlatformProperties() {
		Properties platformProperties = new Properties();
		// set default value

		String nl = Locale.getDefault().toString();
		platformProperties.setProperty("osgi.nl", nl); //$NON-NLS-1$

		// TODO remove EclipseEnvironmentInof
		String os = EclipseEnvironmentInfo.guessOS(System.getProperty("os.name"));//$NON-NLS-1$);
		platformProperties.setProperty("osgi.os", os); //$NON-NLS-1$

		String ws = EclipseEnvironmentInfo.guessWS(os);
		platformProperties.setProperty("osgi.ws", ws);

		// if the user didn't set the system architecture with a command line 
		// argument then use the default.
		String arch = null;
		String name = FrameworkProperties.getProperty("os.arch");//$NON-NLS-1$
		// Map i386 architecture to x86
		if (name.equalsIgnoreCase(INTERNAL_ARCH_I386))
			arch = org.eclipse.osgi.service.environment.Constants.ARCH_X86;
		// Map amd64 architecture to x86_64
		else if (name.equalsIgnoreCase(INTERNAL_AMD64))
			arch = org.eclipse.osgi.service.environment.Constants.ARCH_X86_64;
		else
			arch = name;
		platformProperties.setProperty("osgi.arch", arch); //$NON-NLS-1$			

		platformProperties.setProperty(Constants.FRAMEWORK_SYSTEMPACKAGES, FrameworkProperties.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES));
		platformProperties.setProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, FrameworkProperties.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT));
		platformProperties.setProperty("osgi.resolveOptional", Boolean.toString("true".equals(FrameworkProperties.getProperty("osgi.resolveOptional"))));
		return platformProperties;
	}

	EquinoxFwAdminImpl fwAdmin = null;
	BundleContext context;

	Manipulator manipulator = null;

	Properties platfromProperties = new Properties();
	long maxId = -1;

	StateObjectFactory soFactory = null;

	State state = null;

	/**
	 * If useFwPersistentData flag equals false,
	 * this constructor will not take a framework persistent data into account.	
	 * Otherwise, it will.
	 * 
	 * @param context
	 * @param fwAdmin
	 * @param manipulator
	 * @param useFwPersistentData
	 */
	EquinoxBundlesState(BundleContext context, EquinoxFwAdminImpl fwAdmin, Manipulator manipulator, boolean useFwPersistentData) {
		this.context = context;
		this.fwAdmin = fwAdmin;
		// copy manipulator object for avoiding modifying the parameters of the manipulator.
		this.manipulator = fwAdmin.getManipulator();
		this.manipulator.setConfigData(manipulator.getConfigData());
		this.manipulator.setLauncherData(manipulator.getLauncherData());
		initialize(useFwPersistentData);
	}

	//	EquinoxBundlesState(BundleContext context, EquinoxFwAdminImpl fwAdmin, Manipulator manipulator) {
	//		this(context, fwAdmin, manipulator, true);
	//		//		this.context = context;
	//		//		this.fwAdmin = fwAdmin;
	//		//		// copy manipulator object for avoiding modifying the parameters of the manipulator.
	//		//		this.manipulator = fwAdmin.getManipulator();
	//		//		this.manipulator.setConfigData(manipulator.getConfigData());
	//		//		this.manipulator.setLauncherData(manipulator.getLauncherData());
	//		//		initialize();
	//	}

	//	EquinoxBundlesState(BundleContext context, EquinoxFwAdminImpl fwAdmin, Manipulator manipulator, boolean useFwPersistentData, boolean runtime) {
	//		super();
	//		if (DEBUG)
	//			System.out.println("\nEquinoxBundlesState():useFwPersistentData=" + useFwPersistentData + ",runtime=" + runtime);
	//		this.context = context;
	//		this.fwAdmin = fwAdmin;
	//		// copy manipulator object for avoiding modifying the parameters of the manipulator.
	//		this.manipulator = fwAdmin.getManipulator();
	//		if (runtime) {
	//			this.manipulator.setLauncherData(manipulator.getLauncherData());
	//			this.initializeRuntime();
	//		} else {
	//			this.manipulator.setConfigData(manipulator.getConfigData());
	//			this.manipulator.setLauncherData(manipulator.getLauncherData());
	//			initialize(useFwPersistentData);
	//		}
	//	}

	/**
	 * This constructor does NOT take a framework persistent data into account.
	 * It will create State object based on the specified platformProperties.
	 * 
	 * @param context
	 * @param fwAdmin
	 * @param manipulator
	 * @param platformProperties
	 */
	EquinoxBundlesState(BundleContext context, EquinoxFwAdminImpl fwAdmin, Manipulator manipulator, Properties platformProperties) {
		super();
		this.context = context;
		this.fwAdmin = fwAdmin;
		// copy manipulator object for avoiding modifying the parameters of the manipulator.
		this.manipulator = fwAdmin.getManipulator();
		this.manipulator.setConfigData(manipulator.getConfigData());
		this.manipulator.setLauncherData(manipulator.getLauncherData());
		LauncherData launcherData = manipulator.getLauncherData();
		ConfigData configData = manipulator.getConfigData();
		BundleInfo[] bInfos = configData.getBundles();
		this.composeNewState(launcherData, configData, platformProperties, bInfos);
	}

	/**
	 * compose new state without reading framework persistent data.
	 * The configData.getFwDependentProps() is used for the composition.   
	 * 
	 * @param launcherData
	 * @param configData
	 * @param bInfos
	 */
	private void composeNewState(LauncherData launcherData, ConfigData configData, BundleInfo[] bInfos) {
		this.composeNewState(launcherData, configData, configData.getFwDependentProps(), bInfos);
	}

	/**
	 * compose new state without reading framework persistent data.
	 * The given properties is used for the composition.
	 * If system bundle is not included in the given bInfos, the fw jar launcherData contains will be used.
	 * 
	 * @param launcherData
	 * @param configData
	 * @param properties
	 * @param bInfos
	 */
	private void composeNewState(LauncherData launcherData, ConfigData configData, Properties properties, BundleInfo[] bInfos) {
		composeState(bInfos, properties, null);
		resolve(true);
		if (getSystemBundle() == null) {
			File fwJar = getFwJar(launcherData, configData);
			if (fwJar == null)
				throw new IllegalStateException("fwJar cannot be set.");

			BundleInfo[] newBInfos = new BundleInfo[bInfos.length + 1];
			try {
				newBInfos[0] = new BundleInfo(fwJar.toURL().toExternalForm(), 0, true, 0);
			} catch (MalformedURLException e) {
				// Nothign to do because never happens.
				e.printStackTrace();
			}
			System.arraycopy(bInfos, 0, newBInfos, 1, bInfos.length);
			configData.setBundles(newBInfos);
			composeState(newBInfos, properties, null);
			resolve(true);
		}
	}

	/**
	 * compose state.
	 * If it cannot compose it by somehow, false is returned.
	 * 
	 * @param bInfos
	 * @param props
	 * @param fwPersistentDataLocation
	 * @return if it cannot compose it by somehow, false is returned.
	 * @throws IllegalArgumentException
	 * @throws FrameworkAdminRuntimeException
	 */
	private boolean composeState(BundleInfo[] bInfos, Dictionary props, File fwPersistentDataLocation) throws IllegalArgumentException, FrameworkAdminRuntimeException {
		SimpleBundlesState.checkAvailability(fwAdmin);
		this.setStateObjectFactory();
		BundleDescription[] cachedInstalledBundles = null;
		state = null;
		if (fwPersistentDataLocation != null) {
			if (DEBUG)
				Log.log(LogService.LOG_DEBUG, this, "composeExpectedState()", "fwPersistentDataLocation=" + fwPersistentDataLocation);
			//			// TODO just for test. it should be removed.
			//			File file = new File("C:/eclipse/3.3M3/configuration2");
			//			System.out.println("file=" + file);
			//			System.out.println("file.equals(fwPersistentDataLocation)=" + file.equals(fwPersistentDataLocation));
			//			fwPersistentDataLocation = file;
			File file = new File(fwPersistentDataLocation, "org.eclipse.osgi");
			if (!file.exists())
				return false;
			if (!file.isDirectory())
				return false;

			AlienStateReader alienStateReader = new AlienStateReader(fwPersistentDataLocation, null);

			// Current Equinox (3.3M5) doesn't write state into persistently immediately.
			// Therefore, repeat readState certain times.
			int count = 0;
			while (true) {
				try {
					state = alienStateReader.readState();
					if (state != null)
						break;
					count++;
					if (count >= MAX_COUNT_LOOP) {
						Log.log(LogService.LOG_WARNING, this, "composeState()", "Fail to readState");
						break;
					}
				} catch (IOException e) {
					count++;
					if (count >= MAX_COUNT_LOOP) {
						Log.log(LogService.LOG_WARNING, this, "composeState()", "Fail to readState", e);
						break;
					}
				}
				try {
					Log.log(LogService.LOG_INFO, this, "composeState()", "readState failed(" + count + "):Retry after " + PERIOD_TO_SLEEP + " msec.");
					Thread.sleep(PERIOD_TO_SLEEP);
				} catch (InterruptedException e1) {
					// Nothing to do.
				}
			}
			if (state == null)
				return false;
			cachedInstalledBundles = state.getBundles();
			if (DEBUG) {
				Log.log(LogService.LOG_DEBUG, this, "composeExpectedState()", "state!=null");
				for (int i = 0; i < cachedInstalledBundles.length; i++)
					Log.log(LogService.LOG_DEBUG, "cachedInstalledBundles[" + i + "]=" + cachedInstalledBundles[i]);
			}
			setPlatformProperties(state);
		} else {
			//return false;
			state = soFactory.createState(true);
			cachedInstalledBundles = new BundleDescription[0];
			if (props == null)
				return false;
			setPlatformPropertiesToState(props);
			setPlatformProperties(state);
		}

		// remove initial bundle which were installed but not listed in fwConfigFileBInfos.
		//bundleList.addAll(Arrays.asList(cachedInstalledBundles));
		for (int i = 0; i < cachedInstalledBundles.length; i++) {
			if (cachedInstalledBundles[i].getLocation().startsWith("initial@")) {
				String location = FileUtils.getRealLocation(cachedInstalledBundles[i].getLocation());
				boolean found = false;
				for (int j = 0; j < bInfos.length; j++) {
					if (location.equals(bInfos[j].getLocation())) {
						found = true;
						break;
					}
				}
				if (!found)
					state.removeBundle(cachedInstalledBundles[i].getBundleId());
			}
		}

		try {
			maxId = state.getHighestBundleId();
		} catch (NoSuchMethodError e) {
			maxId = getMaxId(state);
		}
		if (DEBUG) {
			System.out.println("");
			Log.log(LogService.LOG_DEBUG, this, "composeExpectedState()", "installBundle():");
		}
		for (int j = 0; j < bInfos.length; j++) {
			if (DEBUG)
				Log.log(LogService.LOG_DEBUG, this, "composeExpectedState()", "bInfos[" + j + "]=" + bInfos[j]);
			try {
				this.installBundle(bInfos[j]);
				//System.out.println("install bInfos[" + j + "]=" + bInfos[j]);
			} catch (RuntimeException e) {
				Log.log(LogService.LOG_ERROR, this, "composeExpectedState()", "BundleInfo:" + bInfos[j], e);
				e.printStackTrace();
				throw e;
			}
		}
		return true;
	}

	public BundleInfo convert(BundleDescription toConvert) {
		boolean markedAsStarted = false;
		int sl = BundleInfo.NO_LEVEL;

		String location = null;
		// This algorithm is not sophicificated.
		if (toConvert.getBundleId() == 0) {//System Bundle
			String symbolicNameTarget = toConvert.getSymbolicName();
			Version versionTarget = toConvert.getVersion();
			try {
				String fwJarLocation = manipulator.getLauncherData().getFwJar().toURL().toExternalForm();
				String[] clauses = Utils.getClausesManifestMainAttributes(fwJarLocation, Constants.BUNDLE_SYMBOLICNAME);
				String fwJarSymbolicName = Utils.getPathFromClause(clauses[0]);
				String fwJarVersionSt = Utils.getManifestMainAttributes(fwJarLocation, Constants.BUNDLE_VERSION);
				if (fwJarSymbolicName.equals(symbolicNameTarget) && fwJarVersionSt.equals(versionTarget.toString()))
					location = fwJarLocation;
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (FrameworkAdminRuntimeException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} else {
			location = FileUtils.getRealLocation(toConvert.getLocation());
			BundleInfo[] originalBInfos = manipulator.getConfigData().getBundles();
			//			if (DEBUG)
			//				System.out.println("toConvert=" + location);
			boolean found = false;
			for (int i = 0; i < originalBInfos.length; i++) {
				//				if (DEBUG)
				//					System.out.println("originalBInfos[" + i + "]=" + originalBInfos[i].getLocation());
				if (originalBInfos[i].getLocation().equals(location)) {
					markedAsStarted = originalBInfos[i].isMarkedAsStarted();
					sl = getStartLevel(originalBInfos[i].getStartLevel());
					found = true;
					break;
				}
			}
			if (!found) {
				// TODO current equinox impl has no way to get the start level info persistently stored.
				markedAsStarted = false;
				sl = BundleInfo.NO_LEVEL;
			}
		}
		BundleInfo result = new BundleInfo();
		result.setSymbolicName(toConvert.getSymbolicName());
		result.setVersion(toConvert.getVersion().toString());
		result.setLocation(location);
		result.setResolved(toConvert.isResolved());
		result.setMarkedAsStarted(markedAsStarted);
		result.setStartLevel(sl);
		result.setBundleId(toConvert.getBundleId());
		return result;
	}

	public BundleInfo[] convertState(BundleDescription[] bundles) {
		//		BundleDescription[] bundles = state.getBundles();

		BundleInfo[] result = new BundleInfo[bundles.length];
		for (int i = 0; i < bundles.length; i++)
			result[i] = convert(bundles[i]);
		return result;
	}

	public BundleInfo[] convertState(State state) {
		return convertState(state.getBundles());
	}

	/**
	 * return platform properties which is used for the running framework.
	 * 
	 * @return platform properties which is used for the running framework.
	 */
	private Properties getCurrentPlatformProperties() {
		Properties props = new Properties();
		for (int i = 0; i < PROPS.length; i++) {
			String value = context.getProperty(PROPS[i]);
			System.out.println("(" + PROPS[i] + "," + value + ")");
			if (value != null)
				props.setProperty(PROPS[i], value);
		}
		return props;
	}

	public BundleInfo[] getExpectedState() throws FrameworkAdminRuntimeException {
		SimpleBundlesState.checkAvailability(fwAdmin);
		return convertState(state);
	}

	Properties getPlatformProperties() {
		return platfromProperties;
	}

	public BundleInfo[] getPrerequisteBundles(BundleInfo bInfo) {
		Set set = new HashSet();
		BundleDescription bundle = state.getBundleByLocation(bInfo.getLocation());
		ImportPackageSpecification[] imports = bundle.getImportPackages();
		for (int i = 0; i < imports.length; i++) {
			BaseDescription supplier = imports[i].getSupplier();
			set.add(supplier.getSupplier());
			//			System.out.println(supplier.getSupplier());
		}
		BundleDescription[] requires = bundle.getResolvedRequires();
		for (int i = 0; i < requires.length; i++)
			set.add(requires[i]);
		BundleDescription[] bundles = new BundleDescription[set.size()];
		set.toArray(bundles);
		return convertState(bundles);
	}

	private int getStartLevel(int startLevel) {
		if (startLevel == BundleInfo.NO_LEVEL)
			return manipulator.getConfigData().getInitialBundleStartLevel();

		return startLevel;
	}

	public BundleInfo getSystemBundle() {
		BundleDescription bundle = this.getSystemBundleDescription();
		if (bundle == null)
			return null;
		return convert(bundle);
	}

	private BundleDescription getSystemBundleDescription() {
		BundleDescription bundle = state.getBundle(0);
		if (bundle == null)
			return null;
		if (bundle.getHost() != null)// this is a fragment bundle.
			return null;
		//		if (DEBUG) {
		//			System.out.println("EquinoxConstants.FW_SYMBOLIC_NAME=" + EquinoxConstants.FW_SYMBOLIC_NAME);
		//			System.out.println("bundle.getSymbolicName()=" + bundle.getSymbolicName());
		//		}
		if (EquinoxConstants.FW_SYMBOLIC_NAME.equals(bundle.getSymbolicName()))
			return bundle;
		return null;
	}

	public BundleInfo[] getSystemFragmentedBundles() {
		BundleDescription bundle = this.getSystemBundleDescription();
		if (bundle == null)
			return null;
		return convertState(bundle.getFragments());
	}

	public String[] getUnsatisfiedConstraints(BundleInfo bInfo) {
		BundleDescription description = state.getBundleByLocation(bInfo.getLocation());
		PlatformAdmin platformAdmin = (PlatformAdmin) BundleHelper.getDefault().acquireService(PlatformAdmin.class.getName());
		StateHelper helper = platformAdmin.getStateHelper();
		VersionConstraint[] constraints = helper.getUnsatisfiedConstraints(description);
		String[] ret = new String[constraints.length];
		for (int i = 0; i < constraints.length; i++)
			ret[i] = constraints[i].toString();
		return ret;
	}

	private void initialize(boolean useFwPersistentData) {
		LauncherData launcherData = manipulator.getLauncherData();
		ConfigData configData = manipulator.getConfigData();
		BundleInfo[] bInfos = configData.getBundles();

		if (!useFwPersistentData) {
			composeNewState(launcherData, configData, bInfos);
			return;
		}

		EquinoxManipulatorImpl.checkConsistencyOfFwConfigLocAndFwPersistentDataLoc(launcherData);
		if (launcherData.isClean()) {
			composeNewState(launcherData, configData, bInfos);
		} else {
			if (manipulator.getLauncherData().getFwPersistentDataLocation() == null) {
				//	TODO default value should be set more precisely.
				File installArea = null;
				String installAreaSt = configData.getFwDependentProp(EquinoxConstants.PROP_INSTALL);
				if (installAreaSt == null) {
					if (manipulator.getLauncherData().getLauncher() == null) {
						// TODO implement
					} else {
						installArea = manipulator.getLauncherData().getLauncher().getParentFile();
					}
				} else {
					if (installAreaSt.startsWith("file:"))
						installArea = new File(installAreaSt.substring("file:".length()));
					else
						throw new IllegalStateException("Current implementation assume that property value keyed by " + EquinoxConstants.PROP_INSTALL + " must start with \"file:\". But it was not:" + installAreaSt);
				}
				if (DEBUG)
					Log.log(LogService.LOG_DEBUG, this, "initialize(useFwPersistentDat)", "installArea=" + installArea);
				File fwPersistentDataLocation = new File(installArea, "configuration");
				manipulator.getLauncherData().setFwPersistentDataLocation(fwPersistentDataLocation, false);
			}
			if (!composeState(bInfos, null, manipulator.getLauncherData().getFwPersistentDataLocation()))
				composeNewState(launcherData, configData, bInfos);
			resolve(true);
			//if(this.getSystemBundle()==null)
		}
	}

	//	private void initializeRuntime() {
	//		ServiceReference reference = context.getServiceReference(StartLevel.class.getName());
	//		StartLevel startLevel = (StartLevel) context.getService(reference);
	//		//		boolean flag = startLevel.isBundlePersistentlyStarted(context.getBundle(0));
	//		System.out.println("\ninitlializeRuntime()");
	//
	//		Bundle[] bundles = context.getBundles();
	//		List bundlesList = new LinkedList();
	//		for (int i = 0; i < bundles.length; i++) {
	//			System.out.println(" bundles[" + i + "].getBundleId()=" + bundles[i].getBundleId());
	//			if (bundles[i].getBundleId() == 0) {// SystemBundle
	//				LauncherData launcherData = manipulator.getLauncherData();
	//				File fwJar = getFwJar(launcherData);
	//				String location = null;
	//				try {
	//					location = fwJar.toURL().toExternalForm();
	//				} catch (MalformedURLException e) {
	//					// TODO Auto-generated catch block
	//					e.printStackTrace();
	//				}
	//				bundlesList.add(new BundleInfo(location, startLevel.getBundleStartLevel(bundles[i]), startLevel.isBundlePersistentlyStarted(bundles[i]), bundles[i].getBundleId()));
	//				break;
	//			}
	//		}
	//		for (int i = 0; i < bundles.length; i++) {
	//			if (bundles[i].getBundleId() != 0) {// except SystemBundle
	//				//	System.out.println("Bundle["+"] is marked as started or not:" + flag);
	//				bundlesList.add(new BundleInfo(FileUtils.getRealLocation(bundles[i].getLocation()), startLevel.getBundleStartLevel(bundles[i]), startLevel.isBundlePersistentlyStarted(bundles[i]), bundles[i].getBundleId()));
	//			}
	//		}
	//		setStateObjectFactory();
	//		state = soFactory.createState(true);
	//		this.platfromProperties = this.getCurrentPlatformProperties();
	//		state.setPlatformProperties(platfromProperties);
	//
	//		BundleInfo[] bInfos = Utils.getBundleInfosFromList(bundlesList);
	//		for (int j = 0; j < bInfos.length; j++) {
	//			if (DEBUG)
	//				Log.log(LogService.LOG_DEBUG, this, "composeExpectedState()", "bInfos[" + j + "]=" + bInfos[j]);
	//			try {
	//				this.installBundle(bInfos[j]);
	//				//System.out.println("install bInfos[" + j + "]=" + bInfos[j]);
	//			} catch (RuntimeException e) {
	//				Log.log(LogService.LOG_ERROR, this, "composeExpectedState()", "BundleInfo:" + bInfos[j], e);
	//				e.printStackTrace();
	//				throw e;
	//			}
	//		}
	//		resolve(true);
	//	}

	public void installBundle(BundleInfo bInfo) throws FrameworkAdminRuntimeException {
		SimpleBundlesState.checkAvailability(fwAdmin);
		boolean found = false;

		BundleDescription[] currentInstalledBundles = state.getBundles();
		String newLocation = FileUtils.getRealLocation(bInfo.getLocation());
		Dictionary manifest = Utils.getOSGiManifest(newLocation);
		String newSymbolicName = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
		String newVersion = (String) manifest.get(Constants.BUNDLE_VERSION);
		for (int i = 0; i < currentInstalledBundles.length; i++) {
			String location = FileUtils.getRealLocation(currentInstalledBundles[i].getLocation());
			if (newLocation.equals(location)) {
				found = true;
				break;
			}
			String symbolicName = currentInstalledBundles[i].getSymbolicName();
			String version = currentInstalledBundles[i].getVersion().toString();
			if (newSymbolicName.equals(symbolicName) && newVersion.equals(version)) {
				found = true;
				break;
			}
		}
		if (!found) {
			BundleDescription newBundleDescription = null;
			try {
				long bundleId = bInfo.getBundleId();
				if (bundleId == BundleInfo.NO_BUNDLEID)
					newBundleDescription = soFactory.createBundleDescription(state, Utils.getOSGiManifest(newLocation), newLocation, ++maxId);
				else {
					if (bundleId > maxId)
						maxId = bundleId;
					newBundleDescription = soFactory.createBundleDescription(state, Utils.getOSGiManifest(newLocation), newLocation, bundleId);
				}
				state.addBundle(newBundleDescription);
				manipulator.getConfigData().addBundle(bInfo);
			} catch (BundleException e) {
				Log.log(LogService.LOG_WARNING, this, "installBundle(BundleInfo)", e);
			}
		}
	}

	public boolean isFullySupported() {
		return true;
	}

	public boolean isResolved() {
		return state.isResolved();
	}

	public boolean isResolved(BundleInfo bInfo) {
		BundleDescription description = state.getBundleByLocation(bInfo.getLocation());
		if (description == null)
			return false;
		return description.isResolved();
	}

	public void resolve(boolean increment) {
		state.resolve(increment);
	}

	/**
	 * get platforme properties from the given state.
	 * 
	 * @param state
	 */
	private void setPlatformProperties(State state) {
		Dictionary platformProperties = state.getPlatformProperties()[0];
		platfromProperties.clear();
		if (platformProperties != null) {
			for (Enumeration enumeration = platformProperties.keys(); enumeration.hasMoreElements();) {
				String key = (String) enumeration.nextElement();
				Object value = platformProperties.get(key);
				platfromProperties.setProperty(key, (String) value);
			}
		}
		if (DEBUG)
			Utils.printoutProperties(System.out, "PlatformProperties[0]", platfromProperties);
	}

	/**
	 * set platfromProperties required to compose state object
	 * into platformProperties of this state.
	 * 
	 * @param props
	 */
	private void setPlatformPropertiesToState(Dictionary props) {
		Properties platformProperties = setDefaultPlatformProperties();

		for (Enumeration enumeration = props.keys(); enumeration.hasMoreElements();) {
			String key = (String) enumeration.nextElement();
			for (int i = 0; i < PROPS.length; i++) {
				if (key.equals(PROPS[i])) {
					platformProperties.put(key, props.get(key));
					break;
				}
			}
		}
		state.setPlatformProperties(platformProperties);
	}

	private void setStateObjectFactory() {
		if (soFactory != null)
			return;
		BundleHelper helper = BundleHelper.getDefault();//getBundleHelper();
		PlatformAdmin platformAdmin = (PlatformAdmin) helper.acquireService(PlatformAdmin.class.getName());
		//	PlatformAdmin platformAdmin = (PlatformAdmin) heBundleHelper.getDefault().acquireService(PlatformAdmin.class.getName());
		soFactory = platformAdmin.getFactory();
	}

	//	public BundleHelper getBundleHelper() {
	//		BundleHelper helper = BundleHelper.getDefault();
	//		if (helper == null) {
	//			helper = new BundleHelper();
	//			try {
	//				helper.start(context);
	//			} catch (Exception e) {
	//				Log.log(LogService.LOG_WARNING, this, "setStateObjectFactory()", e);
	//			}
	//		}
	//		return helper;
	//	}

	public String toString() {
		if (state == null)
			return null;
		StringBuffer sb = new StringBuffer();
		BundleDescription[] bundleDescriptions = state.getBundles();
		for (int i = 0; i < bundleDescriptions.length; i++) {
			sb.append(bundleDescriptions[i].getBundleId() + ":");
			sb.append(bundleDescriptions[i].toString() + "(");
			sb.append(bundleDescriptions[i].isResolved() + ")");
			String[] ees = bundleDescriptions[i].getExecutionEnvironments();
			for (int j = 0; j < ees.length; j++)
				sb.append(ees[j] + " ");
			sb.append("\n");
		}
		sb.append("PlatformProperties:\n");
		Dictionary[] dics = state.getPlatformProperties();
		for (int i = 0; i < dics.length; i++) {
			for (Enumeration enumeration = dics[i].keys(); enumeration.hasMoreElements();) {
				String key = (String) enumeration.nextElement();
				String value = (String) dics[i].get(key);
				sb.append(" (" + key + "," + value + ")\n");
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	public void uninstallBundle(BundleInfo bInfo) throws FrameworkAdminRuntimeException {
		SimpleBundlesState.checkAvailability(fwAdmin);
		long id = -1;
		String targetLocation = bInfo.getLocation();
		BundleDescription[] currentInstalledBundles = state.getBundles();
		for (int i = 0; i < currentInstalledBundles.length; i++) {
			String location = currentInstalledBundles[i].getLocation();
			// TODO Is handling "reference:" needed ?
			//if(location.startsWith("reference:"))
			//	location = location.substring("reference:".length());
			if (targetLocation.equals(location)) {
				id = currentInstalledBundles[i].getBundleId();
				break;
			}
		}
		if (id != -1) {

			try {
				BundleDescription bundleDescription = soFactory.createBundleDescription(state, Utils.getOSGiManifest(bInfo.getLocation()), bInfo.getLocation(), id);
				state.removeBundle(bundleDescription);
				manipulator.getConfigData().removeBundle(bInfo);
			} catch (BundleException e) {
				Log.log(LogService.LOG_WARNING, this, "uninstallBundle(BundleInfo)", e);
				//throw new ManipulatorException("Fail to createBundleDescription of bInfo:" + bInfo.toString(), e, ManipulatorException.OTHERS);
			}
		}
	}

}
