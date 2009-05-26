package com.discursive.plugins;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * This Mojo simply verifies that each chapter has an appropriate identifer.
 * 
 * @goal inject-examples
 * @phase process-classes
 * @requiresProject
 */
public class InjectExamplesMojo extends AbstractMojo {

	/**
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	protected MavenProject project;

	/**
	 * @parameter
	 */
	protected String wildcardFilter = "*.xml";

	/**
	 * @parameter expression="${project.build.outputDirectory}"
	 * @required
	 */
	protected File outputDir;

	/**
	 * @parameter
	 */
	protected String attributeNamespace = "http://discursive.com/plugins/docbook";

	/**
	 * 
	 * @parameter expression="${localRepository}"
	 * @readonly
	 */
	private ArtifactRepository localRepository;

	/**
	 * @component
	 * @readonly
	 */
	private ArtifactFactory artifactFactory;
	
    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @component
     */
    protected ArchiverManager archiverManager;

	private List<String> validationFailures = new ArrayList<String>();

	public void execute() throws MojoExecutionException, MojoFailureException {

		FileFilter fileFilter = new WildcardFileFilter(wildcardFilter);
		File[] xmlFiles = outputDir.listFiles(fileFilter);
		for (int i = 0; i < xmlFiles.length; i++) {

			try {
				processXml(xmlFiles[i]);
			} catch (Exception e) {
				throw new MojoExecutionException("Problem parsing XML file: "
						+ xmlFiles[i], e);
			}

		}

		if (validationFailures.size() > 0) {
			throw new MojoFailureException("\n"
					+ StringUtils.join(validationFailures.iterator(), "\n"));
		}

	}

	private void processXml(File file) throws MojoFailureException, NoSuchArchiverException, MojoExecutionException, ParserConfigurationException, SAXException, IOException, XPathExpressionException, TransformerFactoryConfigurationError, TransformerException {

		String fileName = file.getName();
		Document doc = parse(file.toURL(), true);
		Element rootElement = doc.getDocumentElement();

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		XPathExpression expr = xpath.compile("//programlisting");
		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;
		for (int i = 0; i < nodes.getLength(); i++) {
			Element pl = (Element) nodes.item(i);
			String artifact = pl.getAttributeNS( attributeNamespace, "artifact");
			if (artifact != null && !artifact.equals("")) {
				String groupId, artifactId, version, classifier, packaging;
				String[] tokens = StringUtils.split(artifact, ":");
				if (tokens.length != 5 )
					throw new MojoFailureException(
							"Invalid artifact, you must specify "
									+ "groupId:artifactId:version:classifier:packaging"
									+ artifact);
				groupId = tokens[0];
				artifactId = tokens[1];
				version = tokens[2];
				classifier = tokens[3];
				packaging = tokens[4];

				Artifact toDownload = artifactFactory.createArtifactWithClassifier(groupId, artifactId, version, packaging, classifier);
				System.out.println( toDownload.toString() );
				
				String path = localRepository.pathOf( toDownload );
				System.out.println( path );
				
				File artifactFile = new File( localRepository.getBasedir(), path );
				
				String filename = pl.getAttributeNS( attributeNamespace, "file" );
				String xpath1 = pl.getAttributeNS( attributeNamespace, "xpath" );
				
				UnArchiver unArchiver;

	            unArchiver = archiverManager.getUnArchiver( artifactFile );

	            unArchiver.setSourceFile( artifactFile );

	            String property = "java.io.tmpdir";
	            // Get the temporary directory and print it.
	            String tempDirPath = System.getProperty(property);
	            
	            File tempDir = new File( tempDirPath );
	            try {
					unArchiver.extract( filename, tempDir );
				} catch (ArchiverException e) {
					throw new MojoExecutionException( "Error finding archiver for file" );
				}
			
				File workingFile = new File( tempDir, filename );
				System.out.println( "Working File: " + workingFile.toString() );
				
				Document workingDoc = parse( workingFile.toURL(), false );
				XPath xpath2 = factory.newXPath();
				XPathExpression expr2 = xpath2.compile(xpath1);
				System.out.println( xpath1 );
				Object result2 = expr2.evaluate(workingDoc, XPathConstants.NODESET);
				NodeList result2List = (NodeList) result2;
				System.out.println(  );
			
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				StreamResult result1 = new StreamResult( baos );
				DOMSource domSource = new DOMSource( result2List.item( 0 ) );
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty("omit-xml-declaration","yes");			
				transformer.transform( domSource, result1);
				String content = baos.toString("UTF-8");
				System.out.println( content );
				pl.setTextContent( content );
				
			}
			
		}
		
		StreamResult result1 = new StreamResult( file.toURI().getPath() );
		DOMSource domSource = new DOMSource( doc );
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.transform( domSource, result1);

	}

	public Document parse(URL url, boolean namespaceAware) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(namespaceAware); // never forget this!
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(url.openStream(), "test");
		return doc;
	}

}
