#!/bin/bash
version="0.005-ramadda-dzi-robust"
date="06/21/2026"

# RAMADDA-safe DZI tile generator based on MagickSlicer CLI conventions.
# Goals:
#   - preserve DZI/OpenSeadragon level numbering
#   - avoid resize-to-0 levels
#   - normalize EXIF orientation before writing DZI metadata or tiles
#   - apply -p ImageMagick options to resize and crop operations
#   - use a configurable ImageMagick temp directory

resultExt=''
resizeFilter=''
resultDir=''
imageSource=''
tileW=256
tileH=256
step=200
imageW=''
imageH=''
imOptions=''
dziFormat=true
verboseLevel=0
gravity='NorthWest'
extent=false

warnMsg(){
    if [ "$verboseLevel" -ge 1 ]; then echo "$1"; fi
}
infoMsg(){
    if [ "$verboseLevel" -ge 2 ]; then echo "$1"; fi
}
debugMsg(){
    if [ "$verboseLevel" -ge 3 ]; then echo "$1"; fi
}

uHelp(){
    echo "Usage:"
    echo "  magick-slicer.sh [options] -i source_image -o output_base"
    echo
    echo "Options:"
    echo "  -i, --in <file>              Input file"
    echo "  -o, --out <path>             Output base path. Creates <path>.dzi and <path>_files/"
    echo "  -e, --extension <ext>        Output tile extension, e.g. jpg"
    echo "  -w, --width <pixels>         Tile width. Height defaults to same value"
    echo "  -h, --height <pixels>        Tile height"
    echo "  -s, --step <value>           Zoom step. 200 means half-size levels. DZI normally uses 200"
    echo "  -p, --options '<options>'    ImageMagick options, e.g. '-limit memory 512MiB ... -quality 85'"
    echo "  -x, --extent                 Pad edge tiles to full tile size"
    echo "  -g, --gravity <type>         Crop gravity; default NorthWest"
    echo "  -v0|-v1|-v2|-v3              Verbosity"
    echo "  -?, --help                   Help"
}

cliHelp(){
    echo "Map tiles generator. License: MIT."
    echo "Version: $version"
    echo "Date: $date"
    echo
    uHelp
    exit 0
}

if [ $# -eq 0 ]; then cliHelp; fi

WnotDefined=true
HnotDefined=true
SourceNotDefined=true
ResDirNotDefined=true
ExtNotDefined=true

while [[ $# > 0 ]]; do
    key="$1"
    case $key in
        -i|--in)
            imageSource="$2"; SourceNotDefined=false; shift ;;
        -o|--out)
            resultDir="$2"; ResDirNotDefined=false; shift ;;
        -e|--extension)
            resultExt="$2"; ExtNotDefined=false; shift ;;
        -w|--width)
            tileW="$2"
            if $HnotDefined; then tileH="$2"; fi
            WnotDefined=false
            shift ;;
        -h|--height)
            tileH="$2"
            if $WnotDefined; then tileW="$2"; fi
            HnotDefined=false
            shift ;;
        -s|--step)
            step="$2"; shift ;;
        -p|--options)
            imOptions="$2"; shift ;;
        -g|--gravity)
            gravity="$2"; shift ;;
        -x|--extent)
            extent=true ;;
        -v|--verbose)
            verboseLevel="$2"; shift ;;
        -v0|--verbose0)
            verboseLevel=0 ;;
        -v1|--verbose1)
            verboseLevel=1 ;;
        -v2|--verbose2)
            verboseLevel=2 ;;
        -v3|--verbose3)
            verboseLevel=3 ;;
        -u|--usage|-\?|--help|-m|--man)
            cliHelp ;;
        *)
            if $SourceNotDefined; then
                imageSource="$1"; SourceNotDefined=false
            elif $ResDirNotDefined; then
                resultDir="$1"; ResDirNotDefined=false
            else
                echo "Unknown option: $1"
            fi ;;
    esac
    shift
done

command -v convert >/dev/null 2>&1 || { echo >&2 "I require ImageMagick tool 'convert', but it's not installed. Aborting."; exit 1; }
command -v identify >/dev/null 2>&1 || { echo >&2 "I require ImageMagick tool 'identify', but it's not installed. Aborting."; exit 1; }

if $SourceNotDefined; then echo "No source file present. Canceled."; exit 1; fi
if [ ! -f "$imageSource" ]; then echo "Error! Input file not found: $imageSource"; exit 1; fi

fullName=$(basename "$imageSource")
fileBase="${fullName%.*}"
fileExt="${fullName##*.}"
if $ExtNotDefined; then resultExt="$fileExt"; fi
if $ResDirNotDefined; then resultDir="$fileBase"; fi

if [ "$step" -le 100 ]; then
    echo "You get infinity loop. Minimum step value = 101% (101)"
    exit 1
fi

# Use one ImageMagick thread by default. This reduces memory spikes.
if [ -z "$MAGICK_THREAD_LIMIT" ]; then
    export MAGICK_THREAD_LIMIT=1
fi

baseOut="$resultDir"
dziFileName="${baseOut}.dzi"
filesDir="${baseOut}_files"
mkdir -p "$(dirname "$dziFileName")"
rm -rf "$filesDir"
mkdir -p "$filesDir"

# Use a writable temp directory on the same filesystem as the output unless caller provided one.
if [ -z "$MAGICK_TEMPORARY_PATH" ]; then
    export MAGICK_TEMPORARY_PATH="${filesDir}/.magick_tmp"
fi
export TMPDIR="$MAGICK_TEMPORARY_PATH"
mkdir -p "$MAGICK_TEMPORARY_PATH"

# Optional hard virtual-memory cap for the shell and convert children.
# Example: export MAGICK_SLICER_VMEM_KB=1500000
if [ -n "$MAGICK_SLICER_VMEM_KB" ]; then
    case "$(uname -s)" in
        Linux*)
            ulimit -v "$MAGICK_SLICER_VMEM_KB" 2>/dev/null || true
            ;;
        *)
            # macOS, BSD, etc.
            ;;
    esac
fi


normSource="${filesDir}/.source_oriented.${resultExt}"

infoMsg "PROGRESS normalize source"
# Normalize EXIF orientation before measuring or tiling. This fixes phone photos whose
# stored pixel dimensions do not match displayed orientation.
convert $imOptions "$imageSource" -auto-orient "$normSource" || exit $?

imageW=$(identify -format "%w" "$normSource") || exit $?
imageH=$(identify -format "%h" "$normSource") || exit $?

if [ -z "$imageW" ] || [ -z "$imageH" ] || [ "$imageW" -le 0 ] || [ "$imageH" -le 0 ]; then
    echo "Error! Could not determine normalized image size. imageW='$imageW' imageH='$imageH'"
    exit 1
fi

# DZI uses square tiles. Preserve original behavior.
tileH=$tileW

cat > "$dziFileName" <<DZI
<?xml version="1.0"?>
<Image TileSize="${tileW}" Overlap="0" Format="${resultExt}" xmlns="http://schemas.microsoft.com/deepzoom/2008">
<Size Width="${imageW}" Height="${imageH}"/>
</Image>
DZI

maxDim=$imageW
if [ "$imageH" -gt "$maxDim" ]; then maxDim=$imageH; fi

# DZI level count: level 0 is tiny; highest level is full resolution.
maxLevel=0
levelDim=1
while [ "$levelDim" -lt "$maxDim" ]; do
    levelDim=$(( levelDim * 2 ))
    maxLevel=$(( maxLevel + 1 ))
done

total=$(( maxLevel + 1 ))
infoMsg "PROGRESS start total=${total} width=${imageW} height=${imageH} tile=${tileW}"

for (( level=0; level<=maxLevel; level++ )); do
    divisorPower=$(( maxLevel - level ))
    divisor=1
    for (( i=0; i<divisorPower; i++ )); do
        divisor=$(( divisor * 2 ))
    done

    levelW=$(( (imageW + divisor - 1) / divisor ))
    levelH=$(( (imageH + divisor - 1) / divisor ))
    if [ "$levelW" -lt 1 ]; then levelW=1; fi
    if [ "$levelH" -lt 1 ]; then levelH=1; fi

    mkdir -p "${filesDir}/${level}"
    levelFile="${filesDir}/${level}.${resultExt}"

    infoMsg "PROGRESS resize level=${level} total=${total} width=${levelW} height=${levelH}"
    convert $imOptions "$normSource" $resizeFilter -resize "${levelW}x${levelH}!" "$levelFile" || exit $?
    infoMsg "PROGRESS resized level=${level} total=${total} width=${levelW} height=${levelH} file=${levelFile}"

    xyDelim='_'
    tilesFormat="%[fx:page.x/${tileW}]${xyDelim}%[fx:page.y/${tileH}]"
    tilePattern="${filesDir}/${level}/%[filename:tile].${resultExt}"
    ext=''
    if $extent; then ext="-background none -extent ${tileW}x${tileH}"; fi

    infoMsg "PROGRESS slice level=${level} total=${total} width=${levelW} height=${levelH}"
    convert $imOptions "$levelFile" -gravity "$gravity" -crop "${tileW}x${tileH}" -set filename:tile "$tilesFormat" +repage +adjoin -gravity "$gravity" $ext "$tilePattern" || exit $?
    infoMsg "PROGRESS sliced level=${level} total=${total} width=${levelW} height=${levelH}"

    rm -f "$levelFile"
done

rm -f "$normSource"
# Leave MAGICK_TEMPORARY_PATH in place during execution only; clean up our default cache.
# If caller supplied MAGICK_TEMPORARY_PATH, do not remove it.
# Note: if the process is killed, this may need manual cleanup.

infoMsg "PROGRESS complete total=${total} width=${imageW} height=${imageH}"
echo
