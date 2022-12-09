#
set -e
java  -Djava.awt.headless=true org.ramadda.util.text.Seesv -header "red,green,blue" -delimiter _spaces_ \
      -start "^ *[0-9]+.*" \
      -columns 0,1,2 \
      -if -find 0 "\." -scale 0,1,2 0 255 0 -decimals 0,1,2 0 -endif \
      -multifiles "processed/\${file_shortname}.csv" \
      -template "\${file_shortname}:{colors:[" "'rgb(\${0},\${1},\${2})'" "," "]},\n" \
      *.rgb 
