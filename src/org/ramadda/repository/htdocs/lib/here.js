/*
 * Copyright (C) 2019 HERE Europe B.V.
 * Licensed under MIT, see full license in LICENSE
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */
/*
Originally from https://github.com/heremaps/flexible-polyline/blob/master/javascript/index.js
changed to not be a module
*/
var DEFAULT_PRECISION = 5;

var ENCODING_TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

var DECODING_TABLE = [
    62, -1, -1, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1,
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
    22, 23, 24, 25, -1, -1, -1, -1, 63, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35,
    36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51
];

var FORMAT_VERSION = 1;
var ABSENT = 0;
var LEVEL = 1;
var ALTITUDE = 2;
var ELEVATION = 3;
// Reserved values 4 and 5 should not be selectable
var CUSTOM1 = 6;
var CUSTOM2 = 7;
var Num = typeof BigInt !== "undefined" ? BigInt : Number;

function hereDecode(encoded) {
    const decoder = decodeUnsignedValues(encoded);
    const header = decodeHeader(decoder[0], decoder[1]);

    const factorDegree = 10 ** header.precision;
    const factorZ = 10 ** header.thirdDimPrecision;
    const { thirdDim } = header;

    let lastLat = 0;
    let lastLng = 0;
    let lastZ = 0;
    const res = [];

    let i = 2;
    for (;i < decoder.length;) {
        const deltaLat = toSigned(decoder[i]) / factorDegree;
        const deltaLng = toSigned(decoder[i + 1]) / factorDegree;
        lastLat += deltaLat;
        lastLng += deltaLng;

        if (thirdDim) {
            const deltaZ = toSigned(decoder[i + 2]) / factorZ;
            lastZ += deltaZ;
            res.push([lastLat, lastLng, lastZ]);
            i += 3;
        } else {
            res.push([lastLat, lastLng]);
            i += 2;
        }
    }

    if (i !== decoder.length) {
        throw new Error('Invalid encoding. Premature ending reached');
    }

    return {
        ...header,
        polyline: res,
    };
}

function decodeChar(char) {
    const charCode = char.charCodeAt(0);
    return DECODING_TABLE[charCode - 45];
}

function decodeUnsignedValues(encoded) {
    let result = Num(0);
    let shift = Num(0);
    const resList = [];

    encoded.split('').forEach((char) => {
        const value = Num(decodeChar(char));
        result |= (value & Num(0x1F)) << shift;
        if ((value & Num(0x20)) === Num(0)) {
            resList.push(result);
            result = Num(0);
            shift = Num(0);
        } else {
            shift += Num(5);
        }
    });

    if (shift > 0) {
        throw new Error('Invalid encoding');
    }

    return resList;
}

function decodeHeader(version, encodedHeader) {
    if (+version.toString() !== FORMAT_VERSION) {
        throw new Error('Invalid format version');
    }
    const headerNumber = +encodedHeader.toString();
    const precision = headerNumber & 15;
    const thirdDim = (headerNumber >> 4) & 7;
    const thirdDimPrecision = (headerNumber >> 7) & 15;
    return { precision, thirdDim, thirdDimPrecision };
}

function toSigned(val) {
    // Decode the sign from an unsigned value
    let res = val;
    if (res & Num(1)) {
        res = ~res;
    }
    res >>= Num(1);
    return +res.toString();
}

function hereEncode({ precision = DEFAULT_PRECISION, thirdDim = ABSENT, thirdDimPrecision = 0, polyline }) {
    // Encode a sequence of lat,lng or lat,lng(,{third_dim}). Note that values should be of type BigNumber
    //   `precision`: how many decimal digits of precision to store the latitude and longitude.
    //   `third_dim`: type of the third dimension if present in the input.
    //   `third_dim_precision`: how many decimal digits of precision to store the third dimension.

    const multiplierDegree = 10 ** precision;
    const multiplierZ = 10 ** thirdDimPrecision;
    const encodedHeaderList = encodeHeader(precision, thirdDim, thirdDimPrecision);
    const encodedCoords = [];

    let lastLat = Num(0);
    let lastLng = Num(0);
    let lastZ = Num(0);
    polyline.forEach((location) => {
       const lat = Num(Math.round(location[0] * multiplierDegree));
       encodedCoords.push(encodeScaledValue(lat - lastLat));
       lastLat = lat;

       const lng = Num(Math.round(location[1] * multiplierDegree));
       encodedCoords.push(encodeScaledValue(lng - lastLng));
       lastLng = lng;

       if (thirdDim) {
           const z = Num(Math.round(location[2] * multiplierZ));
           encodedCoords.push(encodeScaledValue(z - lastZ));
           lastZ = z;
       }
    });

    return [...encodedHeaderList, ...encodedCoords].join('');
}

function encodeHeader(precision, thirdDim, thirdDimPrecision) {
    // Encode the `precision`, `third_dim` and `third_dim_precision` into one encoded char
    if (precision < 0 || precision > 15) {
        throw new Error('precision out of range. Should be between 0 and 15');
    }
    if (thirdDimPrecision < 0 || thirdDimPrecision > 15) {
        throw new Error('thirdDimPrecision out of range. Should be between 0 and 15');
    }
    if (thirdDim < 0 || thirdDim > 7 || thirdDim === 4 || thirdDim === 5) {
        throw new Error('thirdDim should be between 0, 1, 2, 3, 6 or 7');
    }

    const res = (thirdDimPrecision << 7) | (thirdDim << 4) | precision;
    return encodeUnsignedNumber(FORMAT_VERSION) + encodeUnsignedNumber(res);
}

function encodeUnsignedNumber(val) {
    // Uses variable integer encoding to encode an unsigned integer. Returns the encoded string.
    let res = '';
    let numVal = Num(val);
    while (numVal > 0x1F) {
        const pos = (numVal & Num(0x1F)) | Num(0x20);
        res += ENCODING_TABLE[pos];
        numVal >>= Num(5);
    }
    return res + ENCODING_TABLE[numVal];
}

function encodeScaledValue(value) {
    // Transform a integer `value` into a variable length sequence of characters.
    //   `appender` is a callable where the produced chars will land to
    let numVal = Num(value);
    const negative = numVal < 0;
    numVal <<= Num(1);
    if (negative) {
        numVal = ~numVal;
    }

    return encodeUnsignedNumber(numVal);
}

/*
module.exports = {
    encode,
    decode,
    ABSENT,
    LEVEL,
    ALTITUDE,
    ELEVATION,
};

*/

//Taken from https://github.com/googlemaps/js-polyline-codec/blob/main/src/index.ts

function googleDecode(encodedPath,precision) {
    if(!Utils.isDefined(precision)) precision=5;
    const factor = Math.pow(10, precision);
    const len = encodedPath.length;
    // For speed we preallocate to an upper bound on the final length, then
    // truncate the array before returning.
    const path = new Array(Math.floor(encodedPath.length / 2));
    let index = 0;
    let lat = 0;
    let lng = 0;
    let pointIndex = 0;

    // This code has been profiled and optimized, so don't modify it without
    // measuring its performance.
    for (; index < len; ++pointIndex) {
	// Fully unrolling the following loops speeds things up about 5%.
	let result = 1;
	let shift = 0;
	let b;
	do {
	    // Invariant: "result" is current partial result plus (1 << shift).
	    // The following line effectively clears this bit by decrementing "b".
	    b = encodedPath.charCodeAt(index++) - 63 - 1;
	    result += b << shift;
	    shift += 5;
	} while (b >= 0x1f); // See note above.
	lat += result & 1 ? ~(result >> 1) : result >> 1;
	result = 1;
	shift = 0;
	do {
	    b = encodedPath.charCodeAt(index++) - 63 - 1;
	    result += b << shift;
	    shift += 5;
	} while (b >= 0x1f);
	lng += result & 1 ? ~(result >> 1) : result >> 1;
	path[pointIndex] = [lat / factor, lng / factor];
    }
    // truncate array
    path.length = pointIndex;
    return path;
}


