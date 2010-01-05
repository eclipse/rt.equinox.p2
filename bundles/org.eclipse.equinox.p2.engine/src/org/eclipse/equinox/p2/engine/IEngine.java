/*******************************************************************************
 * Copyright (c) 2008, 2009 Band XI International, LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Band XI - initial API and implementation
 *   IBM - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * The engine is a service that naively performs a set of requested changes to a provisioned
 * system. No attempt is made to determine whether the requested changes or the
 * resulting system are valid or consistent. It is assumed that the engine client has
 * crafted a valid provisioning plan for the engine to perform, typically by using a planner
 * service.
 * <p>
 * The engine operates by executing a series of installation phases. The client can
 * customize the set of phases that are executed, or else the engine will execute
 * a default set of phases. During each phase the changes described by the provisioning
 * plan are performed against the profile being provisioned.
 * 
 * @since 2.0
 */
public interface IEngine {
	/**
	 * Service name constant for the engine service.
	 */
	public static final String SERVICE_NAME = IEngine.class.getName();

	/**
	 * Creates a customized provisioning plan describing a set of changes that have already been validated.
	 * This is an advanced method for clients that know they are creating changes that do
	 * not require validation by a planner. Most clients should instead obtain a validated plan
	 * from a planner.
	 * 
	 * @param profile The profile to operate against
	 * @param operands The operands to perform
	 * @param context The provisioning context for the plan
	 * @return A provisioning plan
	 */
	public IProvisioningPlan createCustomPlan(IProfile profile, Operand[] operands, ProvisioningContext context);

	/**
	 * Creates a phase set with a default set of phases, excluding the specified phases.
	 * @param excludes The phases to exclude, or <code>null</code> to include
	 * all default phases.
	 * 
	 * @return A new phase set
	 */
	public IPhaseSet createPhaseSetExcluding(String[] excludes);

	/**
	 * Creates and returns a phase set including only the specified phases.
	 * @param includes The phases to include
	 * @return A new phase set
	 */
	public IPhaseSet createPhaseSetIncluding(String[] includes);

	/**
	 * Executes a provisioning plan.
	 * 
	 * @param plan The plan describing the changes to be made
	 * @param phaseSet The phases to run, or <code>null</code> to run default phases
	 * @param monitor A progress monitor, or <code>null</code> if progress reporting is not required
	 * @return The result of executing the plan
	 */
	public IStatus perform(IProvisioningPlan plan, IPhaseSet phaseSet, IProgressMonitor monitor);

	/**
	 * Executes a provisioning plan with a default phase set and context.
	 * 
	 * @param plan The plan describing the changes to be made
	 * @param monitor A progress monitor, or <code>null</code> if progress reporting is not required
	 * @return The result of executing the plan
	 */
	public IStatus perform(IProvisioningPlan plan, IProgressMonitor monitor);
}