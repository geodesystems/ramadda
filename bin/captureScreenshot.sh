#!/bin/sh
ullr="452,120,850,700"
for i in {1..2}; do 
    timestamp=$(date "+%Y-%m-%d %H:%M")
    hhmmss=$(date "+%H-%M-%S")
    say capturing
    sleep 10
    echo  "capturing ${i}"
    osascript -e "activate application \"Firefox\""
    osascript -e "tell application \"System Events\" to keystroke \"r\" using command down"
    sleep 10
    screencapture -R${ullr} out.png
    osascript -e "beep 1"
    convert out.png -pointsize 20 -background White  label:"${timestamp}"  -gravity Center -append snapshot${hhmmss}.png
    sleep 5
#    sleep 900
done
say done
convert -delay 100 -loop 0 snapshot*.png animated.gif
