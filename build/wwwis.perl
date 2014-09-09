# use perl -*- Perl -*- This line used to have a : at the start
  eval 'exec perl -w -S $0 ${1+"$@"}'
  if 0;
# clever way of calling perl on this script : stolen from weblint
#!/usr/local/bin/perl -w
#
# wwwis: adds HEIGHT= and WIDTH= to images referenced in specified HTML file.
#
# for documentation - changelog and latest version
# see http://www.bungeezone.com/~ark/wwwis/
#  or http://www.tardis.ed.ac.uk/~ark/wwwis/
#
# this program by (and copyright)    Alex Knowles, alex@ed.ac.uk
# based on original code and idea by Andrew Tong,  werdna@ugcs.caltech.edu
#
# You may distribute this code under the GNU public license
#
# THIS SOFTWARE IS PROVIDED "AS-IS" WITHOUT WARRANTY OF ANY KIND.
#
# RCS $Id$


use strict;
use File::Copy;
use Socket;
# if you do not have these system libraries make sure you comment them out
# and have the options UsePerlCp, searchURLS, TryServer ALL SET TO NO

if( ! $\ ){
  # this stops the error Use of uninitialized value at .../File/Copy.pm line 84
  # print "Out rec sep not defined?? someone help me with this\n";
  $\='';
}

# this array specifies what options are available what the default
# value is and also what type it is, files are checked to see if they
# exist and the only possible values for choice are given.
# you should only need to change the third column
my(@options)=
  ('searchURLS',      'bool',    'Yes',
   'DocumentRoot',    'file',    '/usr/local/etc/httpd/htdocs',
   'UserDir',         'string',  'html',
   'MakeBackup',      'bool',	 'Yes',
   'BackupExtension', 'string',  '~',
   'OverwriteBackup', 'choice',  'Yes', 3, 'Yes','No','Ask',
   'ChangeIfThere',   'choice',  'Yes', 4, 'Yes','No','Ask','Clever',
   'Skip1x1',	      'bool',	 'Yes',
   'DoChmodChown',    'bool',    'No',
   'UpcaseTags',      'choice',  'No',  4, 'Yes','No','Upper','Lower',
   'TryServer',       'bool',    'Yes',
   'QuoteNums',       'choice',  'No',  4, 'Yes','No','Single','Double',
   'Munge%',	      'bool',    'Yes',
   'NeedAlt',	      'bool',    'Yes',
   'SkipCGI',	      'bool',	 'Yes',
   'UseNewGifsize',   'bool',    'No',
   'UseHash',	      'bool',    'Yes',
   'Base',	      'string',  '',
   'InFilter',	      'string',  '',
   'OutFilter',	      'string',  '',
   'Script',	      'string',  '',
   'Proxy',	      'string',  '',
   'IgnoreLinks',     'bool',    'Yes',
   'UsePerlCp',       'bool',    'Yes',
   );

######################################################################
######### YOU SHOULD NOT GHAVE TO CHANGE ANYTHING BELOW HERE #########
######################################################################


my($Base,   $SkipCGI,  $InFilter, $MakeBackup,   $SearchURLS, $OverwriteBackup,
   $Proxy,  $UseHash,  $OutFilter, $UpcaseTags,  $UseNewGifsize, $debug,
   $Script, $UserDir,  $TryServer, $DoChmodChown,$ChangeIfThere, $IgnoreLinks,
   $NeedAlt,$MungePer, $QuoteNums, $DocumentRoot,$BackupExtension,
   $UsePerlCp, $Skip1x1, );

my( %hashx, %hashy );

# O.K. now we have defined the options go and get them and set the global vars
my(@optionval)=&GetConfigFile(@options);
&SetGlobals();

$|=1;	# make it so that I can fit lots of info on one line...

############################################################################
# Main routine.  processes all files specified on command line, skipping
# any file for which a .bak file exists.
############################################################################
while (@ARGV) {
  my($FILE)=shift;
  if( $FILE =~ /^-/ ){
    &proc_arg($FILE);
    next;
  }

  print "$FILE -- ";

  if( -s $FILE && -T $FILE ){
    if ( -e "$FILE$BackupExtension"){
      if( &isfalse($OverwriteBackup) ){
	print "Skipping -- found $FILE$BackupExtension\n";
	next;
      } elsif ( $OverwriteBackup =~ /ASK/i ){
	print "overwite $FILE$BackupExtension [Yn]\n";
	$_=<STDIN>;
	if( /n/i ){
	  print " - Skipping\n";
	  next;
	}
      }
    }
    if ( -l $FILE and &istrue($IgnoreLinks) ){
      print "Skipping -- this file is a symbolic link\n";
      next;
    }
    print "Processing...\n";
    &convert($FILE);
  } else {
    print "Skipping -- Doesn't look like a text file to me!\n";
    next;
  }
}

# SetGlobals:
# This converts the optionval array into global variables
# this is cos I don't know how to store pointers to variables in arrys (sorry)
sub SetGlobals
{
  my($i)=0;

  $SearchURLS =		$optionval[$i++];
  $DocumentRoot =	$optionval[$i++];
  $UserDir =		$optionval[$i++];
  $MakeBackup =		$optionval[$i++];
  $BackupExtension =	$optionval[$i++];
  $OverwriteBackup =	$optionval[$i++];
  $ChangeIfThere =	$optionval[$i++];
  $Skip1x1 =		$optionval[$i++];
  $DoChmodChown =	$optionval[$i++];
  $UpcaseTags =		$optionval[$i++];
  $TryServer =		$optionval[$i++];
  $QuoteNums =		$optionval[$i++];
  $MungePer =		$optionval[$i++];
  $NeedAlt =		$optionval[$i++];
  $SkipCGI =		$optionval[$i++];
  $UseNewGifsize =	$optionval[$i++];
  $UseHash =		$optionval[$i++];
  $Base =		$optionval[$i++];
  $InFilter =		$optionval[$i++];
  $OutFilter =		$optionval[$i++];
  $Script =		$optionval[$i++];
  $Proxy =		$optionval[$i++];
  $IgnoreLinks =	$optionval[$i++];
  $UsePerlCp   =        $optionval[$i++];

  # do a quick check just to see we got everything
  $i--;
  if( $i!=$#optionval ){
    print "Internal Error: number of options is not equal to globals!\n";
    print "Please Email alex\@ed.ac.uk for help\n";
    exit;
  }
}

###########################################################################
# Subroutine does all the actual HTML parsing --- grabs image URLs and tells
# other routines to open the images and get their size
###########################################################################
sub convert {
  my($file) = @_;
  my($ox,$oy,$nx,$ny);
  my($changed,$type,$tag,$five,$user,$original,@original);
  my($HTMLbase,$i);
  my(@PATH,$REL,$rel);

  my($ino, $mode, $uid, $gid, $ngid, $nuid );

  $changed=0;	# did we change this file
  $original="";	# the string containing the whole file

  if( !open(ORIGINAL, $InFilter =~ /\S+/ ? "$InFilter $file|" : "<$file") ){
    print "Couldn't open $file\n";
    return;
  }
  while (<ORIGINAL>) {
    $original .= $_;
  }
  close (ORIGINAL);
  @PATH = split(/[\\\/]/, $file); # \\ for NT (brian_helterline@om.cv.hp.com)
  pop(@PATH);
  $REL = join("/", @PATH);

  # print out the header to the columns
  printf(" %s %-34s %-9s %-9s\n",'Type','File','   Old','   New');

  @original=split(/</, $original);
  for ($i=0; $i <= $#original; $i++) {
    # make the tags upper case if that's is what the user wants
    if( &istrue( $UpcaseTags) && $original[$i] !~ /^!--/ ){
      $original[$i]=&changecase($original[$i]);
    }

    if ($original[$i] =~ /^BASE\s+HREF\s*=\s*(\"[^\"]+\"|\'[^\']+\'|\S+)/i){ #"
      # we found a BASE tag this is quite important to us!
      $HTMLbase=&strip_quotes($1);
      print " BASE $HTMLbase\n";
    } elsif ($original[$i] =~
	     /^((IMG|FIGURE|INPUT)\s+([^\000]*\s+)?SRC\s*=\s*(\"[^\"]+\"|\'[^\']+\'|\S+)[^\000]*)>/i){	     #"
      # we found an IMG or FIGURE tag! this is really important

      # initialise some of my flags
      if( !defined($1) || !defined($2) || !defined($4) ){
	print "  Couldn't find tagtype or images source for tag number $i!\n";
	return;
      }
      $tag=$1;  # The whole HTML tag (with attributes)
      $type=$2; # this is either IMG or FIGURE
      $five=$4; # we put the SRC in a variable called five for historic reasons
      $five=&strip_quotes($five);
      $ox=0; $oy=0; # old X & Y values (Was Width & Height)
      $nx=0; $ny=0; # the new values

      printf("  %3s %-34s ",substr($type,0,3),$five);

      if(&istrue($SkipCGI) &&
	 $five =~ /(\.cgi$|\/cgi-bin\/)/ ){
	print "Skipping CGI program\n";
	next;
      }

      if( $tag =~ /(width|height)\s*=\s*[\"\']?\d+%/i ){ #"
	# we found a % sign near width or height
	if( ! &istrue($MungePer) ){
	  print "Found % Skipping\n";
	  next;
	}
      } else {
	$ox=$2 if( $tag =~ /\s*width\s*=\s*(\"|\')?(\d+)\s*/i );  #"
	$oy=$2 if( $tag =~ /\s*height\s*=\s*(\"|\')?(\d+)\s*/i ); #"
      }

      printf("(%3d,%3d) ",$ox,$oy);

      if( $ox && $oy && &isfalse($ChangeIfThere) ){
	print "Already There\n";
	next;
      }

      if( defined($HTMLbase) && $HTMLbase =~ /\S+/ ){
	print "\nUsing HTMLbase to turn:$five\n" if $debug;
	$five=&ARKjoinURL($HTMLbase,$five);
	print "Into                :$five\n"     if $debug;
      }

      if ($five =~ /^http:\/\/.*/) {
	if (&istrue($SearchURLS)) {
	  ($nx,$ny) = &URLsize($five);
	}
      } elsif ($five =~ /^\/\~.*/) {
	@PATH = split(/\//, $five);
	shift(@PATH); $user = shift(@PATH) ; $rel = join ("/", @PATH);
	$user =~ s/^\~//;
	$user=(getpwnam( $user ))[7];
	print "User dir is $user/$UserDir/$rel\n" if $debug;
	($nx,$ny) = &imgsize("$user/$UserDir/$rel",$five);
      } elsif ($five =~ /^\/.*/) {
	($nx,$ny) = &imgsize("$DocumentRoot$five",$five);
      } else {
	if ($REL eq '') {
	  ($nx,$ny) = &imgsize("$five",$five);
	} else {
	  ($nx,$ny) = &imgsize("$REL/$five",$five);
	}
      }

      if( $nx==0 && $ny==0 ){
	print "No Values : $!\n";
	next;
      }

      printf( "(%3d,%3d) ", $nx,$ny);

      if(&istrue($Skip1x1) &&
	 $nx==1 && $ny==1){
	print "Skipping 1x1 image\n";
	next;
      }

      if( $nx && $ny && &do_change($ox,$oy,$nx,$ny)){
	$changed=1;		# mark the page as changed
	$original[$i]=&replce_attrib($original[$i],'HEIGHT',$ny);
	$original[$i]=&replce_attrib($original[$i],'WIDTH',$nx);
	if( $ox==0 && $oy==0 ){
	  print "Added tags ";
	} else {
	  print "Updated ";
	}
      }

      print "Needs Alt" if(&istrue($NeedAlt) && $tag !~ /ALT\s*=\s*\S+/i );

      print "\n";
    }
  }

  if( !($changed)) {
    print " No need to write the file nothing changed\n";
    return;
  }

  if( ! &isfalse($MakeBackup) ){
    # maybe I should move the rest of this stuff into a separate function?
    if( &istrue($DoChmodChown) ){
      # find out about this file
      ($ino,$mode,$uid,$gid) = (stat($file))[1,2,4,5];
      if ($ino == 0 || !rename($file, "$file$BackupExtension")) {
	if( $ino == 0 ){
	  print "Couldn't stat $file for permissions & ownership\n";
	} else {
	  print "couldn't rename file for backup\n";
	}
	return;
      }
    } else {
      if( &istrue( $UsePerlCp ) ){
	copy( $file, "$file$BackupExtension" );
      } else {
	# system( "cp $file $file$BackupExtension" );
	# we could have added the -p flag e.g. cp -p ....
	# use copy cos this keeps the permissions the same!
	system( "cp -p $file $file$BackupExtension" );
      }
    }
  }

  $file="output.html" if $debug;

  if(open(CONVERTED, $OutFilter =~ /\S+/ ? "|$OutFilter $file" : ">$file") ){
    print CONVERTED join("<", @original);
    close(CONVERTED);

    if( &istrue($DoChmodChown) ){
      # now change the ownership & permissions
      chmod $mode, $file || print "Warning: Couldn't chmod $file\n";
      # It seems that chown doesn't necessarily indicate any errors
      chown $uid, $gid, $file || print "Warning: Couldn't chown $file\n";

      ($nuid,$ngid) = (stat($file))[4,5];
      if ($nuid != $uid ||
	  $ngid != $gid   ){
	print "Warning: $file now has different group or owner\n";
      }
    }
    # if we defined a script to run the make it so....
    system("$Script $file")     if( $Script =~ /\S+/ );
  } else {
    print "Either: could not backup or could not write to $file!\n";
  }
}

# replaces the $attrib's value to $val in $line
# if $attrib is not present it is inserted at the start of the tag
sub replce_attrib
{
  my($line,$attrib,$val)=@_;
  my( $start, $oldval );

  # argument checking
  if(!defined($line ) ||
     !defined($attrib) ||
     !defined($val)){
    print "Error: dodgy arguments to replace_attrib!\n";
    return $line if(defined($line)); # have no effect if we can
    exit;
  }

  $attrib =~ tr/[A-Z]/[a-z]/ if($UpcaseTags=~/lower/i);

  if( !(&isfalse($QuoteNums)) ){
    if( $QuoteNums =~ /single/i ){
      $val = "\'" . $val . "\'";
    } else {
      $val = "\"" . $val . "\"";
    }
  }

  if( $line =~ /(\s+$attrib\s*=\s*)([\'\"]?\d+%?[\'\"]?)[^\000]*>/i ){ #"
    $start=$1;
    $oldval=$2;
    $line =~ s/$start$oldval/$start$val/;
  } else {
    $line =~ s/(\S+\s+)/$1$attrib=$val /;
  }
  return $line;
}

sub ask_for_change{
  my($ret)=1;
  print "Change [Yn]?";
  $_=<STDIN>;
  if( /n/i ){
    $ret=0;
  }
  return $ret;
}

sub do_change{
  my($oldwidth, $oldheight, $newwidth, $newheight) = @_;
  my($wrat);
  my($hrat);

  return 0 if (!defined($oldwidth)	||
	       !defined($oldheight)	||
	       !defined($newwidth)	||
	       !defined($newheight)	||
	       !($newwidth)		||
	       !($newheight)              ||
	       ($oldwidth ==$newwidth &&
		$newheight==$oldheight));

  return 1 if(!($oldwidth) && !($oldheight) );

  if( &isfalse($ChangeIfThere) ){
    return 0;
  } elsif( $ChangeIfThere =~ /clever/i ){
    if( $oldwidth ){
      eval { $wrat= $newwidth  / $oldwidth  }; warn $@ if $@;
      if( $wrat < 1.0 ){
	eval {$wrat = 1/ $wrat }; warn $@ if $@;
      }
    } else {
      $wrat=1.5;
    }
    if( $oldheight ){
      eval { $hrat= $newheight / $oldheight }; warn $@ if $@;
      if( $hrat < 1.0 ){
	eval {$hrat = 1/ $hrat }; warn $@ if $@;
      }
    } else {
      $hrat=1.5;
    }
    if((int($wrat) == $wrat) &&
       (int($hrat) == $hrat) ){
      return 0;
    } else {
      return &ask_for_change();
    }
  } elsif($ChangeIfThere =~ /ask/i){
    return &ask_for_change();
  }
  return 1;
}

# looking at the filename really sucks I should be using the first 4 bytes
# of the image. If I ever do it these are the numbers.... (from chris@w3.org)
#  PNG 89 50 4e 47
#  GIF 47 49 46 38
#  JPG ff d8 ff e0
#  XBM 23 64 65 66
sub imgsize {
  my($file)= shift @_;
  my($ref)=@_ ? shift @_ : "";
  my($x,$y)=(0,0);

  # first check the hash table (if we use one)
  # then try and open the file
  # then try the server if we know of one
  if(&istrue($UseHash) &&
     $hashx{$file}     &&
     $hashy{$file}     ){
    print "Hash " if $debug;
    $x=$hashx{$file};
    $y=$hashy{$file};
  } elsif( defined($file) && open(STRM, "<$file") ){
    binmode( STRM ); # for crappy MS OSes - Win/Dos/NT use is NOT SUPPORTED
    if ($file =~ /\.jpg$/i || $file =~ /\.jpeg$/i) {
      ($x,$y) = &jpegsize(\*STRM);
    } elsif($file =~ /\.gif$/i) {
      ($x,$y) = &gifsize(\*STRM);
    } elsif($file =~ /\.xbm$/i) {
      ($x,$y) = &xbmsize(\*STRM);
    } elsif($file =~ /\.png$/i) {
      ($x,$y) = &pngsize(\*STRM);
    } else {
      print "$file is not gif, xbm, jpeg or png (or has stupid name)";
    }
    close(STRM);

    if(&istrue($UseHash) && $x && $y){
      $hashx{$file}=$x;
      $hashy{$file}=$y;
    }

  } else {
    # we couldn't open the file maybe we want to try the server?

    if(&istrue($TryServer) &&
       defined($ref) &&
       $ref =~ /\S+/ &&
       $Base =~ /\S+/ ){
      $ref= &ARKjoinURL( $Base, $ref );
      print "Trying server for $ref\n" if $debug;

      ($x,$y)=&URLsize($ref);
    }
  }

  return ($x,$y);
}

###########################################################################
# Subroutine gets the size of the specified GIF
###########################################################################
sub gifsize
{
  my($GIF) = @_;
  if( &istrue($UseNewGifsize) ){
    return &NEWgifsize($GIF);
  } else {
    return &OLDgifsize($GIF);
  }
}


sub OLDgifsize {
  my($GIF) = @_;
  my($type,$a,$b,$c,$d,$s)=(0,0,0,0,0,0);

  if(defined( $GIF )		&&
     read($GIF, $type, 6)	&&
     $type =~ /GIF8[7,9]a/	&&
     read($GIF, $s, 4) == 4	){
    ($a,$b,$c,$d)=unpack("C"x4,$s);
    return ($b<<8|$a,$d<<8|$c);
  }
  return (0,0);
}

# part of NEWgifsize
sub gif_blockskip {
  my ($GIF, $skip, $type) = @_;
  my ($s)=0;
  my ($dummy)='';

  read ($GIF, $dummy, $skip);	# Skip header (if any)
  while (1) {
    if (eof ($GIF)) {
      warn "Invalid/Corrupted GIF (at EOF in GIF $type)\n";
      return "";
    }
    read($GIF, $s, 1);		# Block size
    last if ord($s) == 0;	# Block terminator
    read ($GIF, $dummy, ord($s));	# Skip data
  }
}

# this code by "Daniel V. Klein" <dvk@lonewolf.com>
sub NEWgifsize {
  my($GIF) = @_;
  my($cmapsize, $a, $b, $c, $d, $e)=0;
  my($type,$s)=(0,0);
  my($x,$y)=(0,0);
  my($dummy)='';

  return($x,$y) if(!defined $GIF);

  read($GIF, $type, 6);
  if($type !~ /GIF8[7,9]a/ || read($GIF, $s, 7) != 7 ){
    warn "Invalid/Corrupted GIF (bad header)\n";
    return($x,$y);
  }
  ($e)=unpack("x4 C",$s);
  if ($e & 0x80) {
    $cmapsize = 3 * 2**(($e & 0x07) + 1);
    if (!read($GIF, $dummy, $cmapsize)) {
      warn "Invalid/Corrupted GIF (global color map too small?)\n";
      return($x,$y);
    }
  }
 FINDIMAGE:
  while (1) {
    if (eof ($GIF)) {
      warn "Invalid/Corrupted GIF (at EOF w/o Image Descriptors)\n";
      return($x,$y);
    }
    read($GIF, $s, 1);
    ($e) = unpack("C", $s);
    if ($e == 0x2c) {		# Image Descriptor (GIF87a, GIF89a 20.c.i)
      if (read($GIF, $s, 8) != 8) {
	warn "Invalid/Corrupted GIF (missing image header?)\n";
	return($x,$y);
      }
      ($a,$b,$c,$d)=unpack("x4 C4",$s);
      $x=$b<<8|$a;
      $y=$d<<8|$c;
      return($x,$y);
    }
    if ($type eq "GIF89a") {
      if ($e == 0x21) {		# Extension Introducer (GIF89a 23.c.i)
	read($GIF, $s, 1);
	($e) = unpack("C", $s);
	if ($e == 0xF9) {	# Graphic Control Extension (GIF89a 23.c.ii)
	  read($GIF, $dummy, 6);	# Skip it
	  next FINDIMAGE;	# Look again for Image Descriptor
	} elsif ($e == 0xFE) {	# Comment Extension (GIF89a 24.c.ii)
	  &gif_blockskip ($GIF, 0, "Comment");
	  next FINDIMAGE;	# Look again for Image Descriptor
	} elsif ($e == 0x01) {	# Plain Text Label (GIF89a 25.c.ii)
	  &gif_blockskip ($GIF, 12, "text data");
	  next FINDIMAGE;	# Look again for Image Descriptor
	} elsif ($e == 0xFF) {	# Application Extension Label (GIF89a 26.c.ii)
	  &gif_blockskip ($GIF, 11, "application data");
	  next FINDIMAGE;	# Look again for Image Descriptor
	} else {
	  printf STDERR "Invalid/Corrupted GIF (Unknown extension %#x)\n", $e;
	  return($x,$y);
	}
      }
      else {
	printf STDERR "Invalid/Corrupted GIF (Unknown code %#x)\n", $e;
	return($x,$y);
      }
    }
    else {
      warn "Invalid/Corrupted GIF (missing GIF87a Image Descriptor)\n";
      return($x,$y);
    }
  }
}

sub xbmsize {
  my($XBM) = @_;
  my($input)="";

  if( defined( $XBM ) ){
    $input .= <$XBM>;
    $input .= <$XBM>;
    $input .= <$XBM>;
    $_ = $input;
    if( /.define\s+\S+\s+(\d+)\s*\n.define\s+\S+\s+(\d+)\s*\n/i ){
      return ($1,$2);
    }
  }
  return (0,0);
}

#  pngsize : gets the width & height (in pixels) of a png file
# cor this program is on the cutting edge of technology! (pity it's blunt!)
#  GRR 970619:  fixed bytesex assumption
sub pngsize {
  my($PNG) = @_;
  my($head) = "";
# my($x,$y);
  my($a, $b, $c, $d, $e, $f, $g, $h)=0;

  if(defined($PNG)				&&
     read( $PNG, $head, 8 ) == 8		&&
     $head eq "\x89\x50\x4e\x47\x0d\x0a\x1a\x0a" &&
     read($PNG, $head, 4) == 4			&&
     read($PNG, $head, 4) == 4			&&
     $head eq "IHDR"				&&
     read($PNG, $head, 8) == 8 			){
#   ($x,$y)=unpack("I"x2,$head);   # doesn't work on little-endian machines
#   return ($x,$y);
    ($a,$b,$c,$d,$e,$f,$g,$h)=unpack("C"x8,$head);
    return ($a<<24|$b<<16|$c<<8|$d, $e<<24|$f<<16|$g<<8|$h);
  }
  return (0,0);
}

# jpegsize : gets the width and height (in pixels) of a jpeg file
# Andrew Tong, werdna@ugcs.caltech.edu           February 14, 1995
# modified slightly by alex@ed.ac.uk
sub jpegsize {
  my($JPEG) = @_;
  my($done)=0;
  my($c1,$c2,$ch,$s,$length, $dummy)=(0,0,0,0,0,0);
  my($a,$b,$c,$d);

  if(defined($JPEG)		&&
     read($JPEG, $c1, 1)	&&
     read($JPEG, $c2, 1)	&&
     ord($c1) == 0xFF		&&
     ord($c2) == 0xD8		){
    while (ord($ch) != 0xDA && !$done) {
      # Find next marker (JPEG markers begin with 0xFF)
      # This can hang the program!!
      while (ord($ch) != 0xFF) { return(0,0) unless read($JPEG, $ch, 1); }
      # JPEG markers can be padded with unlimited 0xFF's
      while (ord($ch) == 0xFF) { return(0,0) unless read($JPEG, $ch, 1); }
      # Now, $ch contains the value of the marker.
      if ((ord($ch) >= 0xC0) && (ord($ch) <= 0xC3)) {
	return(0,0) unless read ($JPEG, $dummy, 3);
	return(0,0) unless read($JPEG, $s, 4);
	($a,$b,$c,$d)=unpack("C"x4,$s);
	return ($c<<8|$d, $a<<8|$b );
      } else {
	# We **MUST** skip variables, since FF's within variable names are
	# NOT valid JPEG markers
	return(0,0) unless read ($JPEG, $s, 2);
	($c1, $c2) = unpack("C"x2,$s);
	$length = $c1<<8|$c2;
	last if (!defined($length) || $length < 2);
	read($JPEG, $dummy, $length-2);
      }
    }
  }
  return (0,0);
}

###########################################################################
# Subroutine grabs a gif from another server, and gets its size
###########################################################################


sub URLsize {
  my($five) = @_;
  my($dummy, $server, $url);
  my($c1, $c2, $c3, $c4)=(0,0,0,0);

  my( $x,$y) = (0,0);

  print "URLsize: $five\n" if $debug;

  # first check the hash table (if we're using one)
  if(&istrue($UseHash) &&
     $hashx{$five}     &&
     $hashy{$five}     ){
    print "Hash " if $debug;

    $x=$hashx{$five};
    $y=$hashy{$five};
    return($x,$y);
  }

  if( $Proxy =~ /\S+/ ){
    ($dummy, $dummy, $server, $url)     = split(/\//, $Proxy, 4);
    $url=$five;
  } else {
    ($dummy, $dummy, $server, $url) = split(/\//, $five, 4);
    $url= '/' . $url;
  }

  my($them,$port) = split(/:/, $server);
  my( $iaddr, $paddr, $proto );

  $port = 80 unless $port;
  $them = 'localhost' unless $them;

  print "\nThey are $them on port $port\n" if $debug;# && $Proxy;
  print "url is $url\n" 		   if $debug;

  $_=$url;
  if( /gif/i || /jpeg/i || /jpg/i || /xbm/i || /png/i ){

    $iaddr= inet_aton( $them );
    $paddr= sockaddr_in( $port, $iaddr );
    $proto=getprotobyname('tcp');

    # Make the socket filehandle.

    if(socket(STRM, PF_INET, SOCK_STREAM, $proto) &&
       connect(STRM,$paddr) ){
      # Set socket to be command buffered.
      select(STRM); $| = 1; select(STDOUT);

      print "Getting $url\n" if $debug;

      my $str=("GET $url HTTP/1.1\n".
	       "User-Agent: Mozilla/4.08 [en] (WWWIS)\n".
	       "Accept: */*\n".
	       "Connection: close\n".
	       "Host: $them\n\n");

      print "$str" if $debug;

      print STRM $str;

      # we're looking for \n\r\n\r
      while ((ord($c1) != 10) || (ord($c2) != 13) || (ord ($c3) != 10) ||
	     (ord($c4) != 13)) {
	$c4 = $c3;
	$c3 = $c2;
	$c2 = $c1;
	read(STRM, $c1, 1);
	print "$c1" if $debug;
      }
      print "\n" if $debug;

      if ($url =~ /\.jpg$/i || $url =~ /\.jpeg$/i) {
	($x,$y) = &jpegsize(\*STRM);
      } elsif($url =~ /\.gif$/i) {
	($x,$y) = &gifsize(\*STRM);
      } elsif($url =~ /\.xbm$/i) {
	($x,$y) = &xbmsize(\*STRM);
      } elsif($url =~ /\.png$/i) {
	($x,$y) = &pngsize(\*STRM);
      } else {
	print "$url is not gif, jpeg, xbm or png (or has stupid name)";
      }

      close ( STRM );
    } else {
      # there was a problem
      print "ERROR: $!";
    }
  } else {
    print "$url is not gif, xbm or jpeg (or has stupid name)";
  }
  if(&istrue($UseHash) && $x && $y){
    $hashx{$five}=$x;
    $hashy{$five}=$y;
  }
  return ($x,$y);
}

sub istrue
{
  my( $val)=@_;
  return (defined($val) && ($val =~ /^y(es)?/i || $val =~ /true/i ));
}

sub isfalse
{
  my( $val)=@_;
  return (defined($val) && ($val =~ /^no?/i || $val =~ /false/i ));
}

sub strip_quotes{
  my($name)=@_;

  $_=$name; # now to gte rid of quotes if they were there
     if(  /\"([^\"]*)\"/ ){ return $1; } #"
  elsif(  /\'([^\']*)\'/ ){ return $1; }
  return $name;
}

# this doesn't cope with \-ed " which it should!!!
# I also didn't cope with javascript stuff like onChange (whoops)
# this is why it is unsupported.
sub changecase{
  my($text)=@_;
  my( @line )=();
  my( $ostr, $str, $j )=("","",0);

  $text=~/^([^>]*)>/;
  return $text if( !defined($1));
  $ostr=$str=$1;

  @line=split(/\"/, $str); #"

  for( $j=0 ; $j <= $#line ; $j+=2 ){
    if( $UpcaseTags =~ /lower/i ){
      $line[$j] =~ tr/[A-Z]/[a-z]/;
    } else {
      $line[$j] =~ tr/[a-z]/[A-Z]/;
    }
  }
  if( $str =~ /\"$/ ){ #"
    $str=join( "\"", @line , "");
  } else {
    $str=join( "\"", @line );
  }
  $text=~ s/^$ostr/$str/;

  return $text;
}

# joins together two URLS to make one url
# e.g. http://www/             +  fish.html = http://www/fish.html
# e.g. http://www/index.html   +  fish.html = http://www/fish.html
# e.g. http://www/s/index.html + /fish.html = http://www/fish.html
sub ARKjoinURL
{
  my($base,$url)=@_;

  # if url has a double // in it then it is fine thank you!
  return $url if( $url =~ /\/\// );

  # strip down base url to make sure that it doesn't have a .html at the end
  $base=~s/[^\/]*$//;

  if( $url =~ /^\// ){
    # strip off leading directories
    $base =~ s/(\/\/[^\/]*)\/.*$/$1/;
  }

  return ($base . $url);
}

# File: wwwis-options.pl		-*- Perl -*-
# Created by: Alex Knowles (alex@ed.ac.uk) Sat Nov  2 16:41:12 1996
# Last Modified: Time-stamp: <03 Nov 96 1549 Alex Knowles>
# RCS $Id$
############################################################################
# There now follows some routines to get the configuration file
############################################################################

# NextOption:
# give me the start of the next option (as options can take up a
# different number of array elements)
sub NextOption
{
  my($i) = @_;

  $_=$options[$i+1];
  if( /string/i || /integer/i || /file/i || /bool/i ){
    $i+=3;
  } elsif( /choice/i ){
    $i+=4+$options[$i+3];
  }else {
    print "unknown option type! $_\n";
    exit 2;
  }
  return $i;
}

# ShowOptions: now I use -usage it's much better

# CheckOption:
# Check if $val (arg2) is valid for option which starts at options[$i (arg1)]
# returns either 0 (failure) or 1 (success)
sub CheckOption
{
  my($i,$val) = @_;
  my($k);

  return 0 unless $i && $val;

  $_=$options[$i+1];
  if( /string/i ){
    # can't think of a check for this
  }elsif( /integer/i ){
    if( $val !~ /^\d+$/ ){
      print "$val is not an integer!\n";
      return 0;
    }
  } elsif( /file/i ){
    if( ! (-e ($val) ) ){
      print "can't find file $val for $options[$i]\n";
      return 0;
    }
  }elsif( /bool/i ){
    if( $val !~ /^(y(es)?|no?)$/i ){
      print "$val is neither Yes nor No\n";
      return 0;
    }
  }elsif( /choice/i ){
    for( $k=0 ; $k < $options[$i+3] ; $k++ ){
      if( $val =~ /^$options[$i+4+$k]$/i ){
	return 1;
      }
    }
    print "$val is not a valid value for $options[$i]\n";
    return 0;
  }else {
    print "unknown option type! $_\n";
    exit 2;
  }
  return 1;
}

# GetConfigFile:
# Read user's configuration file, if such exists.  If WWWIMAGESIZERC is
# set in user's environment, then read the file referenced, otherwise
# try for $HOME/.wwwimagesizerc
sub GetConfigFile
{
  my( @options )= @_;
  my( @optionval )=();
  # my(*CONFIG);
  my($filename)="";
  my(@files)=();
  my($i,$j,$line);

  #first go through options array and puyt the default values into optionval
  $i=0;
  $j=0;
  while( $i < $#options ){
    $optionval[$j]=$options[$i+2];
    $i=&NextOption($i);
    $j++;
  }

  push(@files,$ENV{'WWWISRC'}) if $ENV{'WWWISRC'};
  push(@files,$ENV{'WWWIMAGESIZERC'}) if $ENV{'WWWIMAGESIZERC'};
  push(@files,("$ENV{'HOME'}/.wwwisrc",
	      "$ENV{'HOME'}/.wwwimagesizerc",)) if $ENV{'HOME'};

  foreach $i (@files){
    if( defined($i) && -f $i ){
      $filename=$i;
      last;
    }
  }

  if(defined($filename)	&&
     -f $filename		&&
     open(CONFIG,"< $filename") ){
    while (<CONFIG>){
      # skip lines with a hash on them
      s/#.*$//;
      next if /^\s*$/;

      $line=$_;
      if( $line =~ /^(\S+)(\s+|\s*:\s*)(.+)$/ ){
	if( !(&proc_option($1,$3)) ){
	  print "Invalid .wwwisrc line: $line";
	}
      }
    }
    close CONFIG;
  } else {
    if( -f $filename ){
      print "Unable to read config file `$filename': $!\n";
    }
  }
  return @optionval;
}

sub proc_option
{
  my($opt,$value)=@_;
  my($i,$j,$proced)=(0,0,0);

  return 0 unless $opt && $value;

  while( !$proced && $i < $#options ){
    if( $options[$i] =~ /$opt/i ){
      $proced=1;
      if( &CheckOption($i,$value) ){
	$optionval[$j]=$value;
      } else {
	printf("Invalid .wwwisrc value \"%s\" for option \"%s\"\n",
	       $value,$options[$i]);
      }
    }

    $i=&NextOption($i);	# move onto the next option
    $j++;
  }
  return $proced;
}

sub proc_arg
{
  my($arg)= @_;

  return if !defined($arg);

  if( $arg =~ /^-+v(ersion)?$/i ){
    my($version)='$Revision$ ';
    my($progname)=$0;
    $progname =~ s/.*\///;	# we only want the name
    $version =~ s/[^\d\.]//g;	# we only care about numbers and full stops
    print "$progname: $version\n";
  } elsif( $arg =~ /^-+u(sage)?$/i ||
	   $arg =~ /^-+h(elp)?$/i  ){
    &usage();
  } elsif( $arg =~ /^-+d(ebug)$/i ){
    $debug=1;
  } elsif( $arg =~ /-+im(a)?g(e)?size/i ){
    my($x,$y)=&imgsize(shift @ARGV);
    print "WIDTH=$x HEIGHT=$y\n";
  } else {
    $arg=~s/^-+//;
    if( &proc_option( $arg, shift @ARGV)){
      &SetGlobals();
    } else {
      print "Unrecognized option $arg\n";
      &usage();
      exit;
    }
  }

}

sub get_values
{
  my($i)=@_;
  return "" if !defined $i;

  if( $options[$i+1] =~ /file/i ){
    return ();
  } elsif($options[$i+1] =~ /string/i ){
    return ();
  } elsif($options[$i+1] =~ /bool/i ){
    return ('Yes','No');
  } elsif($options[$i+1] =~ /choice/i ){
    my($start,$end)=(($i+4),($options[$i+3]));
    return (@options[$start .. $start+$end-1]);
  } else {
    print "Unrecognized option type\n";
    exit 0;
  }
}

sub usage
{
  my($progname)=$0;
  $progname =~ s/.*\///;	# we only want the name
  my($vals)="";

  print "$progname: [-version] [-usage] [-option optionval] file.html ... \n";

  my($fmt)="  %15s %6s %-10s %s\n";

  printf($fmt,"Option Name","Type","Default","Values");
  printf($fmt,"-----------","----","-------","------");

  my($i,$j)=(0,0);

  while( $i < $#options ){
    $vals=join(',', &get_values($i));
    printf($fmt,$options[$i],$options[$i+1],$optionval[$j],$vals);

    $i=&NextOption($i);
    $j++;
  }
}

1;
