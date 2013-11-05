package com.mediaworx.intellij.opencmsplugin.tools;

import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: kai
 * Date: 25.01.13
 * Time: 22:36
 * To change this template use File | Settings | File Templates.
 */
public class PathTools {

    public static final String VFS_SYSTEMFOLDER = File.separator+"system";
    public static final String VFS_MODULESFOLDER = File.separator+"modules";

    public static String getLocalModulesParentPath(String moduleName, OpenCmsPluginConfigurationData config) {
        return config.getLocalModuleVfsRoot(moduleName) + VFS_SYSTEMFOLDER + VFS_MODULESFOLDER;
    }

    public static String getVfsPathFromIdeaVFile(String moduleName, OpenCmsPluginConfigurationData config, VirtualFile file) {
        String filepath = file.getPath().replace('\\', '/');
        String syncRoot = config.getLocalModuleVfsRoot(moduleName).replace('\\', '/');
        String relativeName = filepath.substring(filepath.indexOf(syncRoot) + syncRoot.length(), filepath.length());
        if (relativeName.length() == 0) {
            relativeName = "/";
        }
        return relativeName;
    }

    public static boolean isFileInModulePath(final OpenCmsPluginConfigurationData config, final VirtualFile file) {
        String moduleName = getModuleName(config, file);
	    if (moduleName == null || moduleName.length() == 0) {
		    System.out.println("No module configured");
		    return false;
	    }
	    String modulesPath = (PathTools.getLocalModulesParentPath(moduleName, config) + File.separator).replace('\\', '/');
        System.out.println("moduleName: "+getModuleName(config, file));
        System.out.println(modulesPath);
        System.out.println(file.getPath().replace('\\', '/'));
        return file.getPath().replace('\\', '/').matches(Pattern.quote(modulesPath) + ".+");
    }


    public static String getModuleName(OpenCmsPluginConfigurationData config, final VirtualFile file) {
	    String moduleName = null;
	    HashMap<String,String> vfsRoots = config.getLocalModuleVfsRootMap();
	    if (vfsRoots != null) {
	        for (String tmpModuleName : config.getLocalModuleVfsRootMap().keySet()) {
	            if (file.getPath().replace('\\', '/').startsWith(config.getLocalModuleVfsRoot(tmpModuleName).replace('\\', '/'))) {
		            moduleName = tmpModuleName;
		            break;
	            }
	        }
	    }
        return moduleName;
    }

}