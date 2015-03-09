End goals
	Identify the set of unique photos across multiple Picasa & iPhoto accounts
	Replace resized photos in picasa with full-size versions
	For each photo identify where it is located

Scripts for image comparison
	http://www.fmwconcepts.com/imagemagick/phashconvert/index.php
		Generates 168 digit phash of image 
	http://www.fmwconcepts.com/imagemagick/phashcompare/index.php
		compares two 168 digit phash values

Add source specific logic/tracking to the db to allow it to avoid parsing the entire source worth of data
	AlbumFeed has <updated>2015-01-30T23:51:51.862Z</updated> so I just need a list of album Ids and their last-updated timestamps