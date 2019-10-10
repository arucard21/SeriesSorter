# SeriesSorter

## Name
	seriesSorter - sort files that are episodes of a series

## Synopsis
	seriesSorter [OPTIONS] FILE
	seriesSorter [OPTIONS] [-r] [DIRECTORY]

## Description
	Sort files that are episodes of a series with a standard format "<Showname> - <seasonNumber>x<Ignore>" or a given custom format.

	The file FILE is checked for a valid extension, as defined in the properties file, before sorting it to the directory where it belongs. If a file with the same name already exists at the target location, it is not overwritten and the file FILE is not sorted.
	If a directory DIRECTORY is provided, by default only the the files with a valid extension in that directory are sorted. You can use the option -r to recurse to all underlying directories
	The episodes will be sorted in separate folders per series and in that series folder again per season relative to a given base directory, resulting in a path of the form "BaseDir/SeriesName/SeasonNumber/"

	-c, --config path
		specify the path "path" where the configuration files will be stored.
		overrides the default value.
		(default: (user.home)/.SeriesRenamer or current directory if not available)

	-e, --existing
		sort episodes only into series directories that already exist. Episodes of non-existing seasons of existing series will still be sorted.
		(default: false)

	-f, --format pattern
		parse the episodes with a custom format "pattern". The pattern must be enclosed by double-quotes and should itself contain no double-quotes and can use the following variables:
			* <SeriesName> for the name of the series
			* <SeasonNumber> for the season number without any leading zeroes
			* <Ignore> for anything that should be ignored
		These are the only variables currently available for use in the name. Any format you provide must have the <SeriesName> as the first variable in the format and <SeasonNumber> as the second, where <SeasonNumber> is a number.
(default: "<SeriesName> - <SeasonNumber>x<Ignore>")

	-h, --help
		show this help message.

	-o, --output path
		specifies the base directory of the new path for the episodes. Relative to this directory, the episodes will be sorted in base_dir/SeriesName/SeasonNumber/ directories.
		(default: current directory)

	-r, --recursive
		search subfolders recursively to find files to sort. Only used with DIRECTORY, ignored otherwise.
		(default: false)

	    --simulate
		Simulate the sorting of the episodes. This shows the location the files would be sorted in but doesn't actually move the files
		(default: false)

	    --version
		show current version.

	--
		terminates all options, any options entered after this are not recognized as options and as such everything after this will be treated as DIRECTORY

	FILE
		the name of the file representing the episode.If not provided, seriesSorter will use the default value for DIRECTORY

	DIRECTORY
		the path to the directory which holds the files you wish to sort
		(default: current directory)