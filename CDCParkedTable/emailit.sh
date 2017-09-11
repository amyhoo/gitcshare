#!/bin/ksh


#
#  This script takes two ascii files and mails them.  One file is
#  the main body of the message.  The second file is sent as an ascii
#  mime attachment.  But everything is kept in ascii so that users with
#  non-mime mail user agents can deal with it
#
#
#  Options 
#
#  -t addressee  (required)
#  -a address    (required)
#  -b body       (required)
#  -s subject    (optional)
#  -A attachment (optional)
#
#  mimetool -t "Joe Blow" -a jblow@abc.com  -b body.txt  -A attach.txt
#
#  Here body.txt and attach.txt are files.  The name of the body file
#  isn't too important.  The name of the attachment file is important 
#  since it will be sent as well as the contents.  A Microsoft OS uses
#  the name to figure out what to do.  It knows what a .txt file is but
#  it will get mixed up with a .junk file.  

((error=0))
while getopts ':t:a:b:A:s:' opt ; do
	case $opt in
	t)
		TO=$OPTARG
		;;
	a)
		ADDRESS=$OPTARG
		;;
	b)
		BODY=$OPTARG
		;;
	A)
		ATTACHMENT=$OPTARG
		;;
	s)
		SUBJECT=$OPTARG
		;;
	\?)
		print -u2 what is -${OPTARG}?
		((error=error+1))
		;;
	:)
		print -u2 $OPTARG need an argument
		((error=error+1))
		;;
	esac
done

if [[ -z $TO ]] ; then
	print -u2 "-t NAME is required"
	((error=error+1))
fi

if [[ -z $ADDRESS ]] ; then
	print -u2 "-a ADDRESS is required"
	((error=error+1))
fi

if [[ -z $BODY ]] ; then
	print -u2 "-b BODY is required"
	((error=error+1))
fi

if [[ ! -f $BODY ||  ! -r $BODY ]] ; then
	print -u2 "-b $BODY is not a readable file"
	((error=error+1))
fi

if [[ -n $ATTACHMENT ]] ; then
	if [[ ! -f $ATTACHMENT ||  ! -r $ATTACHMENT ]] ; then
	    print -u2 "-b $ATTACHMENT is not a readable file"
	    ((error=error+1))
    fi
fi

if ((error)) ; then
	print -u2 "error in parameter list...exiting"
	exit 1
fi


myname=`whoami`
myaddr=`hostname`
myaddr="do_not_reply@"${myaddr}



BOUNDARY='=== This is the boundary between parts of the message. ==='

{
print -  "From: $myname <${myaddr}>"
print -  "To: $TO <${ADDRESS}>"
if [[ -n $SUBJECT ]] ; then
	print -  'Subject:' $SUBJECT
fi
print -  'MIME-Version: 1.0'
print -  'Content-Type: MULTIPART/MIXED; '
print -  '    BOUNDARY='\"$BOUNDARY\"
print - 
print -  '        This message is in MIME format.  But if you can see this,'
print -  "        you aren't using a MIME aware mail program.  You shouldn't "
print -  '        have too many problems because this message is entirely in'
print -  '        ASCII and is designed to be somewhat readable with old '
print -  '        mail software.'
print - 
print -  "--${BOUNDARY}"
print -  'Content-Type: TEXT/HTML; charset=US-ASCII'
print - 
cat $BODY
print - 
#print - 
#print -  "--${BOUNDARY}"
#print -  'Content-Type: TEXT/PLAIN; charset=US-ASCII; name='${ATTACHMENT}
#print -  'Content-Disposition: attachment;   filename='${ATTACHMENT}
#print - 
#cat $ATTACHMENT
#print - 
print -  "--${BOUNDARY}--"
} | /usr/lib/sendmail $ADDRESS

exit 0
