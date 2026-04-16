/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


class Stats {
    constructor(values) {
	this.values = Array.isArray(values) ? [...values] : [];
	this.count = this.values.length;

	this.sorted = [...this.values].sort((a, b) => a - b);
	this.uniqueSorted = [...new Set(this.sorted)];

	this.sum = Stats.sum(this.values);
	this.min = this.count ? this.sorted[0] : NaN;
	this.max = this.count ? this.sorted[this.count - 1] : NaN;
	this.range = this.count ? this.max - this.min : NaN;

	this.mean = Stats.mean(this.values);
	this.variance = Stats.variance(this.values, this.mean);
	this.sampleVariance = Stats.sampleVariance(this.values, this.mean);
	this.standardDeviation = Number.isNaN(this.variance) ? NaN : Math.sqrt(this.variance);
	this.sampleStandardDeviation = Number.isNaN(this.sampleVariance) ? NaN : Math.sqrt(this.sampleVariance);

	this.medianValue = Stats.median(this.sorted);
	this.q1Value = Stats.quantile(this.sorted, 0.25);
	this.q3Value = Stats.quantile(this.sorted, 0.75);
	this.iqrValue = this.q3Value - this.q1Value;

	this.p5Value = Stats.quantile(this.sorted, 0.05);
	this.p95Value = Stats.quantile(this.sorted, 0.95);

	this.skewnessValue = Stats.skewness(this.values, this.mean, this.standardDeviation);
	this.madValue = Stats.madFromSorted(this.sorted, this.medianValue);
    }

    static sum(values) {
	let total = 0;
	for (const v of values) total += v;
	return total;
    }

    static mean(values) {
	if (!values.length) return NaN;
	return Stats.sum(values) / values.length;
    }

    static variance(values, mean = Stats.mean(values)) {
	if (!values.length) return NaN;
	let sum = 0;
	for (const v of values) {
	    const d = v - mean;
	    sum += d * d;
	}
	return sum / values.length;
    }

    static sampleVariance(values, mean = Stats.mean(values)) {
	if (values.length < 2) return NaN;
	let sum = 0;
	for (const v of values) {
	    const d = v - mean;
	    sum += d * d;
	}
	return sum / (values.length - 1);
    }

    static median(sorted) {
	const n = sorted.length;
	if (!n) return NaN;
	const mid = Math.floor(n / 2);
	return n % 2 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2;
    }

    static quantile(sorted, q) {
	if (!sorted.length) return NaN;
	if (q <= 0) return sorted[0];
	if (q >= 1) return sorted[sorted.length - 1];

	const pos = (sorted.length - 1) * q;
	const base = Math.floor(pos);
	const rest = pos - base;

	if (base + 1 < sorted.length) {
	    return sorted[base] + rest * (sorted[base + 1] - sorted[base]);
	}
	return sorted[base];
    }

    static binarySearch(sorted, value) {
	let low = 0, high = sorted.length;
	while (low < high) {
	    const mid = (low + high) >> 1;
	    if (sorted[mid] < value) low = mid + 1;
	    else high = mid;
	}
	return low;
    }

    static upperBound(sorted, value) {
	let low = 0, high = sorted.length;
	while (low < high) {
	    const mid = (low + high) >> 1;
	    if (sorted[mid] <= value) low = mid + 1;
	    else high = mid;
	}
	return low;
    }

    static percentile(sorted, value) {
	if (!sorted.length) return NaN;
	if (sorted.length === 1) return 0;

	const first = Stats.binarySearch(sorted, value);
	const last = Stats.upperBound(sorted, value) - 1;

	let rank;
	if (first >= sorted.length) {
	    rank = sorted.length - 1;
	} else if (last < first) {
	    rank = first;
	} else {
	    rank = (first + last) / 2;
	}

	rank = Math.max(0, Math.min(rank, sorted.length - 1));
	return rank / (sorted.length - 1);
    }

    static normalize(value, min, max, clamp = false) {
	if (max === min) return 0;
	let p = (value - min) / (max - min);
	return clamp ? Stats.clamp(p) : p;
    }

    static clamp(value, min = 0, max = 1) {
	return Math.max(min, Math.min(max, value));
    }

    static zscore(value, mean, stddev) {
	if (!stddev) return 0;
	return (value - mean) / stddev;
    }

    static skewness(values, mean, std) {
	if (!values.length || !std) return 0;
	let sum = 0;
	for (const v of values) {
	    sum += Math.pow((v - mean) / std, 3);
	}
	return sum / values.length;
    }

    static madFromSorted(sorted, median) {
	if (!sorted.length) return NaN;
	const deviations = sorted.map(v => Math.abs(v - median)).sort((a, b) => a - b);
	return Stats.median(deviations);
    }

    static histogram(values, bins = 50) {
	if (!values.length) return { counts: [], min: NaN, max: NaN, width: NaN };

	let min = values[0], max = values[0];
	for (const v of values) {
	    if (v < min) min = v;
	    if (v > max) max = v;
	}

	if (min === max) {
	    return { counts: [values.length], min, max, width: 0 };
	}

	const counts = new Array(bins).fill(0);
	const width = (max - min) / bins;

	for (const v of values) {
	    let idx = Math.floor((v - min) / width);
	    if (idx >= bins) idx = bins - 1;
	    if (idx < 0) idx = 0;
	    counts[idx]++;
	}

	return { counts, min, max, width };
    }

    // -------- instance methods --------

    percentile(value) {
	return Stats.percentile(this.sorted, value);
    }

    quantile(q) {
	return Stats.quantile(this.sorted, q);
    }

    normalize(value, clamp = false) {
	return Stats.normalize(value, this.min, this.max, clamp);
    }

    zscore(value) {
	return Stats.zscore(value, this.mean, this.standardDeviation);
    }

    histogram(bins = 50) {
	return Stats.histogram(this.values, bins);
    }

    summary() {
	return {
	    count: this.count,
	    min: this.min,
	    max: this.max,
	    mean: this.mean,
	    median: this.medianValue,
	    stddev: this.standardDeviation,
	    q1: this.q1Value,
	    q3: this.q3Value,
	    iqr: this.iqrValue,
	    p5: this.p5Value,
	    p95: this.p95Value,
	    skewness: this.skewnessValue,
	    mad: this.madValue
	};
    }
}

var MAPPER_METHOD = {
    RAW:"raw",
    LINEAR: "linear",
    LOG: "log",
    PERCENTILE: "percentile",
    ORDER: "order",       // same as percentile but using unique values
    ZSCORE: "zscore",
    GAMMA: "gamma",
    CLIPPED: "clipped",
    DIVERGING: "diverging"
};

var MAPPER_METHOD_ALL= Object.values(MAPPER_METHOD).join("|");




class Mapper {
    constructor(stats, options = {}) {
	this.stats = stats;

	this.method = options.method || MAPPER_METHOD.LINEAR;
	this.clampOutput = options.clamp !== false;

	this.gamma = options.gamma ?? 1;
	this.epsilon = options.epsilon ?? 1e-9;

	this.clipMin = options.clipMin ?? stats.p5Value ?? stats.min;
	this.clipMax = options.clipMax ?? stats.p95Value ?? stats.max;

	this.center = options.center ?? 0;
    }

    map(value) {
	let p;

	switch (this.method) {
	case MAPPER_METHOD.LINEAR:
	    p = this.linear(value);
	    break;

	case MAPPER_METHOD.LOG:
	    p = this.log(value);
	    break;

	case MAPPER_METHOD.PERCENTILE:
	    p = this.percentile(value);
	    break;

	case MAPPER_METHOD.ORDER:
	    p = this.order(value);
	    break;

	case MAPPER_METHOD.GAMMA:
	    p = this.gammaMap(value);
	    break;

	case MAPPER_METHOD.CLIPPED:
	    p = this.clipped(value);
	    break;

	case MAPPER_METHOD.ZSCORE:
	    p = this.zscoreMap(value);
	    break;

	case MAPPER_METHOD.DIVERGING:
	    p = this.diverging(value);
	    break;

	default:
	    p = this.linear(value);
	    break;
	}

	return this.clampOutput ? Mapper.clamp(p) : p;
    }

    invert(percent) {
	const p = Mapper.clamp(percent);

	switch (this.method) {
	case MAPPER_METHOD.LINEAR:
	    return this.invertLinear(p);

	case MAPPER_METHOD.LOG:
	    return this.invertLog(p);

	case MAPPER_METHOD.PERCENTILE:
	    return this.invertPercentile(p);

	case MAPPER_METHOD.ORDER:
	    return this.invertOrder(p);

	case MAPPER_METHOD.GAMMA:
	    return this.invertGamma(p);

	case MAPPER_METHOD.CLIPPED:
	    return this.invertClipped(p);

	case MAPPER_METHOD.ZSCORE:
	    return this.invertZScore(p);

	case MAPPER_METHOD.DIVERGING:
	    return this.invertDiverging(p);

	default:
	    return this.invertLinear(p);
	}
    }

    linear(value) {
	return Stats.normalize(value, this.stats.min, this.stats.max, false);
    }

    invertLinear(p) {
	return this.stats.min + p * (this.stats.max - this.stats.min);
    }

    log(value) {
	return this._logTransform(value);
    }

    invertLog(p) {
	const min = this.stats.min;
	const max = this.stats.max;

	let offset = 0;
	if (min <= 0) {
	    offset = Math.abs(min) + this.epsilon;
	}

	const lmin = Math.log(min + offset);
	const lmax = Math.log(max + offset);
	const lv = lmin + p * (lmax - lmin);

	return Math.exp(lv) - offset;
    }

    _logTransform(value) {
	const min = this.stats.min;
	const max = this.stats.max;

	let offset = 0;
	if (min <= 0) {
	    offset = Math.abs(min) + this.epsilon;
	}

	const lv = Math.log(value + offset);
	const lmin = Math.log(min + offset);
	const lmax = Math.log(max + offset);

	return Stats.normalize(lv, lmin, lmax, false);
    }

    percentile(value) {
	return this.stats.percentile(value);
    }

    invertPercentile(p) {
	const sorted = this.stats.sorted;
	if (!sorted.length) return NaN;
	if (sorted.length === 1) return sorted[0];

	const idx = p * (sorted.length - 1);
	const i0 = Math.floor(idx);
	const i1 = Math.min(i0 + 1, sorted.length - 1);
	const t = idx - i0;

	return sorted[i0] + t * (sorted[i1] - sorted[i0]);
    }

    order(value) {
	const arr = this.stats.uniqueSorted;
	if (!arr.length) return NaN;
	if (arr.length === 1) return 0;

	let idx = Stats.binarySearch(arr, value);
	if (idx >= arr.length) idx = arr.length - 1;

	return idx / (arr.length - 1);
    }

    invertOrder(p) {
	const arr = this.stats.uniqueSorted;
	if (!arr.length) return NaN;
	if (arr.length === 1) return arr[0];

	const idx = Math.round(p * (arr.length - 1));
	return arr[idx];
    }

    gammaMap(value) {
	const p = this.linear(value);
	return Math.pow(Math.max(0, p), this.gamma);
    }

    invertGamma(p) {
	if (this.gamma === 0) return this.stats.min;
	const base = Math.pow(p, 1 / this.gamma);
	return this.invertLinear(base);
    }

    clipped(value) {
	const v = Mapper.clamp(value, this.clipMin, this.clipMax);
	return Stats.normalize(v, this.clipMin, this.clipMax, false);
    }

    invertClipped(p) {
	return this.clipMin + p * (this.clipMax - this.clipMin);
    }

    zscoreMap(value) {
	const z = this.stats.zscore(value);
	return 0.5 * (1 + Math.tanh(z));
    }

    invertZScore(p) {
	const x = 2 * p - 1;
	const z = Mapper.atanh(x);
	return this.stats.mean + z * this.stats.standardDeviation;
    }

    diverging(value) {
	if (value <= this.center) {
	    if (this.center === this.stats.min) return 0.5;
	    return 0.5 * Stats.normalize(value, this.stats.min, this.center, false);
	}

	if (this.center === this.stats.max) return 0.5;
	return 0.5 + 0.5 * Stats.normalize(value, this.center, this.stats.max, false);
    }

    invertDiverging(p) {
	if (p <= 0.5) {
	    if (this.center === this.stats.min) return this.center;
	    const t = p / 0.5;
	    return this.stats.min + t * (this.center - this.stats.min);
	}

	if (this.center === this.stats.max) return this.center;
	const t = (p - 0.5) / 0.5;
	return this.center + t * (this.stats.max - this.center);
    }

    mapInfo(value) {
	return {
	    value,
	    percent: this.map(value),
	    method: this.method
	};
    }

    invertInfo(percent) {
	const p = Mapper.clamp(percent);
	return {
	    percent: p,
	    value: this.invert(p),
	    method: this.method
	};
    }

    static clamp(value, min = 0, max = 1) {
	return Math.max(min, Math.min(max, value));
    }

    static atanh(x) {
	if (Math.atanh) return Math.atanh(x);
	return 0.5 * Math.log((1 + x) / (1 - x));
    }
}

