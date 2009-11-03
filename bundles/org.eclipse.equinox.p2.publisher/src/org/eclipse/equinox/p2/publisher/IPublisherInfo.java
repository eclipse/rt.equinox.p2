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
package org.eclipse.equinox.p2.publisher;

import java.util.Collection;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;

public interface IPublisherInfo {

	/**
	 * A bitwise flag to say whether or not the index for the artifact repository should
	 * be updated.
	 */
	public static final int A_INDEX = 1;

	/**
	 * A bitwise flag to say whether or the artifacts themselves should be published.
	 */
	public static final int A_PUBLISH = 2;

	/**
	 * A bitwise flag to say whether or not to overwrite disk content discovered
	 * in the repository when publishing an artifact
	 */
	public static final int A_OVERWRITE = 4;

	/**
	 * Returns the artifact repository into which any publishable artifacts are published
	 * or <code>null</code> if none.
	 * @return a destination artifact repository or <code>null</code>
	 */
	public IArtifactRepository getArtifactRepository();

	/**
	 * Returns the metadata repository into which any publishable metadata is published
	 * or <code>null</code> if none.
	 * @return a destination metadata repository or <code>null</code>
	 */
	public IMetadataRepository getMetadataRepository();

	/**
	 * Returns whether or not artifacts themselves should be published.
	 * @return <code>true</code> if artifacts should be published.  
	 * <code>false</code> otherwise.
	 */
	public int getArtifactOptions();

	/**
	 * Returns the registered advice for the given configuration of WS, OS and ARCH where the
	 * advice is of the given type.  If mergeDefault is <code>true</code> then the advice for
	 * the default configuration is merged into the result.
	 * @param configSpec the configuration to query.  Note that the given configuration
	 * must specify values for WS, OS and ARCH.
	 * @param type the type of advice to look for
	 * @param includeDefault whether or not to merge in the advice common to all configurations
	 * @return the set of advice of the given type for the given configuration
	 */
	public Collection getAdvice(String configSpec, boolean includeDefault, String id, Version version, Class type);

	/**
	 * Add the given advice to the set of publishing advices.  
	 * @param advice the advice to retain
	 */
	public void addAdvice(IPublisherAdvice advice);

	/**
	 * Returns the configurations being published. Each configuration is a string of the form
	 * ws.os.arch describing the window system, operating system, and architecture of each
	 * configuration.
	 * @return The configurations being published
	 * @see AbstractPublisherAction#createConfigSpec(String, String, String)
	 */
	public String[] getConfigurations();

	/**
	 * Returns the artifact repository given to the publisher as context for 
	 * the publisher actions.  May be <code>null</code>.  Note that multiple 
	 * repositories may be represented as one composite repository.
	 * 
	 * @return the context artifact repository or <code>null</code> if none.
	 */
	public IArtifactRepository getContextArtifactRepository();

	/**
	 * Returns the metadata repository given to the publisher as context for 
	 * the publisher actions.  May be <code>null</code>.  Note that multiple 
	 * repositories may be represented as one composite repository.
	 * 
	 * @return the context metadata repository or <code>null</code> if none.
	 */
	public IMetadataRepository getContextMetadataRepository();
}
