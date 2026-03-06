// simple_rect_tiler_optimized.js
class CoreTiler {
  /**
   * @param {object} [opts]
   * @param {number} [opts.minX=0]
   * @param {number|null} [opts.maxX=null]
   * @param {number} [opts.padX=0]
   * @param {number} [opts.padY=0]
   * @param {object} [opts.snap]
   * @param {boolean} [opts.snap.enabled=false]
   * @param {number} [opts.snap.step=1]
   */
  constructor(opts = {}) {
    this.minX = Number.isFinite(opts.minX) ? opts.minX : 0;
    this.maxX = (opts.maxX === undefined) ? null : opts.maxX;

    this.padX = Number.isFinite(opts.padX) ? opts.padX : 0;
    this.padY = Number.isFinite(opts.padY) ? opts.padY : 0;

    const snap = opts.snap || {};
    this.snapEnabled = !!snap.enabled;
    this.snapStep = Number.isFinite(snap.step) && snap.step > 0 ? snap.step : 1;

    /** all placed rectangles */
    this.rects = [];

    /** same objects as rects but sorted by y */
    this.rectsByY = [];
  }

  reset() {
    this.rects.length = 0;
    this.rectsByY.length = 0;
  }

  _snap(x) {
    if (!this.snapEnabled) return x;
    return Math.round(x / this.snapStep) * this.snapStep;
  }

  _overlaps(a, b) {
    const bx0 = b.x;
    const by0 = b.y;
    const bx1 = b.x + b.w + this.padX;
    const by1 = b._y2;

    const ax0 = a.x;
    const ay0 = a.y;
    const ax1 = a.x + a.w;
    const ay1 = a.y + a.h;

    return ax0 < bx1 && ax1 > bx0 && ay0 < by1 && ay1 > by0;
  }

  _lowerBoundY(y) {
    let lo = 0, hi = this.rectsByY.length;
    while (lo < hi) {
      const mid = (lo + hi) >> 1;
      if (this.rectsByY[mid].y < y) lo = mid + 1;
      else hi = mid;
    }
    return lo;
  }

  _insertByY(rect) {
    const idx = this._lowerBoundY(rect.y);
    this.rectsByY.splice(idx, 0, rect);
  }

  _getYCandidates(y, h) {
    const y0 = y;
    const y1 = y + h;

    const arr = this.rectsByY;
    let i = this._lowerBoundY(y0);

    let start = i;
    while (start > 0 && arr[start - 1]._y2 > y0) start--;

    const out = [];
    const yStop = y1 + this.padY;

    for (let j = start; j < arr.length; j++) {
      const r = arr[j];
      if (r.y >= yStop) break;
      if (r._y2 > y0) out.push(r);
    }

    return out;
  }

  place(r) {
    const y = r.y;
    const h = r.h;
    const w = r.w;

    const yCandidates = this._getYCandidates(y, h);

    let x = this._snap(this.minX);

    for (let iter = 0; iter < 100000; iter++) {
      const candidate = { x, y, w, h };

      let blocker = null;

      for (const existing of yCandidates) {
        if (this._overlaps(candidate, existing)) {
          blocker = existing;
          break;
        }
      }

      if (!blocker) {
        const placed = { x, y, w, h, id: r.id, _y2: y + h + this.padY };
        this.rects.push(placed);
        this._insertByY(placed);
        return placed;
      }

      x = blocker.x + blocker.w + this.padX;
      x = this._snap(x);

      if (x < this.minX) x = this.minX;
      if (this.maxX != null && x + w > this.maxX) return null;
    }

    throw new Error("SimpleRectTilerOptimized: iteration limit exceeded");
  }
}
