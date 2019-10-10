package seriesSorter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SeriesSorter {
    private static String version = "0.2_2012_01_20";
    private static String configPath = String.valueOf(System.getProperty("user.home", ".")) + "/.SeriesSorter";
    private static String logFile = "/var/log/seriesRenamer.log";
    private static boolean existing = false;
    private static String format = "<SeriesName> - <SeasonNumber>x<Ignore>";
    private static String outputDir = "";
    private static boolean recursive = false;
    private static boolean nfo = false;
    private static boolean simulate = false;
    private static String targetFile = "";
    private static String propsPath = "/etc/seriesSorter.properties";
    private static Properties props = new Properties();

    public static void main(String[] args) {
	processArgs(args);
	boolean propsLoaded = loadProperties();
	if (!propsLoaded) {
	    logError("The properties could not be loaded");
	    System.exit(0);
	}
	log("Sorting started at: " + (new Timestamp(System.currentTimeMillis())).toString());
	sortFiles((new File(targetFile)).getAbsolutePath());
	log("Sorting ended at: " + (new Timestamp(System.currentTimeMillis())).toString());
	log("");
    }

    private static void sortFiles(String filename) {
	File file = new File(filename);
	if (file.isDirectory()) {
	    File[] files = file.listFiles();
	    for (File curFile : files) {
		if (curFile.isDirectory()) {
		    if (recursive) {
			sortFiles(curFile.getAbsolutePath());
		    }
		} else {
		    sortFiles(curFile.getAbsolutePath());
		}
	    }
	} else {
	    String name = file.getName();
	    String fileType = checkFileExtension(name);
	    if (fileType != null) {
		String seriesname = "";
		int seasonnum = -1;
		String formatRegex = format;
		formatRegex = formatRegex.replace("<SeriesName>", "(.*?)");
		formatRegex = formatRegex.replace("<SeasonNumber>", "([0-9]*?)");
		formatRegex = formatRegex.replace("<Ignore>", "(.*?)");
		Pattern fileVars = Pattern.compile(formatRegex);
		Matcher varsInFile = fileVars.matcher(name);
		if (varsInFile.find()) {
		    seriesname = varsInFile.group(1);
		    String season = varsInFile.group(2);
		    try {
			seasonnum = Integer.parseInt(season);
		    } catch (NumberFormatException num) {
			logError("The found season number was not a number");
		    }
		}
		String outputPath = "";
		if (!outputDir.equals("")) {
		    outputPath = String.valueOf(outputDir) + File.separator;
		}
		String subDir = "";
		if (fileType.equals("sub")) {
		    subDir = String.valueOf(File.separator) + "subtitles";
		}
		if (!seriesname.equals("") && seasonnum != -1) {
		    File sortedFile = new File(
			    String.valueOf(outputPath) + seriesname + File.separator + "Season " + seasonnum + subDir,
			    file.getName());
		    if (simulate) {
			log("Simulated sorting of File " + file.getName());
			log("\t" + file.getParent());
			log("\t-> " + sortedFile.getParent());
		    } else if (!sortedFile.exists()) {
			log("Moving File " + file.getName());
			log("\t" + file.getParent());
			log("\t-> " + sortedFile.getParent());
			if (existing && (!(new File(String.valueOf(outputPath) + seriesname)).exists()
				|| !(new File(String.valueOf(outputPath) + seriesname)).isDirectory())) {
			    logError(
				    "No existing series folder could be found for this series, try again without --existing, -e option");
			} else {
			    sortedFile.getParentFile().mkdirs();
			    File nfoFile = null;
			    File newNfoFile = null;
			    if (nfo) {
				File[] additionalFiles = file.getParentFile().listFiles();
				for (File curFile : additionalFiles) {
				    if (curFile.getName().matches(".*\\.nfo")) {
					nfoFile = curFile;
					break;
				    }
				}
				if (nfoFile != null) {
				    newNfoFile = new File(String.valueOf(outputPath) + seriesname + File.separator
					    + "Season " + seasonnum + File.separator + "nfo", nfoFile.getName());
				    newNfoFile.getParentFile().mkdirs();
				} else {
				    logError("No nfo file was found for " + file.getName());
				}
			    }
			    if (file.renameTo(sortedFile)) {
				if (nfo && nfoFile != null && newNfoFile != null) {
				    log("Sorting nfo");
				    if (nfoFile.renameTo(newNfoFile)) {
					log("Successfully sorted nfo");
				    }
				}
				log("Successfully sorted");
			    } else {
				moveFile(file, sortedFile);
				if (nfo && nfoFile != null && newNfoFile != null) {
				    log("Sorting nfo");
				    moveFile(nfoFile, newNfoFile);
				}
			    }
			}
		    } else {
			log("The file " + file.getName() + " is already at the correct location");
		    }
		} else {
		    logError("The location to sort the file " + file.getName() + " into could not be found");
		}
	    }
	}
    }

    private static void moveFile(File src, File dest) {
	FileInputStream source = null;
	FileOutputStream target = null;
	try {
	    source = new FileInputStream(src);
	    target = new FileOutputStream(dest);
	    byte[] buffer = new byte[4096];
	    for (int bytesRead = source.read(buffer); bytesRead != -1; bytesRead = source.read(buffer)) {
		target.write(buffer, 0, bytesRead);
	    }
	    source.close();
	    target.close();
	} catch (FileNotFoundException fnf) {
	    if (source == null) {
		logError("The source file could not be opened");
	    } else if (target == null) {
		logError("The file could not be created at the target location");
	    } else {
		logError("Unexpected FileNotFoundException found:\n" + fnf.toString());
	    }
	} catch (IOException io) {
	    logError("An I/O exception occured while attempting to copy the file.");
	}
	if (dest.exists() && dest.length() == src.length()) {
	    if (src.delete()) {
		if (src.getName().matches(".*\\.nfo")) {
		    log("Successfully sorted nfo");
		} else {
		    log("Successfully sorted");
		}
	    } else {
		logError("Source file could not be removed");
	    }
	} else if (dest.exists()) {
	    logError("Target file does not match the size of the source file and might not be correctly copied");
	} else {
	    logError("Could not be sorted");
	}
    }

    private static void processArgs(String[] args) {
	for (int curArgIndex = 0; curArgIndex < args.length; curArgIndex++) {
	    String curArg = args[curArgIndex];
	    if (curArg.charAt(0) == '-') {
		if (curArg.charAt(1) == '-') {
		    if (curArg.length() <= 2) {
			curArgIndex++;
			String target = "";
			while (curArgIndex < args.length) {
			    target = String.valueOf(target) + " " + curArg;
			}
			targetFile = target.trim();
		    } else {
			String curLongOpt = curArg.substring(2);
			String[] splitOpt = curLongOpt.split("=", -1);
			if (splitOpt.length < 1) {
			    logError("The command-line option: " + curLongOpt
				    + " could not be recognized after attempting to split it on the = character");
			} else if (splitOpt.length == 1) {
			    if (optionRequiresValue(curLongOpt)) {
				processArg(curLongOpt, args[curArgIndex + 1]);
				curArgIndex++;
			    } else {
				processArg(curLongOpt, null);
			    }
			} else if (splitOpt.length == 2) {
			    processArg(splitOpt[0], splitOpt[1]);
			} else if (splitOpt.length > 2) {
			    String optVal = splitOpt[1];
			    for (int i = 2; i < splitOpt.length; i++) {
				optVal = String.valueOf(optVal) + "=" + splitOpt[i];
			    }
			    processArg(splitOpt[0], optVal);
			}
		    }
		} else {
		    String curShortOpt = curArg.substring(1);
		    if (curShortOpt.length() <= 1) {
			if (optionRequiresValue(curShortOpt)) {
			    processArg(curShortOpt, args[curArgIndex + 1]);
			    curArgIndex++;
			} else {
			    processArg(curShortOpt, null);
			}
		    } else {
			String[] splitOpt = curShortOpt.split("=", -1);
			if (splitOpt.length < 1) {
			    logError("The command-line option: " + curShortOpt
				    + " could not be recognized after attempting to split it on the = character");
			} else if (splitOpt.length == 1) {
			    if (optionRequiresValue(Character.toString(splitOpt[0].charAt(0)))) {
				processArg(Character.toString(splitOpt[0].charAt(0)), splitOpt[0].substring(1));
			    } else {
				for (int i = 0; i < splitOpt[0].length(); i++) {
				    if (optionRequiresValue(Character.toString(splitOpt[0].charAt(i)))) {
					logError(
						"One of the arguments was provided in a combined set of arguments, but requires a value. This needs to be specified separately");
				    } else {
					processArg(Character.toString(splitOpt[0].charAt(i)), null);
				    }
				}
			    }
			} else if (splitOpt.length > 1) {
			    if (splitOpt[0].length() > 1) {
				logError("An option was provided with a long name without putting -- in front of it");
			    } else {
				String optVal = splitOpt[1];
				for (int i = 2; i < splitOpt.length; i++) {
				    optVal = String.valueOf(optVal) + "=" + splitOpt[i];
				}
				processArg(splitOpt[0], optVal);
			    }
			}
		    }
		}
	    } else if (curArgIndex >= args.length - 1) {
		targetFile = curArg;
	    } else {
		String target = curArg;
		while (curArgIndex < args.length) {
		    target = String.valueOf(target) + " " + args[curArgIndex];
		    curArgIndex++;
		}
		targetFile = target;
	    }
	}
    }

    private static boolean optionRequiresValue(String option) {
	boolean reqVal = false;
	if (option.equals("c") || option.equals("config") || option.equals("f") || option.equals("format")
		|| option.equals("o") || option.equals("output")) {
	    reqVal = true;
	}
	return reqVal;
    }

    private static void processArg(String opt, String val) {
	if (opt.equals("c") || opt.equals("config")) {
	    configPath = val;
	}
	if (opt.equals("e") || opt.equals("existing")) {
	    existing = true;
	}
	if (opt.equals("f") || opt.equals("format")) {
	    format = val;
	}
	if (opt.equals("h") || opt.equals("help")) {
	    System.out.print(
		    "Name\n\tseriesSorter - sort files that are episodes of a series\n\nSynopsis\n\tseriesSorter [OPTIONS] FILE\n\tseriesSorter [OPTIONS] [-r] [DIRECTORY]\n\nDescription\n\tSort files that are episodes of a series with a standard format \"<Showname> - <seasonNumber>x<Ignore>\" or a given custom format.\n\n\tThe file FILE is checked for a valid extension, as defined in the properties file, before sorting it to the directory where it belongs. If a file with the same name already exists at the target location, it is not overwritten and the file FILE is not sorted.\n\tIf a directory DIRECTORY is provided, by default only the the files with a valid extension in that directory are sorted. You can use the option -r to recurse to all underlying directories\n\tThe episodes will be sorted in separate folders per series and in that series folder again per season relative to a given base directory, resulting in a path of the form \"BaseDir/SeriesName/SeasonNumber/\"\n\n\t-c, --config path\n\t\tspecify the path \"path\" where the configuration files will be stored.\n\t\toverrides the default value.\n\t\t(default: (user.home)/.SeriesRenamer or current directory if not available)\n\n\t-e, --existing\n\t\tsort episodes only into series directories that already exist. Episodes of non-existing seasons of existing series will still be sorted.\n\t\t(default: false)\n\n\t-f, --format pattern\n\t\tparse the episodes with a custom format \"pattern\". The pattern must be enclosed by double-quotes and should itself contain no double-quotes and can use the following variables:\n\t\t\t* <SeriesName> for the name of the series\n\t\t\t* <SeasonNumber> for the season number without any leading zeroes\n\t\t\t* <Ignore> for anything that should be ignored\n\t\tThese are the only variables currently available for use in the name. Any format you provide must have the <SeriesName> as the first variable in the format and <SeasonNumber> as the second, where <SeasonNumber> is a number.\n(default: \"<SeriesName> - <SeasonNumber>x<Ignore>\")\n\n\t-h, --help\n\t\tshow this help message.\n\n\t-o, --output path\n\t\tspecifies the base directory of the new path for the episodes. Relative to this directory, the episodes will be sorted in base_dir/SeriesName/SeasonNumber/ directories.\n\t\t(default: current directory)\n\n\t-r, --recursive\n\t\tsearch subfolders recursively to find files to sort. Only used with DIRECTORY, ignored otherwise.\n\t\t(default: false)\n\n\t    --simulate\n\t\tSimulate the sorting of the episodes. This shows the location the files would be sorted in but doesn't actually move the files\n\t\t(default: false)\n\n\t    --version\n\t\tshow current version.\n\n\t--\n\t\tterminates all options, any options entered after this are not recognized as options and as such everything after this will be treated as DIRECTORY\n\n\tFILE\n\t\tthe name of the file representing the episode.If not provided, seriesSorter will use the default value for DIRECTORY\n\n\tDIRECTORY\n\t\tthe path to the directory which holds the files you wish to sort\n\t\t(default: current directory)\n\nReporting bugs\n\tReport bugs to arucard21@gmail.com\n");
	    System.exit(0);
	}
	if (opt.equals("o") || opt.equals("output")) {
	    outputDir = val;
	}
	if (opt.equals("r") || opt.equals("recursive")) {
	    recursive = true;
	}
	if (opt.equals("version")) {
	    System.out.println("SeriesSorter " + version);
	    System.out.println(
		    "This software can be used for free, but it can not be redistributed or changed without the express permission of the author.");
	    System.exit(0);
	}
	if (opt.equals("nfo")) {
	    nfo = true;
	}
	if (opt.equals("simulate")) {
	    simulate = true;
	}
    }

    private static boolean loadProperties() {
	FileReader propsReader = null;
	try {
	    File propsFile = new File(String.valueOf(configPath) + propsPath);
	    if (!propsFile.exists()) {
		File propsDir = propsFile.getParentFile();
		if (!propsDir.exists()) {
		    if (!propsDir.mkdirs()) {
			logError("The directory for the properties file " + propsPath + " could not be created");
		    }
		}
		propsFile.createNewFile();
		Properties defProps = new Properties();
		defProps.setProperty("validVideoExtensions", "avi;mkv;mp4;mpg");
		defProps.setProperty("validSubExtensions", "srt;idx;sub");
		FileWriter propsWriter = new FileWriter(propsFile);
		defProps.store(new BufferedWriter(propsWriter), "Properties for the SeriesSorter");
		propsWriter.close();
	    }
	    propsReader = new FileReader(propsFile);
	    props.load(new BufferedReader(propsReader));
	    propsReader.close();
	} catch (FileNotFoundException noFile) {
	    logError("Properties file " + propsPath + " can not be read");
	    StackTraceElement[] stacktrace = noFile.getStackTrace();
	    String stString = String.valueOf(noFile.toString()) + "\n";
	    for (StackTraceElement ste : stacktrace) {
		stString = String.valueOf(stString) + "\t" + ste.toString() + "\n";
	    }
	    logError(stString);
	} catch (IOException IO) {
	    if (propsReader == null) {
		logError("Properties file " + propsPath + " can not be created");
	    } else {
		logError("Properties could not be loaded from the properties file " + propsPath);
	    }
	    StackTraceElement[] stacktrace = IO.getStackTrace();
	    String stString = String.valueOf(IO.toString()) + "\n";
	    for (StackTraceElement ste : stacktrace) {
		stString = String.valueOf(stString) + "\t" + ste.toString() + "\n";
	    }
	    logError(stString);
	} catch (Exception other) {
	    logError("An unexpected error has occurred, please see the stacktrace for more info");
	    StackTraceElement[] stacktrace = other.getStackTrace();
	    String stString = String.valueOf(other.toString()) + "\n";
	    for (StackTraceElement ste : stacktrace) {
		stString = String.valueOf(stString) + "\t" + ste.toString() + "\n";
	    }
	    logError(stString);
	}
	return (props != null && !props.isEmpty());
    }

    private static String checkFileExtension(String name) {
	String type = null;
	String[] nameArr = name.split("\\.");
	String ext = "";
	if (nameArr.length > 0) {
	    ext = nameArr[nameArr.length - 1].toLowerCase();
	}
	String[] validVideoExts = props.getProperty("validVideoExtensions").split(";");
	String[] validSubExts = props.getProperty("validSubExtensions").split(";");
	if (type == null) {
	    for (String validExt : validVideoExts) {
		validExt = validExt.trim();
		if (ext.matches(validExt)) {
		    type = "video";
		}
	    }
	}
	if (type == null) {
	    for (String validExt : validSubExts) {
		validExt = validExt.trim();
		if (ext.matches(validExt)) {
		    type = "sub";
		}
	    }
	}
	return type;
    }

    private static void log(String logmessage) {
	System.out.println(logmessage);
	logToFile(logmessage, false);
    }

    private static void logError(String errorMessage) {
	System.err.println(errorMessage);
	logToFile(errorMessage, true);
    }

    private static void logToFile(String message, boolean error) {
	BufferedWriter logWriter = null;
	try {
	    File log = null;
	    log = new File(String.valueOf(configPath) + logFile);
	    if (!log.exists()) {
		File logDir = log.getParentFile();
		if (!logDir.exists()) {
		    if (!logDir.mkdirs()) {
			System.err.println("The directory for the log file " + logFile + " could not be created");
		    }
		}
		log.createNewFile();
	    }
	    logWriter = new BufferedWriter(new FileWriter(log, true));
	} catch (IOException IO) {
	    System.err.println("The log file " + logFile + " could not be created");
	    IO.printStackTrace();
	} catch (Exception other) {
	    System.err.println("An unexpected error has occurred, please see the stacktrace for more info");
	    other.printStackTrace();
	}
	try {
	    logWriter.newLine();
	    if (error) {
		logWriter.write("ERROR:/t");
	    } else {
		logWriter.write("INFO:/t");
	    }
	    logWriter.write(message);
	} catch (IOException logWriteIO) {
	    System.err.println("The log message could not be written to the log " + logFile);
	    logWriteIO.printStackTrace();
	}
	try {
	    logWriter.close();
	} catch (IOException e) {
	    System.err.println("The writer for the log file " + logFile + " could not be closed");
	    e.printStackTrace();
	}
    }

    public static void reset() {
	configPath = String.valueOf(System.getProperty("user.home", ".")) + "/.SeriesSorter";
	logFile = "/var/log/seriesSorter.log";
	existing = false;
	format = "<SeriesName> - <SeasonNumber>x<Ignore>";
	outputDir = "";
	recursive = false;
	targetFile = "";
	nfo = false;
	simulate = false;
    }
}