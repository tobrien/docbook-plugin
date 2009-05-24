package com.discursive.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class RegexTest extends TestCase {

	public RegexTest(String name) {
		super(name);
	}
	
	public void testRegex() throws Exception {


		String fileName = "book-cjcook.xml";
		String fileNameRegex = "book-(.*).xml";
		
		 Pattern p = Pattern.compile( fileNameRegex );
		 Matcher m = p.matcher( fileName);
		 boolean b = m.matches();
		 assertTrue( b );
		 String fileId = m.group(1);
		assertEquals( "cjcook", fileId ); 
	}
	
}
