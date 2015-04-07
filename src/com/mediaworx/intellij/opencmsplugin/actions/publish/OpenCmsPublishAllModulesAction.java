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

package com.mediaworx.intellij.opencmsplugin.actions.publish;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.mediaworx.intellij.opencmsplugin.actions.tools.ActionTools;

import java.io.File;
import java.util.List;

/**
 * Action to publish all OpenCms modules in the project.
 */
@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsPublishAllModulesAction extends OpenCmsPublishAction {

	/**
	 * @param event the action event, provided by IntelliJ
	 * @return An Array of Virtual Files representing all OpenCms modules
	 */
	@Override
	protected List<File> getPublishFileArray(AnActionEvent event) {
		return ActionTools.getAllModulesFileList(plugin);
	}
}
