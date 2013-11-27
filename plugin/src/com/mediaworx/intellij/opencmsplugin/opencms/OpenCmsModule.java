package com.mediaworx.intellij.opencmsplugin.opencms;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.ModuleExportPoint;
import com.mediaworx.intellij.opencmsplugin.configuration.module.OpenCmsModuleConfigurationData;
import com.mediaworx.intellij.opencmsplugin.entities.SyncMode;
import com.mediaworx.intellij.opencmsplugin.tools.ModuleTools;

import java.util.List;

public class OpenCmsModule {

	private static final Logger LOG = Logger.getInstance(OpenCmsModule.class);

	OpenCmsPlugin plugin;
	Module intelliJModule;
	OpenCmsModuleConfigurationData moduleConfig;

	List<ModuleExportPoint> exportPoints;
	List<String> moduleResources;
	String intelliJModuleRoot;
	String localVfsRoot;
	SyncMode syncMode;

	public OpenCmsModule(OpenCmsPlugin plugin, Module intelliJModule) {
		this.plugin = plugin;
		this.intelliJModule = intelliJModule;
	}

	public void init(OpenCmsModuleConfigurationData moduleConfig) {
		this.moduleConfig = moduleConfig;
		exportPoints = plugin.getOpenCmsConfiguration().getExportPointsForModule(moduleConfig.getModuleName());
		moduleResources = plugin.getOpenCmsConfiguration().getModuleResourcesForModule(moduleConfig.getModuleName());

		String relativeVfsRoot;

		if (moduleConfig.isUseProjectDefaultVfsRootEnabled()) {
			relativeVfsRoot = plugin.getPluginConfiguration().getDefaultLocalVfsRoot();
		}
		else {
			relativeVfsRoot = moduleConfig.getLocalVfsRoot();
		}

		String intelliJModuleRoot = ModuleTools.getModuleContentRoot(intelliJModule);
		if (intelliJModuleRoot == null || intelliJModuleRoot.length() == 0) {
			this.intelliJModuleRoot = null;
			localVfsRoot = null;
		}
		else {
			this.intelliJModuleRoot = intelliJModuleRoot;
			StringBuilder vfsRootBuilder = new StringBuilder();
			vfsRootBuilder.append(this.intelliJModuleRoot).append("/").append(relativeVfsRoot);
			localVfsRoot = vfsRootBuilder.toString();
		}

		if (moduleConfig.isUseProjectDefaultSyncModeEnabled()) {
			syncMode = plugin.getPluginConfiguration().getDefaultSyncMode();
		}
		else {
			syncMode = moduleConfig.getSyncMode();
		}
	}

	public void refresh(OpenCmsModuleConfigurationData moduleConfig) {
		// for now a refresh just does the same as init
		init(moduleConfig);
	}

	public void refresh() {
		// for now a refresh just does the same as init
		init(moduleConfig);
	}

	public String getModuleName() {
		return moduleConfig.getModuleName();
	}

	public String getIntelliJModuleRoot() {
		return intelliJModuleRoot;
	}

	public String getLocalVfsRoot() {
		return localVfsRoot;
	}

	public String getManifestRoot() {
		return intelliJModuleRoot + "/" + plugin.getPluginConfiguration().getManifestRoot();
	}

	public SyncMode getSyncMode() {
		return syncMode;
	}

	public List<ModuleExportPoint> getExportPoints() {
		return exportPoints;
	}

	public List<String> getModuleResources() {
		return moduleResources;
	}

	public boolean isIdeaVFileModuleResource(VirtualFile ideaVFile) {
		for (String moduleResource : getModuleResources()) {
			String resourcePath = getLocalVfsRoot() + moduleResource;
			String filePath = ideaVFile.getPath().replace('\\', '/');
			if ((filePath + "/").endsWith(resourcePath)) {
				filePath = filePath + "/";
			}
			LOG.info("resourcePath: " + resourcePath);
			LOG.info("filePath:     " + filePath);
			if (filePath.startsWith(resourcePath)) {
				return true;
			}
		}
		return false;
	}

	public boolean isIdeaVFileModuleRoot(VirtualFile ideaVFile) {
		if (!ideaVFile.isDirectory()) {
			return false;
		}
		String filePath = ideaVFile.getPath();
		return filePath.equals(intelliJModuleRoot);
	}

	public boolean isIdeaVFileInVFSPath(VirtualFile ideaVFile) {
		String filePath = ideaVFile.getPath();
		return filePath.startsWith(localVfsRoot);
	}


	public String getVfsPathForIdeaVFile(final VirtualFile ideaVFile) {
		String filepath = ideaVFile.getPath();
		String relativeName = filepath.substring(localVfsRoot.length());
		if (relativeName.length() == 0) {
			relativeName = "/";
		}
		return relativeName;
	}
}
