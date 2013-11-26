package com.mediaworx.intellij.opencmsplugin.sync;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.entities.*;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsConnectionException;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPermissionDeniedException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;

import java.io.File;
import java.util.*;

class SyncFileAnalyzer implements Runnable {

	private static final Logger LOG = Logger.getInstance(SyncFileAnalyzer.class);

	private OpenCmsPlugin plugin;
	private SyncJob syncJob;
	private final VirtualFile[] syncFiles;
	private final StringBuilder message;
	List<OpenCmsModuleResource> moduleResourcesToBePulled;

	private VfsAdapter vfsAdapter;
	ProgressIndicator progressIndicator;

	private boolean executeSync = true;

	SyncFileAnalyzer(final OpenCmsPlugin plugin, final SyncJob syncJob, final VirtualFile[] syncFiles, final StringBuilder message) throws CmsConnectionException {
		this.plugin = plugin;
		this.syncJob = syncJob;
		this.syncFiles = syncFiles;
		this.message = message;

		vfsAdapter = plugin.getVfsAdapter();

		// Not connected yet (maybe OpenCms wasn't started when the project opened)
		if (!vfsAdapter.isConnected()) {
			// Try again to connect
			vfsAdapter.startSession();

			// Still not connected? Show an error message and stop
			if (!vfsAdapter.isConnected()) {
				// Get the hell out of here
				throw new CmsConnectionException("Connection to OpenCms VFS failed. Is OpenCms running?");
			}
		}
		this.vfsAdapter.clearCache();

		moduleResourcesToBePulled = new ArrayList<OpenCmsModuleResource>();
	}

	@Override
	public void run() {
		progressIndicator = ProgressManager.getInstance().getProgressIndicator();
		progressIndicator.setIndeterminate(true);
		progressIndicator.setText("Calculating resources to sync, please wait");

		if (syncFiles != null && syncFiles.length > 0) {
			for (VirtualFile syncFile : syncFiles) {
				if (progressIndicator.isCanceled()) {
					executeSync = false;
					return;
				}

				// file/folder is ignored
				if (OpenCmsSyncer.fileOrPathIsIgnored(syncFile)) {
					// do nothing (filter VCS files and OpenCms Sync Metadata)
					LOG.info("file/folder is ignored");
					continue;
				}

				OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForIdeaVFile(syncFile);
				if (ocmsModule == null) {
					LOG.info("file/folder is not within a configured OpenCms module, ignore");
					continue;
				}

				// it is a folder that is a module root, so sync all corresponding module resources
				if (syncFile.isDirectory() && ocmsModule.isIdeaVFileModuleRoot(syncFile)) {
					handleModule(ocmsModule);
				}

				// file/folder is within a module resource path, handle it
				else if (ocmsModule.isIdeaVFileModuleResource(syncFile)) {
					LOG.info("Handling a module resource path, a folder or a file in a module");
					walkFileTree(ocmsModule, syncFile, FolderSyncMode.AUTO);
				}

				// if it is a folder that is not a resource path, but within the VFS path ...
				else if (syncFile.isDirectory()  && ocmsModule.isIdeaVFileInVFSPath(syncFile)) {
					String relativeFolderPath = syncFile.getPath().substring(ocmsModule.getLocalVfsRoot().length());
					// ... get all module resources under the folder and add them
					for (String moduleResource : ocmsModule.getModuleResources()) {
						// if the module resource is within the selected folder ...
						if (moduleResource.startsWith(relativeFolderPath)) {
							// ... handle it
							handleModuleResourcePath(ocmsModule, moduleResource);
						}
					}
				}

				// file/folder is neither a module resource nor a VFS resource parent, ignore
				else {
					message.append("Ignoring '").append(syncFile.getPath()).append("' (not a module path).\n");
					LOG.info("File is not in the sync path, ignore");
				}
			}
		}
		if (moduleResourcesToBePulled != null && moduleResourcesToBePulled.size() > 0) {
			handleModuleResourcesToBePulled(moduleResourcesToBePulled, message, progressIndicator);
		}
		if (progressIndicator.isCanceled()) {
			executeSync = false;
		}
	}


	boolean isExecuteSync() {
		return executeSync;
	}

	private void handleModule(OpenCmsModule ocmsModule) {
		for (String resourcePath : ocmsModule.getModuleResources()) {
			handleModuleResourcePath(ocmsModule, resourcePath);
		}
		syncJob.setSyncModuleMetaInformation(true);
		syncJob.addOcmsModule(ocmsModule);
	}

	private void handleModuleResourcePath(OpenCmsModule ocmsModule, String resourcePath) {
		LOG.info("resource path: " + ocmsModule.getLocalVfsRoot() + resourcePath);
		VirtualFile resourceFile = LocalFileSystem.getInstance().findFileByPath(ocmsModule.getLocalVfsRoot() + resourcePath);
		if (resourceFile != null) {
			LOG.info("vFolder path: " + resourceFile.getPath());
			walkFileTree(ocmsModule, resourceFile, FolderSyncMode.AUTO);
		}
		else {
			LOG.info("Resource path doesn't exist in the FS, it has to be pulled from the VFS");
			moduleResourcesToBePulled.add(new OpenCmsModuleResource(ocmsModule, resourcePath));
		}
	}

	// TODO: handle cases where a folder on the vfs has the same name as a file on the rfs or vice versa
	private void walkFileTree(OpenCmsModule ocmsModule, VirtualFile ideaVFile, FolderSyncMode folderSyncMode) {

		if (progressIndicator.isCanceled()) {
			executeSync = false;
			return;
		}
		if (ocmsModule == null) {
			LOG.info("Skipping " + ideaVFile.getPath() + ", no module defined");
			return;
		}

		if (OpenCmsSyncer.fileOrPathIsIgnored(ideaVFile)) {
			LOG.info("File is ignored: " + ideaVFile.getPath() + " " + ideaVFile.getName());
			return;
		}

		String vfsPath = ocmsModule.getVfsPathForIdeaVFile(ideaVFile);

		LOG.info("VFS path is " + vfsPath);

		boolean vfsObjectExists;
		CmisObject vfsObject = null;

		if (folderSyncMode != FolderSyncMode.PUSH) {
			// Get the corresponding vfs object (if it exists)
			try {
				vfsObject = vfsAdapter.getVfsObject(vfsPath);
			}
			catch (CmsPermissionDeniedException e) {
				message.append("Skipping ").append(vfsPath).append(", permission denied\n");
				return;
			}

			vfsObjectExists = vfsObject != null;
			LOG.info(vfsPath + (vfsObjectExists ? " exists" : " does not exist"));
		}
		else {
			vfsObjectExists = false;
		}

		// It's a folder, check if it is already there and compare contents
		if (ideaVFile.isDirectory()) {
			// The folder is not there, so push it with all child contents
			if (!vfsObjectExists) {
				LOG.info("It's a folder that does not exist on the VFS, PUSH recursively");
				addRfsOnlyFolderTreeToSyncJob(ocmsModule, vfsPath, ideaVFile, false);
			}
			// The Folder is there, compare contents of VFS and RFS
			else {
				handleExistingFolder(ocmsModule, ideaVFile, (Folder)vfsObject);
			}
		}
		// It's a file
		else if (!ideaVFile.isSpecialFile()) {
			// The file is not there, so push it
			if (!vfsObjectExists) {
				LOG.info("It's a file that does not exist on the VFS, PUSH");
				addRfsOnlyFileToSyncJob(ocmsModule, vfsPath, ideaVFile, null);
			}
			// The file exists, check which one is newer
			else {
				handleExistingFile(ocmsModule, ideaVFile, vfsObject, vfsPath);
			}
		}

	}

	private void handleExistingFolder(OpenCmsModule ocmsModule, VirtualFile ideaVFile, Folder vfsObject) {
		LOG.info("It's a folder that does exist on the VFS, compare");

		// Get folder content from the vfs, put it in a set
		LOG.info("Getting VFS content");
		LOG.info("Children:");
		ItemIterable<CmisObject> vfsChildren = vfsObject.getChildren();
		Map<String, CmisObject> vfsChildMap = new LinkedHashMap<String, CmisObject>();
		for (CmisObject vfsChild : vfsChildren) {
			vfsChildMap.put(vfsChild.getName(), vfsChild);
			LOG.info("    " + vfsChild.getName());
		}

		LOG.info("Looping RFS children");
		VirtualFile[] rfsChildren = ideaVFile.getChildren();

		// handle resources in the RFS
		for (VirtualFile rfsChild : rfsChildren) {
			if (progressIndicator.isCanceled()) {
				return;
			}
			String filename = rfsChild.getName();

			// The file/folder does not exist on the VFS, recurse in PUSH mode (all children will be pushed)
			if (!vfsChildMap.containsKey(filename)) {
				LOG.info("RFS child " + rfsChild.getName() + " is not on the VFS, handle it in PUSH mode");
				walkFileTree(ocmsModule, rfsChild, FolderSyncMode.PUSH);
			}
			// The file/folder does exist on the VFS, recurse in AUTO mode (children will be pushed or pulled depending on their date)
			else {
				LOG.info("RFS child " + rfsChild.getName() + " exists on the VFS, handle it in AUTO mode");
				walkFileTree(ocmsModule, rfsChild, FolderSyncMode.AUTO);

				// remove the file from the vfsChildren map, so that only files that exist only on the vfs will be left
				vfsChildMap.remove(filename);
			}
		}

		// Handle files/folders that exist only on the vfs
		handleVfsOnlyChildren(ocmsModule, vfsObject.getPath(), vfsChildMap);
	}


	private void handleVfsOnlyChildren(OpenCmsModule ocmsModule, String parentVfsPath, Map<String, CmisObject> vfsChildMap) {
		LOG.info("Handle files/folders that exist only on the vfs");
		for (CmisObject vfsChild : vfsChildMap.values()) {
			if (progressIndicator.isCanceled()) {
				return;
			}
			String childVfsPath = parentVfsPath + "/" + vfsChild.getName();

			// files
			if (vfsChild.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
				addVfsOnlyFileToSyncJob(ocmsModule, childVfsPath, vfsChild, false);
			}
			// folders
			else if (vfsChild.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
				addVfsOnlyFolderTreeToSyncJob(ocmsModule, childVfsPath, vfsChild, false);
			}
		}
	}

	private void handleExistingFile(OpenCmsModule ocmsModule, VirtualFile ideaVFile, CmisObject vfsObject, String vfsPath) {
		LOG.info("It's a file that exists on the VFS and the RFS");
		File realFile = new File(ideaVFile.getPath());
		Date localDate = new Date(realFile.lastModified());
		Date vfsDate = vfsObject.getLastModificationDate().getTime();

		if (ocmsModule.getSyncMode() == SyncMode.SYNC) {
			LOG.info("SyncMode is SYNC, so compare dates");
			if (localDate.after(vfsDate)) {
				LOG.info("RFS file is newer, PUSH");
				SyncFile syncFile = new SyncFile(ocmsModule, vfsPath, ideaVFile, vfsObject, SyncAction.PUSH, true);
				syncJob.addSyncEntity(syncFile);
			}
			else if (vfsDate.after(localDate)) {
				LOG.info("VFS file is newer, PULL");
				SyncFile syncFile = new SyncFile(ocmsModule, vfsPath, ideaVFile, vfsObject, SyncAction.PULL, true);
				syncJob.addSyncEntity(syncFile);
			}
			else {
				LOG.info("VFS file and RFS file have the same date, ignore");
			}
		}
		else if (ocmsModule.getSyncMode() == SyncMode.PUSH && vfsDate.compareTo(localDate) != 0) {
			LOG.info("SyncMode is PUSH and files are not equal, so force push");
			SyncFile syncFile = new SyncFile(ocmsModule, vfsPath, ideaVFile, vfsObject, SyncAction.PUSH, true);
			syncJob.addSyncEntity(syncFile);
		}
		else if (ocmsModule.getSyncMode() == SyncMode.PULL && vfsDate.compareTo(localDate) != 0) {
			LOG.info("SyncMode is PULL and files are not equal, so force pull");
			SyncFile syncFile = new SyncFile(ocmsModule, vfsPath, ideaVFile, vfsObject, SyncAction.PULL, true);
			syncJob.addSyncEntity(syncFile);
		}
	}

	private void handleModuleResourcesToBePulled(List<OpenCmsModuleResource> moduleResourcesToBePulled, StringBuilder message, ProgressIndicator progressIndicator) {
		for (OpenCmsModuleResource moduleResourceToBePulled : moduleResourcesToBePulled) {
			if (progressIndicator.isCanceled()) {
				executeSync = false;
				return;
			}

			String vfsPath = moduleResourceToBePulled.getResourcePath();

			CmisObject vfsObject;
			try {
				vfsObject = vfsAdapter.getVfsObject(vfsPath);
			}
			catch (CmsPermissionDeniedException e) {
				message.append("Skipping ").append(vfsPath).append(", permission denied\n");
				continue;
			}
			if (vfsObject == null)  {
				message.append("Skipping ").append(vfsPath).append(", doesn't exist in the VFS\n");
				continue;
			}

			OpenCmsModule ocmsModule = moduleResourceToBePulled.getOpenCmsModule();

			// files
			if (vfsObject.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
				addModuleResourceFileToSyncJob(ocmsModule, vfsPath, vfsObject);
			}
			// folders
			else if (vfsObject.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
				addModuleResourceFolderTreeToSyncJob(ocmsModule, vfsPath, vfsObject);
			}
		}
	}

	private void addRfsOnlyFileToSyncJob(OpenCmsModule ocmsModule, String vfsPath, VirtualFile file, Document vfsFile) {
		LOG.info("Adding RFS only file " + vfsPath);
		SyncAction syncAction = getRfsOnlySyncAction(ocmsModule.getSyncMode());
		SyncFile syncFile = new SyncFile(ocmsModule, vfsPath, file, vfsFile, syncAction, vfsFile != null);
		syncJob.addSyncEntity(syncFile);
	}

	private void addRfsOnlyFolderTreeToSyncJob(OpenCmsModule ocmsModule, String vfsPath, VirtualFile file, boolean replaceExistingEntity) {
		LOG.info("Adding RFS only folder " + vfsPath);

		SyncAction syncAction = getRfsOnlySyncAction(ocmsModule.getSyncMode());
		SyncFolder syncFolder = new SyncFolder(ocmsModule, vfsPath, file, null, syncAction, replaceExistingEntity);
		syncJob.addSyncEntity(syncFolder);

		if (syncAction != SyncAction.DELETE_RFS) {
			LOG.info("Get children of folder " + vfsPath);
			VirtualFile[] children = file.getChildren();
			for (VirtualFile child : children) {
				LOG.info("Handle PUSH child " + child.getPath());
				walkFileTree(ocmsModule, child, FolderSyncMode.PUSH);
			}
		}
	}

	private void addVfsOnlyFileToSyncJob(OpenCmsModule ocmsModule, String vfsPath, CmisObject vfsObject, boolean replaceExistingEntity) {
		LOG.info("Adding VFS only file " + vfsPath);
		SyncAction syncAction = getVfsOnlySyncAction(ocmsModule.getSyncMode());
		SyncFile syncFile = new SyncFile(ocmsModule, vfsPath, null, vfsObject, syncAction, replaceExistingEntity);
		syncJob.addSyncEntity(syncFile);
	}

	private void addVfsOnlyFolderTreeToSyncJob(OpenCmsModule ocmsModule, String vfsPath, CmisObject vfsObject, boolean replaceExistingEntity) {
		LOG.info("Adding VFS only folder " + vfsPath);

		SyncAction syncAction = getVfsOnlySyncAction(ocmsModule.getSyncMode());
		SyncFolder syncFolder = new SyncFolder(ocmsModule, vfsPath, null, vfsObject, syncAction, replaceExistingEntity);
		syncJob.addSyncEntity(syncFolder);

		if (syncAction != SyncAction.DELETE_VFS) {
			// traverse folder, add children to the SyncJob
			LOG.info("Get children of VFS folder " + vfsPath);
			ItemIterable<CmisObject> vfsChildren = ((Folder) vfsObject).getChildren();
			for (CmisObject child : vfsChildren) {
				String childVfsPath = vfsPath + "/" + child.getName();

				// files
				if (child.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
					addVfsOnlyFileToSyncJob(ocmsModule, childVfsPath, child, false);
				}
				// folders
				else if (child.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
					addVfsOnlyFolderTreeToSyncJob(ocmsModule, childVfsPath, child, false);
				}
			}
		}
	}

	private void addModuleResourceFileToSyncJob(OpenCmsModule ocmsModule, String vfsPath, CmisObject vfsObject) {
		LOG.info("Adding VFS module resource file " + vfsPath);
		SyncFile syncFile = new SyncFile(ocmsModule, vfsPath, null, vfsObject, SyncAction.PULL, false);
		syncJob.addSyncEntity(syncFile);
	}

	private void addModuleResourceFolderTreeToSyncJob(OpenCmsModule ocmsModule, String vfsPath, CmisObject vfsObject) {
		LOG.info("Adding VFS module resource folder " + vfsPath);

		SyncFolder syncFolder = new SyncFolder(ocmsModule, vfsPath, null, vfsObject, SyncAction.PULL, false);
		syncJob.addSyncEntity(syncFolder);

		// traverse folder, add children to the SyncJob
		LOG.info("Get children of VFS folder " + vfsPath);
		ItemIterable<CmisObject> vfsChildren = ((Folder) vfsObject).getChildren();
		for (CmisObject child : vfsChildren) {
			String childVfsPath = vfsPath + "/" + child.getName();

			// files
			if (child.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
				addModuleResourceFileToSyncJob(ocmsModule, childVfsPath, child);
			}
			// folders
			else if (child.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
				addModuleResourceFolderTreeToSyncJob(ocmsModule, childVfsPath, child);
			}
		}
	}

	private SyncAction getRfsOnlySyncAction(SyncMode syncMode) {
		SyncAction syncAction;
		switch (syncMode) {
			case PULL:
				syncAction = SyncAction.DELETE_RFS;
				break;
			case PUSH:
			case SYNC:
			default:
				syncAction = SyncAction.PUSH;
				break;
		}
		return syncAction;
	}

	private SyncAction getVfsOnlySyncAction(SyncMode syncMode) {
		SyncAction syncAction;
		switch (syncMode) {
			case PUSH:
				syncAction = SyncAction.DELETE_VFS;
				break;
			case PULL:
			case SYNC:
			default:
				syncAction = SyncAction.PULL;
				break;
		}
		return syncAction;
	}
}