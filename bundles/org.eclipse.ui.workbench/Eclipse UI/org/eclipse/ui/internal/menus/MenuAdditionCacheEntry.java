/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Remy Chi Jian Suen <remy.suen@gmail.com> - Bug 221662 [Contributions] Extension point org.eclipse.ui.menus: sub menu contribution does not have icon even if specified
 *******************************************************************************/

package org.eclipse.ui.internal.menus;

import java.util.ArrayList;
import java.util.Map;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.internal.workbench.ContributionsAnalyzer;
import org.eclipse.e4.ui.internal.workbench.URIHelper;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MParameter;
import org.eclipse.e4.ui.model.application.commands.impl.CommandsFactoryImpl;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledToolItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuContribution;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MRenderedMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarContribution;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarElement;
import org.eclipse.e4.ui.model.application.ui.menu.MToolControl;
import org.eclipse.e4.ui.model.application.ui.menu.MTrimContribution;
import org.eclipse.e4.ui.model.application.ui.menu.impl.MenuFactoryImpl;
import org.eclipse.e4.ui.workbench.renderers.swt.MenuManagerRenderer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.commands.ICommandImageService;
import org.eclipse.ui.internal.registry.IWorkbenchRegistryConstants;
import org.eclipse.ui.internal.services.ServiceLocator;

public class MenuAdditionCacheEntry {
	private final static String MAIN_TOOLBAR = "org.eclipse.ui.main.toolbar"; //$NON-NLS-1$

	private final static String TRIM_COMMAND1 = "org.eclipse.ui.trim.command1"; //$NON-NLS-1$

	private final static String TRIM_COMMAND2 = "org.eclipse.ui.trim.command2"; //$NON-NLS-1$

	private final static String TRIM_VERTICAL1 = "org.eclipse.ui.trim.vertical1"; //$NON-NLS-1$

	private final static String TRIM_VERTICAL2 = "org.eclipse.ui.trim.vertical2"; //$NON-NLS-1$

	private final static String TRIM_STATUS = "org.eclipse.ui.trim.status"; //$NON-NLS-1$

	private MApplication application;
	// private IEclipseContext appContext;
	private IConfigurationElement configElement;
	private MenuLocationURI location;

	// private String namespaceIdentifier;

	public MenuAdditionCacheEntry(MApplication application, IEclipseContext appContext,
			IConfigurationElement configElement, String attribute, String namespaceIdentifier) {
		this.application = application;
		// this.appContext = appContext;
		assert appContext.equals(this.application.getContext());
		this.configElement = configElement;
		this.location = new MenuLocationURI(attribute);
		// this.namespaceIdentifier = namespaceIdentifier;
	}

	private boolean inToolbar() {
		return location.getScheme().startsWith("toolbar"); //$NON-NLS-1$
	}

	/**
	 * @return <code>true</code> if this is a toolbar contribution
	 */
	public void mergeIntoModel(ArrayList<MMenuContribution> menuContributions,
			ArrayList<MToolBarContribution> toolBarContributions,
			ArrayList<MTrimContribution> trimContributions) {
		if ("menu:help?after=additions".equals(location.toString())) { //$NON-NLS-1$
			IConfigurationElement[] menus = configElement
					.getChildren(IWorkbenchRegistryConstants.TAG_MENU);
			if (menus.length == 1
					&& "org.eclipse.update.ui.updateMenu".equals(MenuHelper.getId(menus[0]))) { //$NON-NLS-1$
				return;
			}
		}
		if (inToolbar()) {
			String path = location.getPath();
			if (path.equals(MAIN_TOOLBAR) || path.equals(TRIM_COMMAND1)
					|| path.equals(TRIM_COMMAND2) || path.equals(TRIM_VERTICAL1)
					|| path.equals(TRIM_VERTICAL2) || path.equals(TRIM_STATUS)) {
				processTrimChildren(trimContributions, toolBarContributions, configElement);
			} else {
				String query = location.getQuery();
				if (query == null || query.length() == 0) {
					query = "after=additions"; //$NON-NLS-1$
				}
				processToolbarChildren(toolBarContributions, configElement, path,
						query);
			}
			return;
		}
		MMenuContribution menuContribution = MenuFactoryImpl.eINSTANCE.createMenuContribution();
		String idContrib = MenuHelper.getId(configElement);
		if (idContrib != null && idContrib.length() > 0) {
			menuContribution.setElementId(idContrib);
		}
		setContributorURI(menuContribution, configElement);
		if ("org.eclipse.ui.popup.any".equals(location.getPath())) { //$NON-NLS-1$
			menuContribution.setParentId("popup"); //$NON-NLS-1$
		} else {
			menuContribution.setParentId(location.getPath());
		}
		String query = location.getQuery();
		if (query == null || query.length() == 0) {
			query = "after=additions"; //$NON-NLS-1$
		}
		menuContribution.setPositionInParent(query);
		menuContribution.getTags().add("scheme:" + location.getScheme()); //$NON-NLS-1$
		String filter = ContributionsAnalyzer.MC_MENU;
		if ("popup".equals(location.getScheme())) { //$NON-NLS-1$
			filter = ContributionsAnalyzer.MC_POPUP;
		}
		menuContribution.getTags().add(filter);
		menuContribution.setVisibleWhen(MenuHelper.getVisibleWhen(configElement));
		addMenuChildren(menuContribution, configElement, filter);
		menuContributions.add(menuContribution);
		processMenuChildren(menuContributions, configElement, filter);
	}

	private void setContributorURI(MApplicationElement element, IConfigurationElement ce) {
		element.setContributorURI(URIHelper.constructPlatformURI(configElement.getContributor()));
	}

	/**
	 * @param menuContributions
	 * @param filter
	 */
	private void processMenuChildren(ArrayList<MMenuContribution> menuContributions,
			IConfigurationElement element, String filter) {
		IConfigurationElement[] menus = element.getChildren(IWorkbenchRegistryConstants.TAG_MENU);
		if (menus.length == 0) {
			return;
		}
		for (IConfigurationElement menu : menus) {
			MMenuContribution menuContribution = MenuFactoryImpl.eINSTANCE.createMenuContribution();
			String idContrib = MenuHelper.getId(menu);
			if (idContrib != null && idContrib.length() > 0) {
				menuContribution.setElementId(idContrib);
			}
			setContributorURI(menuContribution, configElement);
			menuContribution.setParentId(idContrib);
			menuContribution.setPositionInParent("after=additions"); //$NON-NLS-1$
			menuContribution.getTags().add("scheme:" + location.getScheme()); //$NON-NLS-1$
			menuContribution.getTags().add(filter);
			menuContribution.setVisibleWhen(MenuHelper.getVisibleWhen(menu));
			addMenuChildren(menuContribution, menu, filter);
			menuContributions.add(menuContribution);
			processMenuChildren(menuContributions, menu, filter);
		}
	}

	private void addMenuChildren(final MElementContainer<MMenuElement> container,
			IConfigurationElement parent, String filter) {
		IConfigurationElement[] items = parent.getChildren();
		for (int i = 0; i < items.length; i++) {
			final IConfigurationElement child = items[i];
			String itemType = child.getName();
			String id = MenuHelper.getId(child);

			if (IWorkbenchRegistryConstants.TAG_COMMAND.equals(itemType)) {
				MMenuElement element = createMenuCommandAddition(child);
				container.getChildren().add(element);
			} else if (IWorkbenchRegistryConstants.TAG_SEPARATOR.equals(itemType)) {
				MMenuElement element = createMenuSeparatorAddition(child);
				container.getChildren().add(element);
			} else if (IWorkbenchRegistryConstants.TAG_MENU.equals(itemType)) {
				MMenu element = createMenuAddition(child, filter);
				container.getChildren().add(element);
			} else if (IWorkbenchRegistryConstants.TAG_TOOLBAR.equals(itemType)) {
				System.out.println("Toolbar: " + id + " in " + location); //$NON-NLS-1$//$NON-NLS-2$
			} else if (IWorkbenchRegistryConstants.TAG_DYNAMIC.equals(itemType)) {
				ContextFunction generator = new ContextFunction() {
					@Override
					public Object compute(IEclipseContext context) {
						ServiceLocator sl = new ServiceLocator();
						sl.setContext(context);
						DynamicMenuContributionItem item = new DynamicMenuContributionItem(
								MenuHelper.getId(child), sl, child);
						return item;
					}
				};

				MRenderedMenuItem menuItem = MenuFactoryImpl.eINSTANCE.createRenderedMenuItem();
				menuItem.setElementId(id);
				setContributorURI(menuItem, child);
				menuItem.setContributionItem(generator);
				container.getChildren().add(menuItem);
			}
		}
	}

	/**
	 * @param iConfigurationElement
	 * @return
	 */
	private MMenuElement createMenuCommandAddition(IConfigurationElement commandAddition) {
		MHandledMenuItem item = MenuFactoryImpl.eINSTANCE.createHandledMenuItem();
		item.setElementId(MenuHelper.getId(commandAddition));
		setContributorURI(item, commandAddition);
		String commandId = MenuHelper.getCommandId(commandAddition);
		MCommand commandById = ContributionsAnalyzer.getCommandById(application, commandId);
		if (commandById == null) {
			commandById = CommandsFactoryImpl.eINSTANCE.createCommand();
			commandById.setElementId(commandId);
			commandById.setCommandName(commandId);
			application.getCommands().add(commandById);
		}
		item.setCommand(commandById);
		Map parms = MenuHelper.getParameters(commandAddition);
		for (Object obj : parms.entrySet()) {
			Map.Entry e = (Map.Entry) obj;
			MParameter parm = CommandsFactoryImpl.eINSTANCE.createParameter();
			parm.setName(e.getKey().toString());
			parm.setValue(e.getValue().toString());
			item.getParameters().add(parm);
		}
		String iconUrl = MenuHelper.getIconUrl(commandAddition,
				IWorkbenchRegistryConstants.ATT_ICON);

		if (iconUrl == null) {
			ICommandImageService commandImageService = application.getContext().get(
					ICommandImageService.class);
			ImageDescriptor descriptor = commandImageService == null ? null : commandImageService
					.getImageDescriptor(item.getElementId());
			if (descriptor != null) {
				item.setIconURI(MenuHelper.getImageUrl(descriptor));
			}
		} else {
			item.setIconURI(iconUrl);
		}
		item.setLabel(MenuHelper.getLabel(commandAddition));
		item.setMnemonics(MenuHelper.getMnemonic(commandAddition));
		item.setTooltip(MenuHelper.getTooltip(commandAddition));
		item.setType(MenuHelper.getStyle(commandAddition));
		item.setVisibleWhen(MenuHelper.getVisibleWhen(commandAddition));
		return item;
	}

	private MMenuElement createMenuSeparatorAddition(final IConfigurationElement sepAddition) {
		String name = MenuHelper.getName(sepAddition);
		MMenuElement element = MenuFactoryImpl.eINSTANCE.createMenuSeparator();
		element.setElementId(name);
		setContributorURI(element, sepAddition);
		if (!MenuHelper.isSeparatorVisible(sepAddition)) {
			element.setVisible(false);
			element.getTags().add(MenuManagerRenderer.GROUP_MARKER);
		}
		return element;
	}

	private MMenu createMenuAddition(final IConfigurationElement menuAddition, String filter) {
		// Is this for a menu or a ToolBar ? We can't create
		// a menu directly under a Toolbar; we have to add an
		// item of style 'pulldown'
		if (inToolbar()) {
			return null;
		}

		MMenu menu = MenuHelper.createMenuAddition(menuAddition);
		menu.getTags().add(filter);
		// addMenuChildren(menu, menuAddition, filter);
		return menu;
	}

	private void processTrimChildren(ArrayList<MTrimContribution> trimContributions,
			ArrayList<MToolBarContribution> toolBarContributions, IConfigurationElement element) {
		IConfigurationElement[] toolbars = element
				.getChildren(IWorkbenchRegistryConstants.TAG_TOOLBAR);
		if (toolbars.length == 0) {
			return;
		}
		MTrimContribution trimContribution = MenuFactoryImpl.eINSTANCE.createTrimContribution();
		String idContrib = MenuHelper.getId(configElement);
		if (idContrib != null && idContrib.length() > 0) {
			trimContribution.setElementId(idContrib);
		}
		setContributorURI(trimContribution, element);
		trimContribution.setParentId(location.getPath());
		String query = location.getQuery();
		if (query == null || query.length() == 0) {
			query = "after=additions"; //$NON-NLS-1$
		}
		trimContribution.setPositionInParent(query);
		trimContribution.getTags().add("scheme:" + location.getScheme()); //$NON-NLS-1$
		for (IConfigurationElement toolbar : toolbars) {
			MToolBar item = MenuFactoryImpl.eINSTANCE.createToolBar();
			item.setElementId(MenuHelper.getId(toolbar));
			setContributorURI(item, element);
			processToolbarChildren(toolBarContributions, toolbar, item.getElementId(),
					"after=additions"); //$NON-NLS-1$
			trimContribution.getChildren().add(item);
		}
		trimContributions.add(trimContribution);
	}

	private void processToolbarChildren(ArrayList<MToolBarContribution> contributions,
			IConfigurationElement toolbar, String parentId, String position) {
		MToolBarContribution toolBarContribution = MenuFactoryImpl.eINSTANCE
				.createToolBarContribution();
		String idContrib = MenuHelper.getId(toolbar);
		if (idContrib != null && idContrib.length() > 0) {
			toolBarContribution.setElementId(idContrib);
		}
		toolBarContribution.setParentId(parentId);
		toolBarContribution.setPositionInParent(position);
		toolBarContribution.getTags().add("scheme:" + location.getScheme()); //$NON-NLS-1$

		IConfigurationElement[] items = toolbar.getChildren();
		for (int i = 0; i < items.length; i++) {
			String itemType = items[i].getName();

			if (IWorkbenchRegistryConstants.TAG_COMMAND.equals(itemType)) {
				MToolBarElement element = createToolBarCommandAddition(items[i]);
				toolBarContribution.getChildren().add(element);

			} else if (IWorkbenchRegistryConstants.TAG_SEPARATOR.equals(itemType)) {
				MToolBarElement element = createToolBarSeparatorAddition(items[i]);
				toolBarContribution.getChildren().add(element);
			} else if (IWorkbenchRegistryConstants.TAG_CONTROL.equals(itemType)) {
				MToolBarElement element = createToolControlAddition(items[i]);
				toolBarContribution.getChildren().add(element);
			}
		}

		contributions.add(toolBarContribution);
	}

	private MToolBarElement createToolControlAddition(IConfigurationElement element) {
		String id = MenuHelper.getId(element);
		MToolControl control = MenuFactoryImpl.eINSTANCE.createToolControl();
		control.setElementId(id);
		setContributorURI(control, element);
		control.setContributionURI(CompatibilityWorkbenchWindowControlContribution.CONTROL_CONTRIBUTION_URI);
		ControlContributionRegistry.add(id, element);
		return control;
	}

	private MToolBarElement createToolBarSeparatorAddition(final IConfigurationElement sepAddition) {
		String name = MenuHelper.getName(sepAddition);
		MToolBarElement element = MenuFactoryImpl.eINSTANCE.createToolBarSeparator();
		element.setElementId(name);
		setContributorURI(element, sepAddition);
		if (!MenuHelper.isSeparatorVisible(sepAddition)) {
			element.setToBeRendered(false);
		}
		return element;
	}

	private MToolBarElement createToolBarCommandAddition(final IConfigurationElement commandAddition) {
		MHandledToolItem item = MenuFactoryImpl.eINSTANCE.createHandledToolItem();
		item.setElementId(MenuHelper.getId(commandAddition));
		setContributorURI(item, commandAddition);
		String commandId = MenuHelper.getCommandId(commandAddition);
		MCommand commandById = ContributionsAnalyzer.getCommandById(application, commandId);
		if (commandById == null) {
			commandById = CommandsFactoryImpl.eINSTANCE.createCommand();
			commandById.setElementId(commandId);
			commandById.setCommandName(commandId);
			application.getCommands().add(commandById);
		}
		item.setCommand(commandById);
		Map parms = MenuHelper.getParameters(commandAddition);
		for (Object obj : parms.entrySet()) {
			Map.Entry e = (Map.Entry) obj;
			MParameter parm = CommandsFactoryImpl.eINSTANCE.createParameter();
			parm.setName(e.getKey().toString());
			parm.setValue(e.getValue().toString());
			item.getParameters().add(parm);
		}
		String iconUrl = MenuHelper.getIconUrl(commandAddition,
				IWorkbenchRegistryConstants.ATT_ICON);

		if (iconUrl == null) {
			ICommandImageService commandImageService = application.getContext().get(
					ICommandImageService.class);
			ImageDescriptor descriptor = commandImageService == null ? null : commandImageService
					.getImageDescriptor(commandId);
			if (descriptor == null) {
				descriptor = commandImageService == null ? null : commandImageService
						.getImageDescriptor(item.getElementId());
				if (descriptor == null) {
					item.setLabel(MenuHelper.getLabel(commandAddition));
				} else {
					item.setIconURI(MenuHelper.getImageUrl(descriptor));
				}
			} else {
				item.setIconURI(MenuHelper.getImageUrl(descriptor));
			}
		} else {
			item.setIconURI(iconUrl);
		}
		item.setTooltip(MenuHelper.getTooltip(commandAddition));
		item.setType(MenuHelper.getStyle(commandAddition));
		item.setVisibleWhen(MenuHelper.getVisibleWhen(commandAddition));
		return item;
	}
}
