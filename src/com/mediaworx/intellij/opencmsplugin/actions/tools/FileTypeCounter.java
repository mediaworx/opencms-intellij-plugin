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

package com.mediaworx.intellij.opencmsplugin.actions.tools;

import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

import java.util.ArrayList;

public class FileTypeCounter {

	private OpenCmsPlugin plugin;
	private int numModules;
	private int numFolders;
	private int numFiles;

	public FileTypeCounter(OpenCmsPlugin plugin) {
		this.plugin = plugin;
		numModules = 0;
		numFolders = 0;
		numFiles = 0;
	}

	public void count(VirtualFile[] selectedFiles) {
		// calculate the number of selected modules, folders and files
		for (VirtualFile ideaVFile : selectedFiles) {

			OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForIdeaVFile(ideaVFile);

			if (ocmsModule == null) {
				continue;
			}

			if (ocmsModule.isIdeaVFileModuleRoot(ideaVFile)) {
				numModules += 1;
			}
			else if (ocmsModule.isIdeaVFileInVFSPath(ideaVFile)) {
				if (ideaVFile.isDirectory()) {
					numFolders += 1;
				}
				else {
					numFiles += 1;
				}
			}
			// if we know that there are multiple modules, multiple folders and multiple files, then there's no reason to go on
			if (numModules > 1 && numFolders > 1 && numFiles > 1) {
				break;
			}
		}
	}

	public boolean hasEntities() {
		return numModules + numFolders + numFiles > 0;
	}

	public String getEntityNames() {
		StringBuilder entityNames = new StringBuilder();
		ArrayList<String> textElements = new ArrayList<String>(3);
		if (numModules + numFolders + numFiles > 0) {
			if (numModules > 0) {
				textElements.add(numModules > 1 ? "Modules" : "Module");
			}
			if (numFolders > 0) {
				textElements.add(numFolders > 1 ? "Folders" : "Folder");
			}
			if (numFiles > 0) {
				textElements.add(numFiles > 1 ? "Files" : "File");
			}

			for (int i = 0; i < textElements.size(); i++) {
				if (i > 0) {
					entityNames.append("/");
				}
				entityNames.append(textElements.get(i));
			}
		}
		else {
			entityNames.append("Modules/Folders/Files");
		}
		return entityNames.toString();
	}
}
