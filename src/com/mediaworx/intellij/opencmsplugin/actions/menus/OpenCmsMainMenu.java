/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2014 mediaworx berlin AG (http://www.mediaworx.com)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.mediaworx.intellij.opencmsplugin.actions.menus;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.actions.OpenCmsPluginAction;
import com.mediaworx.intellij.opencmsplugin.actions.generatemanifest.OpenCmsGenerateAllManifestsAction;
import com.mediaworx.intellij.opencmsplugin.actions.generatemanifest.OpenCmsGenerateSelectedModuleManifestsAction;
import com.mediaworx.intellij.opencmsplugin.actions.packagemodule.OpenCmsPackageAllModulesAction;
import com.mediaworx.intellij.opencmsplugin.actions.packagemodule.OpenCmsPackageSelectedModulesAction;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishAllModulesAction;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishModuleAction;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishOpenEditorTabsAction;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishSelectedAction;
import com.mediaworx.intellij.opencmsplugin.actions.pullmetadata.OpenCmsPullAllMetaDataAction;
import com.mediaworx.intellij.opencmsplugin.actions.pullmetadata.OpenCmsPullSelectedModuleMetaDataAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncAllModulesAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncModuleAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncOpenEditorTabsAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncSelectedAction;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Collection;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsMainMenu extends OpenCmsMenu {

	private static final Logger LOG = Logger.getInstance(OpenCmsMainMenu.class);

	private static final String SYNC_SELECTED_ID = "OpenCmsPlugin.SyncAction";
	private static final String SYNC_OPEN_TABS_ID = "OpenCmsPlugin.SyncOpenTabsAction";
	private static final String SYNC_ALL_MODULES_ID = "OpenCmsPlugin.SyncAllAction";
	public static final String SYNC_MODULE_ID_PREFIX = "OpenCmsPlugin.SyncModule.";
	private static final String PULL_MODULE_METADATA_ID = "OpenCmsPlugin.PullModuleMetaDataAction";
	private static final String PULL_ALL_METADATA_ID = "OpenCmsPlugin.PullAllMetaDataAction";
	private static final String GENERATE_SELECTED_MODULE_MANIFEST_ID = "OpenCmsPlugin.GenerateManifestAction";
	private static final String GENERATE_ALL_MANIFESTS_ID = "OpenCmsPlugin.GenerateAllManifestsAction";
	private static final String PACKAGE_SELECTED_MODULE_ID = "OpenCmsPlugin.PackageModuleAction";
	private static final String PACKAGE_ALL_MODULES_ID = "OpenCmsPlugin.PackageAllModulesAction";
	private static final String PUBLISH_SELECTED_ID = "OpenCmsPlugin.PublishAction";
	private static final String PUBLISH_OPEN_TABS_ID = "OpenCmsPlugin.PublishOpenTabsAction";
	private static final String PUBLSH_ALL_MODULES_ID = "OpenCmsPlugin.PublishAllModules";
	public static final String PUBLISH_MODULE_ID_PREFIX = "OpenCmsPlugin.PublishModule.";

	private static final Shortcut SYNC_SHORTCUT = new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK + KeyEvent.SHIFT_MASK), null);

	private Keymap keymap;
	private DefaultActionGroup syncModuleActions;
	private DefaultActionGroup publishModuleActions;

	private static OpenCmsMainMenu instance;

	Project currentProject;

	private OpenCmsMainMenu(OpenCmsPlugin plugin) {
		super(plugin, "All OpenCms actions", false);
		currentProject = plugin.getProject();
		registerModuleActions();
	}

	public static OpenCmsMainMenu getInstance(OpenCmsPlugin plugin) {
		if (instance == null) {
			instance = new OpenCmsMainMenu(plugin);
		}
		return instance;
	}

	private void registerKeyboardShortcuts() {
		if (keymap.getShortcuts(SYNC_SELECTED_ID).length == 0) {
			keymap.addShortcut(SYNC_SELECTED_ID, SYNC_SHORTCUT);
		}
	}

	@Override
	protected void registerActions() {
		keymap = KeymapManager.getInstance().getActiveKeymap();
		syncModuleActions = new DefaultActionGroup();
		syncModuleActions.getTemplatePresentation().setText("Sync &Module");
		syncModuleActions.setPopup(true);
		publishModuleActions = new DefaultActionGroup();
		publishModuleActions.getTemplatePresentation().setText("Publish Module");
		publishModuleActions.setPopup(true);

		registerKeyboardShortcuts();

		plugin.addAction(this, SYNC_SELECTED_ID, new OpenCmsSyncSelectedAction(), "_Sync selected Modules/Folders/Files");
		plugin.addAction(this, SYNC_OPEN_TABS_ID, new OpenCmsSyncOpenEditorTabsAction(), "Sync all open Editor _Tabs");
		plugin.addAction(this, SYNC_ALL_MODULES_ID, new OpenCmsSyncAllModulesAction(), "Sync _all Modules");
		add(syncModuleActions);

		add(Separator.getInstance());

		plugin.addAction(this, PULL_MODULE_METADATA_ID, new OpenCmsPullSelectedModuleMetaDataAction(), "_Pull Meta Data for selected Modules");
		plugin.addAction(this, PULL_ALL_METADATA_ID, new OpenCmsPullAllMetaDataAction(), "Pull all _Meta Data");

		add(Separator.getInstance());

		plugin.addAction(this, GENERATE_SELECTED_MODULE_MANIFEST_ID, new OpenCmsGenerateSelectedModuleManifestsAction(), "_Generate manifest.xml for selected Modules");
		plugin.addAction(this, GENERATE_ALL_MANIFESTS_ID, new OpenCmsGenerateAllManifestsAction(), "Generate manifest.xml for all Modules");

		add(Separator.getInstance());

		plugin.addAction(this, PACKAGE_SELECTED_MODULE_ID, new OpenCmsPackageSelectedModulesAction(), "Package Module _Zip for selected Modules");
		plugin.addAction(this, PACKAGE_ALL_MODULES_ID, new OpenCmsPackageAllModulesAction(), "Package Module Zip for all Modules");

		add(Separator.getInstance());

		plugin.addAction(this, PUBLISH_SELECTED_ID, new OpenCmsPublishSelectedAction(), "_Publish selected Modules/Folders/Files");
		plugin.addAction(this, PUBLISH_OPEN_TABS_ID, new OpenCmsPublishOpenEditorTabsAction(), "Publish all open Editor Tabs");
		plugin.addAction(this, PUBLSH_ALL_MODULES_ID, new OpenCmsPublishAllModulesAction(), "Publish all Modules");
		add(publishModuleActions);
	}

	/**
	 * refreshes the list of module actions on project change (if two instances of IntelliJ are running and a
	 * switch from one instance to the other occurs). If the active project doesn't use the IntelliJ plugin, the
	 * OpenCms menu in the main menu is explicitly disabled because the disableIfNoVisibleChildren mechanism doesn't
	 * work for the main menu (this seems to be a bug in IntelliJ, see
	 * {@link com.mediaworx.intellij.opencmsplugin.actions.menus.OpenCmsMenu#disableIfNoVisibleChildren()})
	 * @param event the event (provided by IntelliJ)
	 */
	@Override
	public void update(AnActionEvent event) {
		super.update(event);

		Presentation presentation = event.getPresentation();

		if (presentation.isEnabled() != isPluginEnabled()) {
			presentation.setEnabled(isPluginEnabled());
		}

		Project eventProject = event.getProject();

		if (eventProject == null) {
			return;
		}

		if (eventProject != currentProject) {

			if (isPluginEnabled()) {
				LOG.info("project switched, reinitializing module actions");
				currentProject = eventProject;
				registerModuleActions();
			}
			else {
				unregisterModuleActions();
			}
			plugin.setToolWindowAvailable(isPluginEnabled());
		}
	}

	/**
	 * the disableIfNoVisibleChildren mechanism doesn't work for the main menu (this seems to be a bug in IntelliJ),
	 * so it is explicitly disabled.
	 * @return always returns <code>false</code>
	 */
	@Override
	public boolean disableIfNoVisibleChildren() {
		return false;
	}

	public void unregisterModuleActions() {
		if (syncModuleActions.getChildrenCount() > 0) {
			unregisterCurrentSyncModuleActions();
		}
		if (publishModuleActions.getChildrenCount() > 0) {
			unregisterCurrentPublishModuleActions();
		}
	}

	public void registerModuleActions() {

		unregisterModuleActions();

		if (plugin.getPluginConfiguration() != null && plugin.getPluginConfiguration().isOpenCmsPluginEnabled()) {
			try {
				Collection<OpenCmsModule> ocmsModules = plugin.getOpenCmsModules().getAllModules();
				for (OpenCmsModule ocmsModule : ocmsModules) {
					registerSyncModuleAction(ocmsModule);
				}
				for (OpenCmsModule ocmsModule : ocmsModules) {
					registerPublishModuleAction(ocmsModule);
				}
			}
			catch (NullPointerException e) {
				LOG.warn("NullPointerException during OpenCms module registration in the main menu.", e);
				LOG.warn("plugin: " + plugin);
				LOG.warn("plugin.getOpenCmsModules(): " + plugin.getOpenCmsModules());
				LOG.warn("plugin.getOpenCmsModules().getAllModules(): " + plugin.getOpenCmsModules().getAllModules());
			}
		}
	}

	public void registerSyncModuleAction(OpenCmsModule ocmsModule) {
		registerModuleAction(ocmsModule, syncModuleActions, new OpenCmsSyncModuleAction(), SYNC_MODULE_ID_PREFIX);
	}

	public void registerPublishModuleAction(OpenCmsModule ocmsModule) {
		registerModuleAction(ocmsModule, publishModuleActions, new OpenCmsPublishModuleAction(), PUBLISH_MODULE_ID_PREFIX);
	}

	public void registerModuleAction(OpenCmsModule ocmsModule, DefaultActionGroup group, OpenCmsPluginAction action, String idPrefix) {
		int moduleNo = group.getChildrenCount() + 1;
		String actionId = idPrefix + ocmsModule.getIntelliJModuleRoot();
		String text = (moduleNo < 10 ? "_" : "") + moduleNo + " " + ocmsModule.getModuleName();
		plugin.addAction(group, actionId, action, text);
	}

	private void unregisterCurrentSyncModuleActions() {
		AnAction[] allActions = syncModuleActions.getChildActionsOrStubs();
		for (AnAction action : allActions) {
			String actionId = actionManager.getId(action);
			if (actionId != null) {
				keymap.removeAllActionShortcuts(actionId);
				actionManager.unregisterAction(actionId);
			}
		}
		syncModuleActions.removeAll();
	}

	private void unregisterCurrentPublishModuleActions() {
		AnAction[] allActions = publishModuleActions.getChildActionsOrStubs();
		for (AnAction action : allActions) {
			String actionId = actionManager.getId(action);
			keymap.removeAllActionShortcuts(actionId);
			actionManager.unregisterAction(actionId);
		}
		publishModuleActions.removeAll();
	}

}
