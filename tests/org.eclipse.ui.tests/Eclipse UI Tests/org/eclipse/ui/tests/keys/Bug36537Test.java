/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.tests.keys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.commands.CommandManager;
import org.eclipse.ui.internal.commands.KeyBindingDefinition;
import org.eclipse.ui.keys.KeySequence;
import org.eclipse.ui.tests.util.UITestCase;

/**
 * Tests Bug 36537
 * 
 * @since 3.0
 */
public class Bug36537Test extends UITestCase {

	/**
	 * Constructor for Bug36537Test.
	 * 
	 * @param name
	 *           The name of the test
	 */
	public Bug36537Test(String name) {
		super(name);
	}

	/**
	 * Tests that there are no redundant key bindings defined in the
	 * application.
	 */
	public void testForRedundantKeyBindings() {
		IWorkbenchWindow window = openTestWindow();
		Workbench workbench = (Workbench) window.getWorkbench();
		CommandManager commandManager = (CommandManager) workbench.getCommandManager();

		List keyBindings = commandManager.getPluginCommandRegistry().getKeyBindingDefinitions();
		Iterator keyBindingItr = keyBindings.iterator();
		Map keyBindingsByKeySequence = new HashMap();

		while (keyBindingItr.hasNext()) {
			// Retrieve the key binding.
			KeyBindingDefinition keyBinding = (KeyBindingDefinition) keyBindingItr.next();

			// Find the point the bindings with matching key sequences.
			KeySequence keySequence = keyBinding.getKeySequence();
			List matches = (List) keyBindingsByKeySequence.get(keySequence);
			if (matches == null) {
				matches = new ArrayList();
				keyBindingsByKeySequence.put(keySequence, matches);
			}

			// Check that we don't have any redundancy or other wackiness.
			Iterator matchItr = matches.iterator();
			while (matchItr.hasNext()) {
				KeyBindingDefinition definition = (KeyBindingDefinition) matchItr.next();
				String commandA = keyBinding.getCommandId();
				String commandB = definition.getCommandId();
				String contextA = keyBinding.getContextId();
				String contextB = definition.getContextId();
				String keyConfA = keyBinding.getKeyConfigurationId();
				String keyConfB = definition.getKeyConfigurationId();
				String localeA = keyBinding.getLocale();
				String localeB = definition.getLocale();
				String platformA = keyBinding.getPlatform();
				String platformB = definition.getPlatform();

				boolean same = true;
				int nullMatches = 0;
				same &= (commandA == null) ? (commandB == null) : (commandA.equals(commandB));
				same &= (contextA == null) || (contextB == null) || (contextA.equals(contextB));
				if (((contextA == null) || (contextB == null)) && (contextA != contextB)) {
					nullMatches++;
				}
				same &= (keyConfA == null) || (keyConfB == null) || (keyConfA.equals(keyConfB));
				if (((keyConfA == null) || (keyConfB == null)) && (keyConfA != keyConfB)) {
					nullMatches++;
				}
				same &= (localeA == null) || (localeB == null) || (localeA.equals(localeB));
				if (((localeA == null) || (localeB == null)) && (localeA != localeB)) {
					nullMatches++;
				}
				same &= (platformA == null) || (platformB == null) || (platformA.equals(platformB));
				if (((platformA == null) || (platformB == null)) && (platformA != platformB)) {
					nullMatches++;
				}

				assertFalse("Redundant key bindings: " + keyBinding + ", " + definition, same && (nullMatches < 1)); //$NON-NLS-1$ //$NON-NLS-2$
			}

			// Add the key binding.
			matches.add(keyBinding);
		}
	}
}
