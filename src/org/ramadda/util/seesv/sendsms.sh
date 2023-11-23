#!/bin/sh
#This is used to send text messages from an input list of phone numbers using  Twilio (https://www.twilio.com/)
#To run this you need to have an account on Twilio and then to download and install the RAMADDA SeeSV package

#There are only 3 things you need to configure in this file - the environment variables
#TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN and TWILIO_PHONE

################################################################################################
#Setting up Twilio
################################################################################################

#Go to: https://www.twilio.com/ and create an account. Once logged in:
#Go to Account->API Keys and Tokens
#From there click on Create API key and enter your information
#Copy the SID and Secret and set them here:
export TWILIO_ACCOUNT_SID=add sid here
export TWILIO_AUTH_TOKEN=add auth token here

#You will also need a twilio phone number. I'm not sure if they always give you one or if you have to buy one
#The phone numbers are under Develop->Phone Numbers->Manage->Active Numbers
#Enter your Twilio phone number here:
export TWILIO_PHONE=add phone number here


################################################################################################
#Setting up SeeSV
################################################################################################

#The SeeSV package relies on Java version 8 or higher. To check your Java from a terminal window do:
#java -version

#Download the seesv.zip file from https://ramadda.org/repository/alias/release
#Unzip the file and you should have a directory:
#seesv
#     seesv.sh
#     ...


#When running this script it looks for the seesv dir either in the same directory as this script
#or in your home directory. You can set it here to another path
seesv="/some/directory"

#The SeeSV package has a large number of functions for processing CSV and other input
#data formats. Here we use it to send text messages but to see the full set of commands do:
#sh seesv.sh -help 
#The documentation for the command is available at:
#https://ramadda.org/repository/userguide/seesv.html#-sms



################################################################################################
#Sending text messages
################################################################################################

#
#To run this file do:
#sh sendsms.sh  <input file> <campaign> <message template>
#e.g.:
#sh sendsms.sh  test.csv campaign1 "Hi {first_name}, \nthis is a test message\nSend STOP to opt out" 

#The csv file needs to have at least a column called "phone"

#The campaign is used to write a file of sent phone numbers (e.g. campaign1.sent.txt) for a particular campaign
#If you are sending a message to a long list of numbers the program can be stopped and then restarted
#phone numbers that have already been sent to won't be resent
#You can check to see what is actually sent from your Twilio dashboard @ Monitor->Logs->Messaging

#The message can contain macros of the form {column name} which correspond to the columns in the input csv file


################################################################################################
#Don't touch anything below here
################################################################################################

input=$1
campaign=$2
message=$3

usage() {
    echo $1
    echo "Usage: sendsms.sh input.csv campaign message"
    exit
}

if [ -z "${input}" ]; then usage "No input file provided"; exit; fi
if [ -z "${campaign}" ]; then usage "No campaign provided"; exit; fi
if [ -z "${message}" ]; then usage "No message provided"; exit; fi
if [ ! -f "${input}" ]; then usage "Input $input is not a file"; exit; fi

myDir=`dirname $0`

#Find the seesv path
if [ ! -d $seesv ]; then
    seesv="$myDir/seesv"
fi

if [ ! -d $seesv ]; then
    seesv=~/seesv
    if [ ! -d $seesv ];    then	 
	seesv="./seesv"
	if [ ! -d $seesv ];	then	 
            echo "No seesv directory found in $myDir/seesv or ~/seesv"
	fi
    fi
fi

set -e

echo "calling sh $seesv/seesv.sh -sms phone ${campaign} ${message} ${input}"
sh $seesv/seesv.sh -progress 100 -sms phone "${campaign}" "${message}" ${input}
