package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class OpenCmsSyncAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsSyncAction.class);



	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		try {
			VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
			OpenCmsSyncer fileSyncer = new OpenCmsSyncer(plugin);
			fileSyncer.syncFiles(selectedFiles);
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsSyncAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	@Override
	public void update(@NotNull AnActionEvent event) {

		super.update(event);

		VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);

		if (selectedFiles != null && selectedFiles.length > 0) {
			int numFiles = 0;
			int numFolders = 0;
			int numModules = 0;

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

			if (numModules + numFolders + numFiles > 0) {
				String actionText = getActionText(numFiles, numFolders, numModules);
				event.getPresentation().setText(actionText);
				event.getPresentation().setVisible(true);
			}
			else {
				event.getPresentation().setVisible(false);
			}
		}
		else {
			event.getPresentation().setVisible(false);
		}

	}

	private String getActionText(int numFiles, int numFolders, int numModules) {
		ArrayList<String> textElements = new ArrayList<String>(3);
		if (numModules > 0) {
			textElements.add(numModules > 1 ? "Modules" : "Module");
		}
		if (numFolders > 0) {
			textElements.add(numFolders > 1 ? "Folders" : "Folder");
		}
		if (numFiles > 0) {
			textElements.add(numFiles > 1 ? "Files" : "File");
		}

		StringBuilder actionText = new StringBuilder("_Sync selected ");
		for (int i = 0; i < textElements.size(); i++) {
			if (i > 0) {
				actionText.append("/");
			}
			actionText.append(textElements.get(i));
		}
		return actionText.toString();
	}

}
