/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jan-Hendrik Diederich, Bredex GmbH - bug 201052
 *******************************************************************************/
package org.eclipse.ui.internal.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.e4.ui.internal.workbench.URIHelper;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.descriptor.basic.MPartDescriptor;
import org.eclipse.e4.ui.model.application.descriptor.basic.impl.BasicFactoryImpl;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityPart;
import org.eclipse.ui.views.IStickyViewDescriptor;
import org.eclipse.ui.views.IViewCategory;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;

public class ViewRegistry implements IViewRegistry {

	@Inject
	private MApplication application;

	@Inject
	private IExtensionRegistry extensionRegistry;

	private Map<String, IViewDescriptor> descriptors = new HashMap<String, IViewDescriptor>();

	private List<IStickyViewDescriptor> stickyDescriptors = new ArrayList<IStickyViewDescriptor>();

	private List<ViewCategory> categories = new ArrayList<ViewCategory>();

	@PostConstruct
	void postConstruct() {
		IExtensionPoint point = extensionRegistry.getExtensionPoint("org.eclipse.ui.views"); //$NON-NLS-1$
		for (IExtension extension : point.getExtensions()) {
			// find the category first
			for (IConfigurationElement element : extension.getConfigurationElements()) {
				if (element.getName().equals(IWorkbenchRegistryConstants.TAG_CATEGORY)) {
					ViewCategory category = new ViewCategory(
							element.getAttribute(IWorkbenchRegistryConstants.ATT_ID),
							element.getAttribute(IWorkbenchRegistryConstants.ATT_NAME));
					categories.add(category);
				} else if (element.getName().equals(IWorkbenchRegistryConstants.TAG_STICKYVIEW)) {
					try {
						stickyDescriptors.add(new StickyViewDescriptor(element));
					} catch (CoreException e) {
						// log an error since its not safe to open a dialog here
						WorkbenchPlugin.log(
								"Unable to create sticky view descriptor.", e.getStatus());//$NON-NLS-1$
					}
				}
			}
		}

		for (IExtension extension : point.getExtensions()) {
			for (IConfigurationElement element : extension.getConfigurationElements()) {
				if (element.getName().equals(IWorkbenchRegistryConstants.TAG_VIEW)) {
					MPartDescriptor descriptor = BasicFactoryImpl.eINSTANCE.createPartDescriptor();
					descriptor.setLabel(element.getAttribute(IWorkbenchRegistryConstants.ATT_NAME));
					descriptor.setContributorURI(URIHelper.constructPlatformURI(element
							.getContributor()));
					String id = element.getAttribute(IWorkbenchRegistryConstants.ATT_ID);
					descriptor.setElementId(id);
					if (id.equals(IPageLayout.ID_RES_NAV)
							|| id.equals(IPageLayout.ID_PROJECT_EXPLORER)) {
						descriptor.setCategory("org.eclipse.e4.primaryNavigationStack"); //$NON-NLS-1$
					} else if (id.equals(IPageLayout.ID_OUTLINE)) {
						descriptor.setCategory("org.eclipse.e4.secondaryNavigationStack"); //$NON-NLS-1$
					} else {
						descriptor.setCategory("org.eclipse.e4.secondaryDataStack"); //$NON-NLS-1$
					}

					List<String> tags = descriptor.getTags();
					tags.add("View"); //$NON-NLS-1$

					descriptor.setCloseable(true);
					descriptor.setAllowMultiple(Boolean.parseBoolean(element
							.getAttribute(IWorkbenchRegistryConstants.ATT_ALLOW_MULTIPLE)));
					descriptor.setContributionURI(CompatibilityPart.COMPATIBILITY_VIEW_URI);

					String iconURI = element.getAttribute(IWorkbenchRegistryConstants.ATT_ICON);
					if (iconURI != null && !iconURI.startsWith("platform:/plugin/")) { //$NON-NLS-1$
						StringBuilder builder = new StringBuilder("platform:/plugin/"); //$NON-NLS-1$
						builder.append(element.getNamespaceIdentifier()).append('/');

						// FIXME: need to get rid of $nl$ properly
						if (iconURI.startsWith("$nl$")) { //$NON-NLS-1$
							iconURI = iconURI.substring(4);
						}

						builder.append(iconURI);
						descriptor.setIconURI(builder.toString());
					} else {
						descriptor.setIconURI(iconURI);
					}

					ViewDescriptor viewDescriptor = new ViewDescriptor(application, descriptor,
							element);

					application.getDescriptors().add(descriptor);
					descriptors.put(descriptor.getElementId(), viewDescriptor);

					String categoryId = element
							.getAttribute(IWorkbenchRegistryConstants.ATT_CATEGORY);
					ViewCategory category = findCategory(categoryId);

					if (category == null) {
						tags.add("categoryTag:" + WorkbenchMessages.ICategory_other); //$NON-NLS-1$	
					} else {
						tags.add("categoryTag:" + category.getLabel()); //$NON-NLS-1$
						category.addDescriptor(viewDescriptor);
					}
				}
			}
		}
	}

	public IViewDescriptor find(String id) {
		IViewDescriptor candidate = descriptors.get(id);
		if (WorkbenchActivityHelper.restrictUseOf(candidate)) {
			return null;
		}
		return candidate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.views.IViewRegistry#getCategories()
	 */
	public IViewCategory[] getCategories() {
		return categories.toArray(new IViewCategory[categories.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.views.IViewRegistry#getViews()
	 */
	public IViewDescriptor[] getViews() {
		Collection<?> allowedViews = WorkbenchActivityHelper.restrictCollection(
				descriptors.values(), new ArrayList<Object>());
		return allowedViews.toArray(new IViewDescriptor[allowedViews.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.views.IViewRegistry#getStickyViews()
	 */
	public IStickyViewDescriptor[] getStickyViews() {
		Collection<?> allowedViews = WorkbenchActivityHelper.restrictCollection(stickyDescriptors,
				new ArrayList<Object>());
		return allowedViews.toArray(new IStickyViewDescriptor[allowedViews.size()]);
	}

	/**
	 * 
	 */
	public void dispose() {

	}

	public ViewCategory findCategory(String id) {
		if (id == null) {
			return null;
		}
		for (ViewCategory category : categories) {
			if (id.equals(category.getId())) {
				return category;
			}
		}
		return null;
	}

	public Category getMiscCategory() {
		// TODO Auto-generated method stub
		return null;
	}

}
