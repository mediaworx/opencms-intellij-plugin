package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import com.mediaworx.intellij.opencmsplugin.sync.SyncJob;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsPullModuleMetaDataAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsPullModuleMetaDataAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		try {
			VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);

			cleanupMetaFolders(selectedFiles);

			OpenCmsSyncer ocmsSyncer = new OpenCmsSyncer(plugin);
			ocmsSyncer.setPullMetaDataOnly(true);
			ocmsSyncer.syncFiles(selectedFiles);
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsSyncAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	private void cleanupMetaFolders(VirtualFile[] moduleFiles) {
		for (VirtualFile moduleFile : moduleFiles) {
			OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForIdeaVFile(moduleFile);
			if (ocmsModule != null) {
				SyncJob.cleanupModuleMetaFolder(ocmsModule);
			}
		}
	}

	@Override
	public void update(@NotNull AnActionEvent event) {

		super.update(event);

		event.getPresentation().setText("_Pull Meta Data (select Modules to use this option)");

		OpenCmsPluginConfigurationData config = plugin.getPluginConfiguration();
		if (!config.isPluginConnectorEnabled() || !config.isPullMetadataEnabled()) {
			event.getPresentation().setEnabled(false);
			return;
		}

		VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);

		boolean enableAction = true;

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
			event.getPresentation().setText("_Pull Meta Data for selected " + fileTypeCounter.getEntityNames());
			event.getPresentation().setEnabled(true);
		}
		else {
			event.getPresentation().setEnabled(false);
		}
	}
}
