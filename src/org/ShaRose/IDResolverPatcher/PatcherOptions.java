package org.ShaRose.IDResolverPatcher;

import java.io.File;
import java.util.List;

import uk.co.flamingpenguin.jewel.cli.Option;

public interface PatcherOptions {

	@Option(description = "Raw .class files to attempt patching. Scanned first.",shortName = "c")
	List<File> getCla();
	
	boolean isCla();
	
	@Option(description = "Folders to scan. Only accepts .class. NOT recursivly scanned. Scanned Second.",shortName = "f")
	List<File> getFld();
	
	boolean isFld();
	
	@Option(description = "Basic archive types like .jar or .zip. Recursivly scanned. Scanned Third.",shortName = "a")
	List<File> getArc();
	
	boolean isArc();
	
	@Option(description = "Fallback archive. This is what the patcher will use if one of the classes are missing (for references). Only scanned if no matches for block or item, and no other locations. Normally, minecraft.jar.",shortName="b")
	File getFba();
	
	@Option(defaultValue="ID Resolver/",description = "The directory to output the patched files to.",shortName = "o")
	File getOut();
	
	@Option(helpRequest = true,description = "Prints this help message.",shortName = "h")
	boolean getHelp();
}
