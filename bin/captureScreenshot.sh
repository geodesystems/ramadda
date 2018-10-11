#!/bin/sh
#This script runs on a Mac and captures a screenshot of a Firefox browser window every 15 minutes. It also uses Imagemagick's convert to do the image conversions
#It captures 12 time steps

#The screen bounds to capture. Do a command-shift-4 on the mac and drag out the
#rectangle you want to capture to see the coordinates
ullr="452,120,850,700"

for i in {1..12}; do 
    timestamp=$(date "+%Y-%m-%d %H:%M")
    hhmmss=$(date "+%H-%M-%S")
#say "capturing" and sleep for 10 seconds to give user time to bring up the firefox window
    say capturing
    sleep 10
    echo  "capturing ${i}"
#Bring Firefox to the front and tell it to reload the main page
    osascript -e "activate application \"Firefox\""
    osascript -e "tell application \"System Events\" to keystroke \"r\" using command down"
#Wait for 10 seconds to have the page reload
    sleep 10
    screencapture -R${ullr} out.png
    osascript -e "beep 1"
#Add the timestamp to the bottom
    convert out.png -pointsize 20 -background White  label:"${timestamp}"  -gravity Center -append snapshot${hhmmss}.png
    sleep 900
done
say done
#Make the animated GIF
convert -delay 100 -loop 0 snapshot*.png animated.gif
