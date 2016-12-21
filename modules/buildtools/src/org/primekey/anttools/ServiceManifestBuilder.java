/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.primekey.anttools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is a tool for adding a manifest file for a given interface to the META-INF/services directory of a JAR. It
 * will automatically scan the jar file for all non-abstract classes implementing the interface or an extension of it 
 * and add them to the manifest file. 
 * 
 * All operations are basically stateless, so all methods are static.
 * 
 * Note that while java.nio.file.Path is really neat, for the time being Java SDK 6 compatibility is required. 
 * 
 * @version $Id: ServiceManifestBuilder.java 20305 2014-11-24 13:57:56Z mikekushner $
 *
 */
public class ServiceManifestBuilder {

    private static final String META_INF = "META-INF";
    private static final String SERVICES = "services";
    private static final String CLASS_EXTENSION = ".class";

    /**
     * Entry method for this class as a standalone tool. 
     * 
     * @param args
     */
    public static void main(String[] args) {
        final int errorCode = mainInternal(args);
        if (errorCode!=0) {
            System.exit(errorCode);
        }
    }

    static int mainInternal(String[] args) {
        if (args.length < 2 || args.length > 3) {
            final String TAB = "     ";
            StringBuffer out = new StringBuffer();
            out.append("DESCRIPTION:\n");
            out.append(TAB + "This command line tool inserts service manifest files into a given directory.\n");
            out.append(TAB + "It uses the following two arguments (without flags):\n");
            out.append(TAB + "(1) Path to a directory\n");
            out.append(TAB + "(2) A semicolon separated list of interfaces\n");
            out.append(TAB + "(3) (OPTIONAL) Temporary working directory, only applicable (but not required) when writing to jar. "
                    + "Will use system default if left blank.\n");
            out.append("\n");
            out.append("EXAMPLES:\n");
            out.append(TAB + "/usr/ejbca/modules/ejbca-ejb-cli/build/ com.foo.bar.InterfaceAlpha;com.bar.foo.InterfaceBeta /var/tmp/ \n");
            out.append("\n");
            out.append("WARNING: Adding a service manifest to a JAR with a file manifest is unstable at the moment.");
            System.err.println(out.toString());
            return 1;
        }
        File archive = new File(args[0]);
        if (!archive.exists()) {
            System.err.println(archive + " does not exist on the system.");
            return 1;
        } else if (archive.isFile() && !archive.getName().endsWith(".jar")) {
            System.err.println(archive + " does not appear to be a .jar file.");
            return 1;
        }
        //Make sure that the directory to be modified is on the classpath
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        try {
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] { archive.toURI().toURL() });
        } catch (Throwable t) {
            throw new RuntimeException("Exception caught while trying to modify classpath", t);
        }

        String[] classNames = args[1].split(";");
        Class<?>[] classes = new Class<?>[classNames.length];
        for (int i = 0; i < classNames.length; ++i) {
            try {
                classes[i] = Class.forName(classNames[i]);
            } catch (ClassNotFoundException e) {
                System.err.println("Class " + classNames[i] + " not found on classpath, cannot continue.");
                return 1;
            }
        }
        try {
            buildServiceManifestToLocation(archive, classes);
        } catch (IOException e) {
            System.err.println("Disk related error occured while building manifest, see following stacktrace");
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

    /**
     * Recursively deletes a file. If file is a directory, then it will delete all files and subdirectories contained.
     * 
     * @param file the file to delete
     */
    public static void delete(File file) {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                delete(subFile);
            }
        }
        if (!file.delete()) {
            System.err.println("Could not delete directory " + file.getAbsolutePath());
        }

    }

    public static File createTempDirectory() throws IOException {
        return createTempDirectory(null);
    }

    public static File createTempDirectory(File location) throws IOException {
        final File temp = File.createTempFile("tmp", Long.toString(System.nanoTime()), location);
        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        //Known race condition exists here, not sure what an attacker would accomplish with it though
        if (!temp.mkdir()) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return temp;
    }
   

    /**
     * Method that constructs a file to any location. Can be a jarfile, directly to the classpath, you name it. 
     * 
     * @param location a writable location where a META-INF/services directory will be created (if not existing) and a manifest file placed,
     *                 and where suitable class files will be searched for.
     * @param interfaceClass the interface to base the manifest on
     * @throws IOException for any file related errors
     */
    public static void buildServiceManifestToLocation(File location, Class<?>... interfaceClasses) throws IOException {
        if (!location.isDirectory()) {
            throw new IOException("File " + location + " was not a directory.");
        }
        if (!location.canWrite() && !location.canRead()) {
            throw new IOException("Could not read/write to directory " + location);
        }
        for (Class<?> interfaceClass : interfaceClasses) {
            if (!interfaceClass.isInterface()) {
                throw new IllegalArgumentException("Class " + interfaceClass.getName() + " was not an interface.");
            }
            List<Class<?>> implementingClasses = getImplementingClasses(location, location, interfaceClass);
            System.out.println("Added " + implementingClasses.size() + " implementations of " + interfaceClass.getName());
            File metaInf = new File(location, META_INF);
            if (!metaInf.exists()) {
                if (!metaInf.mkdir()) {
                    throw new IOException("Could not create directory " + metaInf);
                }
            }
            File servicesDirectory = new File(metaInf, SERVICES);
            if (!servicesDirectory.exists()) {
                if (!servicesDirectory.mkdirs()) {
                    throw new IOException("Could not create directory " + servicesDirectory);
                }
            }
            final File manifestFile = new File(servicesDirectory, interfaceClass.getName());
            if (!manifestFile.exists()) {
                if (!manifestFile.createNewFile()) {
                    throw new IOException("Could not create manifest file.");
                }
            }
            PrintWriter printWriter = new PrintWriter(manifestFile);
            try {
                for (Class<?> implementingClass : implementingClasses) {
                    printWriter.println(implementingClass.getName());
                }
            } finally {
                printWriter.flush();
                printWriter.close();
            }
        }
    }

    /**
     * Recursive method which recursively seeks out all classes implementing a certain interface
     * 
     * @param baseLocation File from which the search was started
     * @param location local file in which to search
     * @param interfaceClass an interface to search for
     * @return a list of classes implementing the given interface
     * @throws IOException 
     */
    private static List<Class<?>> getImplementingClasses(final File baseLocation, File location, Class<?> interfaceClass) {
        List<Class<?>> result = new ArrayList<Class<?>>();
        if (location.isDirectory()) {
            //Recurse to find all files in all subdirectories
            for (File file : location.listFiles()) {
                if (file.isDirectory()) {
                    result.addAll(getImplementingClasses(baseLocation, file, interfaceClass));
                } else {
                    if (file.getName().toLowerCase().endsWith(CLASS_EXTENSION)) {
                        String className = file
                                .getAbsolutePath()
                                .substring(baseLocation.getAbsolutePath().length() + File.separator.length(),
                                        file.getAbsolutePath().indexOf(CLASS_EXTENSION)).replace(File.separatorChar, '.');
                        try {
                            Class<?> candidate = Class.forName(className);
                            if (interfaceClass.isAssignableFrom(candidate) && !Modifier.isAbstract(candidate.getModifiers())
                                    && !candidate.isInterface()) {                              
                                result.add(candidate);
                            }
                        } catch (ClassNotFoundException e) {
                            throw new IllegalArgumentException("Class of name " + className + " was not found, even though a class file"
                                    + " of that name was found in " + baseLocation.getAbsolutePath(), e);
                        }
                    }
                }
            }
           
        }
        return result;
    }

}
