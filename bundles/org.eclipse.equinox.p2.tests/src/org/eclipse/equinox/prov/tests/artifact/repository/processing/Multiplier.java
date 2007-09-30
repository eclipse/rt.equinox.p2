/*******************************************************************************
* Copyright (c) 2007 compeople AG and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* 	compeople AG (Stefan Liebig) - initial API and implementation
*  IBM - continuing development
*******************************************************************************/
package org.eclipse.equinox.prov.tests.artifact.repository.processing;

import java.io.IOException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.prov.artifact.repository.Activator;
import org.eclipse.equinox.prov.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.prov.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.prov.artifact.repository.processing.ProcessingStepDescriptor;

public class Multiplier extends ProcessingStep {

	protected int operand;

	public Multiplier() {
		// needed
	}

	public Multiplier(int operand) {
		super();
		this.operand = operand;
	}

	public void initialize(ProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(descriptor, context);
		try {
			operand = Integer.valueOf(descriptor.getData()).intValue();
		} catch (NumberFormatException e) {
			int code = descriptor.isRequired() ? IStatus.ERROR : IStatus.INFO;
			status = new Status(code, Activator.ID, "Multiplier operand specification invalid", e);
			return;
		}
	}

	public void write(int b) throws IOException {
		destination.write(b == -1 ? b : b * operand);
	}

	public void close() throws IOException {
		super.close();
		status = Status.OK_STATUS;
	}
}
