package com.mediaworx.intellij.opencmsplugin.actions.tools;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

import java.util.ArrayList;
import java.util.Collection;

public class ActionTools {

	public static VirtualFile[] getOpenTabsFileArray() {
		VirtualFile[] publishFiles = null;
		Editor[] editors = EditorFactory.getInstance().getAllEditors();
		if (editors.length > 0) {
			ArrayList<VirtualFile> openFiles = new ArrayList<VirtualFile>(editors.length);
			FileDocumentManager fileDocManager = FileDocumentManager.getInstance();
			for (Editor editor : editors) {
				VirtualFile vf = fileDocManager.getFile(editor.getDocument());
				if (vf != null) {
					openFiles.add(vf);
				}
			}
			publishFiles = openFiles.toArray(new VirtualFile[openFiles.size()]);
		}
		return publishFiles;
	}

	public static VirtualFile[] getAllModulesFileArray(OpenCmsPlugin plugin) {
		VirtualFile[] publishFiles;Collection<OpenCmsModule> ocmsModules = plugin.getOpenCmsModules().getAllModules();
		publishFiles = new VirtualFile[ocmsModules.size()];
		int i = 0;
		for (OpenCmsModule ocmsModule : ocmsModules) {
			publishFiles[i++] = LocalFileSystem.getInstance().findFileByPath(ocmsModule.getIntelliJModuleRoot());
		}
		return publishFiles;
	}

	public static void setSelectionSpecificActionText(AnActionEvent event, OpenCmsPlugin plugin, String textPrefix) {
		boolean enableAction = false;
		String actionPlace = event.getPlace();

		VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
		FileTypeCounter fileTypeCounter = new FileTypeCounter(plugin);

		if (selectedFiles != null && selectedFiles.length > 0) {
			fileTypeCounter.count(selectedFiles);
			if (fileTypeCounter.hasEntities()) {
				enableAction = true;
			}
		}

		if (!actionPlace.equals(ActionPlaces.EDITOR_POPUP) && !actionPlace.equals(ActionPlaces.EDITOR_TAB_POPUP)) {
			String actionText = textPrefix + " selected " + fileTypeCounter.getEntityNames();
			event.getPresentation().setText(actionText);
		}

		if (enableAction) {
			event.getPresentation().setEnabled(true);
		}
		else {
			event.getPresentation().setEnabled(false);
		}
	}

	public static void setOnlyModulesSelectedPresentation(AnActionEvent event, String textPrefix) {
		boolean enableAction = true;

		Project project = event.getProject();
		if (project == null) {
			return;
		}

		OpenCmsPlugin plugin = project.getComponent(OpenCmsPlugin.class);
		VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);

		if (selectedFiles != null) {
			// check if only module roots have been selected
			for (VirtualFile ideaVFile : selectedFiles) {
				OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForIdeaVFile(ideaVFile);
				if (ocmsModule == null || !ocmsModule.isIdeaVFileModuleRoot(ideaVFile)) {
					enableAction = false;
					break;
				}
			}
		}
		else {
			enableAction = false;
		}

		if (enableAction) {
			FileTypeCounter fileTypeCounter = new FileTypeCounter(plugin);
			fileTypeCounter.count(selectedFiles);
			event.getPresentation().setText(textPrefix + " selected " + fileTypeCounter.getEntityNames());
			event.getPresentation().setEnabled(true);
		}
		else {
			event.getPresentation().setEnabled(false);
		}
	}

}
