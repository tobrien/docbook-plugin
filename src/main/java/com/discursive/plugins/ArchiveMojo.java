package com.discursive.plugins;

import java.io.File;

/**
 * Goal to create a JAR-package containing all the source files of a DocBook book.
 * 
 * @extendsPlugin jar
 * @extendsGoal jar
 * @goal archive
 * @phase package
 * @requiresProject
 * @requiresDependencyResolution runtime
 */
public class ArchiveMojo extends AbstractArchiveMojo {

    /**
     * Directory containing the classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File classesDirectory;

    /**
     * Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.
     *
     * @parameter
     */
    private String classifier;

    protected String getClassifier()
    {
        return classifier;
    }

    /**
     * @return type of the generated artifact
     */
    protected String getType()
    {
        return "docbook";
    }

    /**
     * Return the main classes directory, so it's used as the root of the jar.
     */
    protected File getClassesDirectory()
    {
    	getLog().info( "Returning Classes directory: " + classesDirectory.toString() );
    	return classesDirectory;
    }	
	
}
