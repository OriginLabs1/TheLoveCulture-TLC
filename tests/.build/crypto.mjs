// node_modules/@noble/hashes/esm/_assert.js
/**
* Internal assertion helpers.
* @module
*/
/** Asserts something is positive integer. */
function anumber$1(n) {
	if (!Number.isSafeInteger(n) || n < 0) throw new Error("positive integer expected, got " + n);
}
/** Is number an Uint8Array? Copied from utils for perf. */
function isBytes$2(a) {
	return a instanceof Uint8Array || ArrayBuffer.isView(a) && a.constructor.name === "Uint8Array";
}
/** Asserts something is Uint8Array. */
function abytes$1(b, ...lengths) {
	if (!isBytes$2(b)) throw new Error("Uint8Array expected");
	if (lengths.length > 0 && !lengths.includes(b.length)) throw new Error("Uint8Array expected of length " + lengths + ", got length=" + b.length);
}
/** Asserts something is hash */
function ahash(h) {
	if (typeof h !== "function" || typeof h.create !== "function") throw new Error("Hash should be wrapped by utils.wrapConstructor");
	anumber$1(h.outputLen);
	anumber$1(h.blockLen);
}
/** Asserts a hash instance has not been destroyed / finished */
function aexists(instance, checkFinished = true) {
	if (instance.destroyed) throw new Error("Hash instance has been destroyed");
	if (checkFinished && instance.finished) throw new Error("Hash#digest() has already been called");
}
/** Asserts output is properly-sized byte array */
function aoutput(out, instance) {
	abytes$1(out);
	const min = instance.outputLen;
	if (out.length < min) throw new Error("digestInto() expects output buffer of length at least " + min);
}
// node_modules/@noble/hashes/esm/crypto.js
const crypto$1 = typeof globalThis === "object" && "crypto" in globalThis ? globalThis.crypto : void 0;
// node_modules/@noble/hashes/esm/utils.js
/**
* Utilities for hex, bytes, CSPRNG.
* @module
*/
/*! noble-hashes - MIT License (c) 2022 Paul Miller (paulmillr.com) */
function createView(arr) {
	return new DataView(arr.buffer, arr.byteOffset, arr.byteLength);
}
/** The rotate right (circular right shift) operation for uint32 */
function rotr(word, shift) {
	return word << 32 - shift | word >>> shift;
}
/**
* Convert JS string to byte array.
* @example utf8ToBytes('abc') // new Uint8Array([97, 98, 99])
*/
function utf8ToBytes(str) {
	if (typeof str !== "string") throw new Error("utf8ToBytes expected string, got " + typeof str);
	return new Uint8Array(new TextEncoder().encode(str));
}
/**
* Normalizes (non-hex) string or Uint8Array to Uint8Array.
* Warning: when Uint8Array is passed, it would NOT get copied.
* Keep in mind for future mutable operations.
*/
function toBytes$1(data) {
	if (typeof data === "string") data = utf8ToBytes(data);
	abytes$1(data);
	return data;
}
/** For runtime check if class implements interface */
var Hash = class {
	clone() {
		return this._cloneInto();
	}
};
/** Wraps hash function, creating an interface on top of it */
function wrapConstructor(hashCons) {
	const hashC = (msg) => hashCons().update(toBytes$1(msg)).digest();
	const tmp = hashCons();
	hashC.outputLen = tmp.outputLen;
	hashC.blockLen = tmp.blockLen;
	hashC.create = () => hashCons();
	return hashC;
}
/** Cryptographically secure PRNG. Uses internal OS-level `crypto.getRandomValues`. */
function randomBytes(bytesLength = 32) {
	if (crypto$1 && typeof crypto$1.getRandomValues === "function") return crypto$1.getRandomValues(new Uint8Array(bytesLength));
	if (crypto$1 && typeof crypto$1.randomBytes === "function") return crypto$1.randomBytes(bytesLength);
	throw new Error("crypto.getRandomValues must be defined");
}
// node_modules/@noble/hashes/esm/_md.js
/**
* Internal Merkle-Damgard hash utils.
* @module
*/
/** Polyfill for Safari 14. https://caniuse.com/mdn-javascript_builtins_dataview_setbiguint64 */
function setBigUint64(view, byteOffset, value, isLE) {
	if (typeof view.setBigUint64 === "function") return view.setBigUint64(byteOffset, value, isLE);
	const _32n = BigInt(32);
	const _u32_max = BigInt(4294967295);
	const wh = Number(value >> _32n & _u32_max);
	const wl = Number(value & _u32_max);
	const h = isLE ? 4 : 0;
	const l = isLE ? 0 : 4;
	view.setUint32(byteOffset + h, wh, isLE);
	view.setUint32(byteOffset + l, wl, isLE);
}
/** Choice: a ? b : c */
function Chi(a, b, c) {
	return a & b ^ ~a & c;
}
/** Majority function, true if any two inputs is true. */
function Maj(a, b, c) {
	return a & b ^ a & c ^ b & c;
}
/**
* Merkle-Damgard hash construction base class.
* Could be used to create MD5, RIPEMD, SHA1, SHA2.
*/
var HashMD = class extends Hash {
	constructor(blockLen, outputLen, padOffset, isLE) {
		super();
		this.blockLen = blockLen;
		this.outputLen = outputLen;
		this.padOffset = padOffset;
		this.isLE = isLE;
		this.finished = false;
		this.length = 0;
		this.pos = 0;
		this.destroyed = false;
		this.buffer = new Uint8Array(blockLen);
		this.view = createView(this.buffer);
	}
	update(data) {
		aexists(this);
		const { view, buffer, blockLen } = this;
		data = toBytes$1(data);
		const len = data.length;
		for (let pos = 0; pos < len;) {
			const take = Math.min(blockLen - this.pos, len - pos);
			if (take === blockLen) {
				const dataView = createView(data);
				for (; blockLen <= len - pos; pos += blockLen) this.process(dataView, pos);
				continue;
			}
			buffer.set(data.subarray(pos, pos + take), this.pos);
			this.pos += take;
			pos += take;
			if (this.pos === blockLen) {
				this.process(view, 0);
				this.pos = 0;
			}
		}
		this.length += data.length;
		this.roundClean();
		return this;
	}
	digestInto(out) {
		aexists(this);
		aoutput(out, this);
		this.finished = true;
		const { buffer, view, blockLen, isLE } = this;
		let { pos } = this;
		buffer[pos++] = 128;
		this.buffer.subarray(pos).fill(0);
		if (this.padOffset > blockLen - pos) {
			this.process(view, 0);
			pos = 0;
		}
		for (let i = pos; i < blockLen; i++) buffer[i] = 0;
		setBigUint64(view, blockLen - 8, BigInt(this.length * 8), isLE);
		this.process(view, 0);
		const oview = createView(out);
		const len = this.outputLen;
		if (len % 4) throw new Error("_sha2: outputLen should be aligned to 32bit");
		const outLen = len / 4;
		const state = this.get();
		if (outLen > state.length) throw new Error("_sha2: outputLen bigger than state");
		for (let i = 0; i < outLen; i++) oview.setUint32(4 * i, state[i], isLE);
	}
	digest() {
		const { buffer, outputLen } = this;
		this.digestInto(buffer);
		const res = buffer.slice(0, outputLen);
		this.destroy();
		return res;
	}
	_cloneInto(to) {
		to || (to = new this.constructor());
		to.set(...this.get());
		const { blockLen, buffer, length, finished, destroyed, pos } = this;
		to.length = length;
		to.pos = pos;
		to.finished = finished;
		to.destroyed = destroyed;
		if (length % blockLen) to.buffer.set(buffer);
		return to;
	}
};
// node_modules/@noble/hashes/esm/_u64.js
/**
* Internal helpers for u64. BigUint64Array is too slow as per 2025, so we implement it using Uint32Array.
* @todo re-check https://issues.chromium.org/issues/42212588
* @module
*/
const U32_MASK64 = /* @__PURE__ */ BigInt(2 ** 32 - 1);
const _32n = /* @__PURE__ */ BigInt(32);
function fromBig(n, le = false) {
	if (le) return {
		h: Number(n & U32_MASK64),
		l: Number(n >> _32n & U32_MASK64)
	};
	return {
		h: Number(n >> _32n & U32_MASK64) | 0,
		l: Number(n & U32_MASK64) | 0
	};
}
function split(lst, le = false) {
	let Ah = new Uint32Array(lst.length);
	let Al = new Uint32Array(lst.length);
	for (let i = 0; i < lst.length; i++) {
		const { h, l } = fromBig(lst[i], le);
		[Ah[i], Al[i]] = [h, l];
	}
	return [Ah, Al];
}
const toBig = (h, l) => BigInt(h >>> 0) << _32n | BigInt(l >>> 0);
const shrSH = (h, _l, s) => h >>> s;
const shrSL = (h, l, s) => h << 32 - s | l >>> s;
const rotrSH = (h, l, s) => h >>> s | l << 32 - s;
const rotrSL = (h, l, s) => h << 32 - s | l >>> s;
const rotrBH = (h, l, s) => h << 64 - s | l >>> s - 32;
const rotrBL = (h, l, s) => h >>> s - 32 | l << 64 - s;
const rotr32H = (_h, l) => l;
const rotr32L = (h, _l) => h;
const rotlSH = (h, l, s) => h << s | l >>> 32 - s;
const rotlSL = (h, l, s) => l << s | h >>> 32 - s;
const rotlBH = (h, l, s) => l << s - 32 | h >>> 64 - s;
const rotlBL = (h, l, s) => h << s - 32 | l >>> 64 - s;
function add(Ah, Al, Bh, Bl) {
	const l = (Al >>> 0) + (Bl >>> 0);
	return {
		h: Ah + Bh + (l / 2 ** 32 | 0) | 0,
		l: l | 0
	};
}
const add3L = (Al, Bl, Cl) => (Al >>> 0) + (Bl >>> 0) + (Cl >>> 0);
const add3H = (low, Ah, Bh, Ch) => Ah + Bh + Ch + (low / 2 ** 32 | 0) | 0;
const add4L = (Al, Bl, Cl, Dl) => (Al >>> 0) + (Bl >>> 0) + (Cl >>> 0) + (Dl >>> 0);
const add4H = (low, Ah, Bh, Ch, Dh) => Ah + Bh + Ch + Dh + (low / 2 ** 32 | 0) | 0;
const add5L = (Al, Bl, Cl, Dl, El) => (Al >>> 0) + (Bl >>> 0) + (Cl >>> 0) + (Dl >>> 0) + (El >>> 0);
const add5H = (low, Ah, Bh, Ch, Dh, Eh) => Ah + Bh + Ch + Dh + Eh + (low / 2 ** 32 | 0) | 0;
const u64 = {
	fromBig,
	split,
	toBig,
	shrSH,
	shrSL,
	rotrSH,
	rotrSL,
	rotrBH,
	rotrBL,
	rotr32H,
	rotr32L,
	rotlSH,
	rotlSL,
	rotlBH,
	rotlBL,
	add,
	add3L,
	add3H,
	add4L,
	add4H,
	add5H,
	add5L
};
// node_modules/@noble/hashes/esm/sha512.js
/**
* SHA2-512 a.k.a. sha512 and sha384. It is slower than sha256 in js because u64 operations are slow.
*
* Check out [RFC 4634](https://datatracker.ietf.org/doc/html/rfc4634) and
* [the paper on truncated SHA512/256](https://eprint.iacr.org/2010/548.pdf).
* @module
*/
const [SHA512_Kh, SHA512_Kl] = /* @__PURE__ */ (() => u64.split([
	"0x428a2f98d728ae22",
	"0x7137449123ef65cd",
	"0xb5c0fbcfec4d3b2f",
	"0xe9b5dba58189dbbc",
	"0x3956c25bf348b538",
	"0x59f111f1b605d019",
	"0x923f82a4af194f9b",
	"0xab1c5ed5da6d8118",
	"0xd807aa98a3030242",
	"0x12835b0145706fbe",
	"0x243185be4ee4b28c",
	"0x550c7dc3d5ffb4e2",
	"0x72be5d74f27b896f",
	"0x80deb1fe3b1696b1",
	"0x9bdc06a725c71235",
	"0xc19bf174cf692694",
	"0xe49b69c19ef14ad2",
	"0xefbe4786384f25e3",
	"0x0fc19dc68b8cd5b5",
	"0x240ca1cc77ac9c65",
	"0x2de92c6f592b0275",
	"0x4a7484aa6ea6e483",
	"0x5cb0a9dcbd41fbd4",
	"0x76f988da831153b5",
	"0x983e5152ee66dfab",
	"0xa831c66d2db43210",
	"0xb00327c898fb213f",
	"0xbf597fc7beef0ee4",
	"0xc6e00bf33da88fc2",
	"0xd5a79147930aa725",
	"0x06ca6351e003826f",
	"0x142929670a0e6e70",
	"0x27b70a8546d22ffc",
	"0x2e1b21385c26c926",
	"0x4d2c6dfc5ac42aed",
	"0x53380d139d95b3df",
	"0x650a73548baf63de",
	"0x766a0abb3c77b2a8",
	"0x81c2c92e47edaee6",
	"0x92722c851482353b",
	"0xa2bfe8a14cf10364",
	"0xa81a664bbc423001",
	"0xc24b8b70d0f89791",
	"0xc76c51a30654be30",
	"0xd192e819d6ef5218",
	"0xd69906245565a910",
	"0xf40e35855771202a",
	"0x106aa07032bbd1b8",
	"0x19a4c116b8d2d0c8",
	"0x1e376c085141ab53",
	"0x2748774cdf8eeb99",
	"0x34b0bcb5e19b48a8",
	"0x391c0cb3c5c95a63",
	"0x4ed8aa4ae3418acb",
	"0x5b9cca4f7763e373",
	"0x682e6ff3d6b2b8a3",
	"0x748f82ee5defb2fc",
	"0x78a5636f43172f60",
	"0x84c87814a1f0ab72",
	"0x8cc702081a6439ec",
	"0x90befffa23631e28",
	"0xa4506cebde82bde9",
	"0xbef9a3f7b2c67915",
	"0xc67178f2e372532b",
	"0xca273eceea26619c",
	"0xd186b8c721c0c207",
	"0xeada7dd6cde0eb1e",
	"0xf57d4f7fee6ed178",
	"0x06f067aa72176fba",
	"0x0a637dc5a2c898a6",
	"0x113f9804bef90dae",
	"0x1b710b35131c471b",
	"0x28db77f523047d84",
	"0x32caab7b40c72493",
	"0x3c9ebe0a15c9bebc",
	"0x431d67c49c100d4c",
	"0x4cc5d4becb3e42b6",
	"0x597f299cfc657e2a",
	"0x5fcb6fab3ad6faec",
	"0x6c44198c4a475817"
].map((n) => BigInt(n))))();
const SHA512_W_H = /* @__PURE__ */ new Uint32Array(80);
const SHA512_W_L = /* @__PURE__ */ new Uint32Array(80);
var SHA512 = class extends HashMD {
	constructor() {
		super(128, 64, 16, false);
		this.Ah = 1779033703;
		this.Al = -205731576;
		this.Bh = -1150833019;
		this.Bl = -2067093701;
		this.Ch = 1013904242;
		this.Cl = -23791573;
		this.Dh = -1521486534;
		this.Dl = 1595750129;
		this.Eh = 1359893119;
		this.El = -1377402159;
		this.Fh = -1694144372;
		this.Fl = 725511199;
		this.Gh = 528734635;
		this.Gl = -79577749;
		this.Hh = 1541459225;
		this.Hl = 327033209;
	}
	get() {
		const { Ah, Al, Bh, Bl, Ch, Cl, Dh, Dl, Eh, El, Fh, Fl, Gh, Gl, Hh, Hl } = this;
		return [
			Ah,
			Al,
			Bh,
			Bl,
			Ch,
			Cl,
			Dh,
			Dl,
			Eh,
			El,
			Fh,
			Fl,
			Gh,
			Gl,
			Hh,
			Hl
		];
	}
	set(Ah, Al, Bh, Bl, Ch, Cl, Dh, Dl, Eh, El, Fh, Fl, Gh, Gl, Hh, Hl) {
		this.Ah = Ah | 0;
		this.Al = Al | 0;
		this.Bh = Bh | 0;
		this.Bl = Bl | 0;
		this.Ch = Ch | 0;
		this.Cl = Cl | 0;
		this.Dh = Dh | 0;
		this.Dl = Dl | 0;
		this.Eh = Eh | 0;
		this.El = El | 0;
		this.Fh = Fh | 0;
		this.Fl = Fl | 0;
		this.Gh = Gh | 0;
		this.Gl = Gl | 0;
		this.Hh = Hh | 0;
		this.Hl = Hl | 0;
	}
	process(view, offset) {
		for (let i = 0; i < 16; i++, offset += 4) {
			SHA512_W_H[i] = view.getUint32(offset);
			SHA512_W_L[i] = view.getUint32(offset += 4);
		}
		for (let i = 16; i < 80; i++) {
			const W15h = SHA512_W_H[i - 15] | 0;
			const W15l = SHA512_W_L[i - 15] | 0;
			const s0h = u64.rotrSH(W15h, W15l, 1) ^ u64.rotrSH(W15h, W15l, 8) ^ u64.shrSH(W15h, W15l, 7);
			const s0l = u64.rotrSL(W15h, W15l, 1) ^ u64.rotrSL(W15h, W15l, 8) ^ u64.shrSL(W15h, W15l, 7);
			const W2h = SHA512_W_H[i - 2] | 0;
			const W2l = SHA512_W_L[i - 2] | 0;
			const s1h = u64.rotrSH(W2h, W2l, 19) ^ u64.rotrBH(W2h, W2l, 61) ^ u64.shrSH(W2h, W2l, 6);
			const s1l = u64.rotrSL(W2h, W2l, 19) ^ u64.rotrBL(W2h, W2l, 61) ^ u64.shrSL(W2h, W2l, 6);
			const SUMl = u64.add4L(s0l, s1l, SHA512_W_L[i - 7], SHA512_W_L[i - 16]);
			const SUMh = u64.add4H(SUMl, s0h, s1h, SHA512_W_H[i - 7], SHA512_W_H[i - 16]);
			SHA512_W_H[i] = SUMh | 0;
			SHA512_W_L[i] = SUMl | 0;
		}
		let { Ah, Al, Bh, Bl, Ch, Cl, Dh, Dl, Eh, El, Fh, Fl, Gh, Gl, Hh, Hl } = this;
		for (let i = 0; i < 80; i++) {
			const sigma1h = u64.rotrSH(Eh, El, 14) ^ u64.rotrSH(Eh, El, 18) ^ u64.rotrBH(Eh, El, 41);
			const sigma1l = u64.rotrSL(Eh, El, 14) ^ u64.rotrSL(Eh, El, 18) ^ u64.rotrBL(Eh, El, 41);
			const CHIh = Eh & Fh ^ ~Eh & Gh;
			const CHIl = El & Fl ^ ~El & Gl;
			const T1ll = u64.add5L(Hl, sigma1l, CHIl, SHA512_Kl[i], SHA512_W_L[i]);
			const T1h = u64.add5H(T1ll, Hh, sigma1h, CHIh, SHA512_Kh[i], SHA512_W_H[i]);
			const T1l = T1ll | 0;
			const sigma0h = u64.rotrSH(Ah, Al, 28) ^ u64.rotrBH(Ah, Al, 34) ^ u64.rotrBH(Ah, Al, 39);
			const sigma0l = u64.rotrSL(Ah, Al, 28) ^ u64.rotrBL(Ah, Al, 34) ^ u64.rotrBL(Ah, Al, 39);
			const MAJh = Ah & Bh ^ Ah & Ch ^ Bh & Ch;
			const MAJl = Al & Bl ^ Al & Cl ^ Bl & Cl;
			Hh = Gh | 0;
			Hl = Gl | 0;
			Gh = Fh | 0;
			Gl = Fl | 0;
			Fh = Eh | 0;
			Fl = El | 0;
			({h: Eh, l: El} = u64.add(Dh | 0, Dl | 0, T1h | 0, T1l | 0));
			Dh = Ch | 0;
			Dl = Cl | 0;
			Ch = Bh | 0;
			Cl = Bl | 0;
			Bh = Ah | 0;
			Bl = Al | 0;
			const All = u64.add3L(T1l, sigma0l, MAJl);
			Ah = u64.add3H(All, T1h, sigma0h, MAJh);
			Al = All | 0;
		}
		({h: Ah, l: Al} = u64.add(this.Ah | 0, this.Al | 0, Ah | 0, Al | 0));
		({h: Bh, l: Bl} = u64.add(this.Bh | 0, this.Bl | 0, Bh | 0, Bl | 0));
		({h: Ch, l: Cl} = u64.add(this.Ch | 0, this.Cl | 0, Ch | 0, Cl | 0));
		({h: Dh, l: Dl} = u64.add(this.Dh | 0, this.Dl | 0, Dh | 0, Dl | 0));
		({h: Eh, l: El} = u64.add(this.Eh | 0, this.El | 0, Eh | 0, El | 0));
		({h: Fh, l: Fl} = u64.add(this.Fh | 0, this.Fl | 0, Fh | 0, Fl | 0));
		({h: Gh, l: Gl} = u64.add(this.Gh | 0, this.Gl | 0, Gh | 0, Gl | 0));
		({h: Hh, l: Hl} = u64.add(this.Hh | 0, this.Hl | 0, Hh | 0, Hl | 0));
		this.set(Ah, Al, Bh, Bl, Ch, Cl, Dh, Dl, Eh, El, Fh, Fl, Gh, Gl, Hh, Hl);
	}
	roundClean() {
		SHA512_W_H.fill(0);
		SHA512_W_L.fill(0);
	}
	destroy() {
		this.buffer.fill(0);
		this.set(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
	}
};
/** SHA2-512 hash function. */
const sha512 = /* @__PURE__ */ wrapConstructor(() => new SHA512());
// node_modules/@noble/curves/esm/abstract/utils.js
/**
* Hex, bytes and number utilities.
* @module
*/
/*! noble-curves - MIT License (c) 2022 Paul Miller (paulmillr.com) */
const _0n$4 = /* @__PURE__ */ BigInt(0);
const _1n$5 = /* @__PURE__ */ BigInt(1);
const _2n$3 = /* @__PURE__ */ BigInt(2);
function isBytes$1(a) {
	return a instanceof Uint8Array || ArrayBuffer.isView(a) && a.constructor.name === "Uint8Array";
}
function abytes(item) {
	if (!isBytes$1(item)) throw new Error("Uint8Array expected");
}
function abool(title, value) {
	if (typeof value !== "boolean") throw new Error(title + " boolean expected, got " + value);
}
const hexes = /* @__PURE__ */ Array.from({ length: 256 }, (_, i) => i.toString(16).padStart(2, "0"));
/**
* @example bytesToHex(Uint8Array.from([0xca, 0xfe, 0x01, 0x23])) // 'cafe0123'
*/
function bytesToHex$1(bytes) {
	abytes(bytes);
	let hex = "";
	for (let i = 0; i < bytes.length; i++) hex += hexes[bytes[i]];
	return hex;
}
function hexToNumber(hex) {
	if (typeof hex !== "string") throw new Error("hex string expected, got " + typeof hex);
	return hex === "" ? _0n$4 : BigInt("0x" + hex);
}
const asciis = {
	_0: 48,
	_9: 57,
	A: 65,
	F: 70,
	a: 97,
	f: 102
};
function asciiToBase16(ch) {
	if (ch >= asciis._0 && ch <= asciis._9) return ch - asciis._0;
	if (ch >= asciis.A && ch <= asciis.F) return ch - (asciis.A - 10);
	if (ch >= asciis.a && ch <= asciis.f) return ch - (asciis.a - 10);
}
/**
* @example hexToBytes('cafe0123') // Uint8Array.from([0xca, 0xfe, 0x01, 0x23])
*/
function hexToBytes(hex) {
	if (typeof hex !== "string") throw new Error("hex string expected, got " + typeof hex);
	const hl = hex.length;
	const al = hl / 2;
	if (hl % 2) throw new Error("hex string expected, got unpadded hex of length " + hl);
	const array = new Uint8Array(al);
	for (let ai = 0, hi = 0; ai < al; ai++, hi += 2) {
		const n1 = asciiToBase16(hex.charCodeAt(hi));
		const n2 = asciiToBase16(hex.charCodeAt(hi + 1));
		if (n1 === void 0 || n2 === void 0) {
			const char = hex[hi] + hex[hi + 1];
			throw new Error("hex string expected, got non-hex character \"" + char + "\" at index " + hi);
		}
		array[ai] = n1 * 16 + n2;
	}
	return array;
}
function bytesToNumberBE(bytes) {
	return hexToNumber(bytesToHex$1(bytes));
}
function bytesToNumberLE(bytes) {
	abytes(bytes);
	return hexToNumber(bytesToHex$1(Uint8Array.from(bytes).reverse()));
}
function numberToBytesBE(n, len) {
	return hexToBytes(n.toString(16).padStart(len * 2, "0"));
}
function numberToBytesLE(n, len) {
	return numberToBytesBE(n, len).reverse();
}
/**
* Takes hex string or Uint8Array, converts to Uint8Array.
* Validates output length.
* Will throw error for other types.
* @param title descriptive title for an error e.g. 'private key'
* @param hex hex string or Uint8Array
* @param expectedLength optional, will compare to result array's length
* @returns
*/
function ensureBytes(title, hex, expectedLength) {
	let res;
	if (typeof hex === "string") try {
		res = hexToBytes(hex);
	} catch (e) {
		throw new Error(title + " must be hex string or Uint8Array, cause: " + e);
	}
	else if (isBytes$1(hex)) res = Uint8Array.from(hex);
	else throw new Error(title + " must be hex string or Uint8Array");
	const len = res.length;
	if (typeof expectedLength === "number" && len !== expectedLength) throw new Error(title + " of length " + expectedLength + " expected, got " + len);
	return res;
}
/**
* Copies several Uint8Arrays into one.
*/
function concatBytes(...arrays) {
	let sum = 0;
	for (let i = 0; i < arrays.length; i++) {
		const a = arrays[i];
		abytes(a);
		sum += a.length;
	}
	const res = new Uint8Array(sum);
	for (let i = 0, pad = 0; i < arrays.length; i++) {
		const a = arrays[i];
		res.set(a, pad);
		pad += a.length;
	}
	return res;
}
const isPosBig = (n) => typeof n === "bigint" && _0n$4 <= n;
function inRange(n, min, max) {
	return isPosBig(n) && isPosBig(min) && isPosBig(max) && min <= n && n < max;
}
/**
* Asserts min <= n < max. NOTE: It's < max and not <= max.
* @example
* aInRange('x', x, 1n, 256n); // would assume x is in (1n..255n)
*/
function aInRange(title, n, min, max) {
	if (!inRange(n, min, max)) throw new Error("expected valid " + title + ": " + min + " <= n < " + max + ", got " + n);
}
/**
* Calculates amount of bits in a bigint.
* Same as `n.toString(2).length`
*/
function bitLen(n) {
	let len = 0;
	for (; n > _0n$4; n >>= _1n$5, len += 1);
	return len;
}
/**
* Calculate mask for N bits. Not using ** operator with bigints because of old engines.
* Same as BigInt(`0b${Array(i).fill('1').join('')}`)
*/
const bitMask = (n) => (_2n$3 << BigInt(n - 1)) - _1n$5;
const validatorFns = {
	bigint: (val) => typeof val === "bigint",
	function: (val) => typeof val === "function",
	boolean: (val) => typeof val === "boolean",
	string: (val) => typeof val === "string",
	stringOrUint8Array: (val) => typeof val === "string" || isBytes$1(val),
	isSafeInteger: (val) => Number.isSafeInteger(val),
	array: (val) => Array.isArray(val),
	field: (val, object) => object.Fp.isValid(val),
	hash: (val) => typeof val === "function" && Number.isSafeInteger(val.outputLen)
};
function validateObject(object, validators, optValidators = {}) {
	const checkField = (fieldName, type, isOptional) => {
		const checkVal = validatorFns[type];
		if (typeof checkVal !== "function") throw new Error("invalid validator function");
		const val = object[fieldName];
		if (isOptional && val === void 0) return;
		if (!checkVal(val, object)) throw new Error("param " + String(fieldName) + " is invalid. Expected " + type + ", got " + val);
	};
	for (const [fieldName, type] of Object.entries(validators)) checkField(fieldName, type, false);
	for (const [fieldName, type] of Object.entries(optValidators)) checkField(fieldName, type, true);
	return object;
}
/**
* Memoizes (caches) computation result.
* Uses WeakMap: the value is going auto-cleaned by GC after last reference is removed.
*/
function memoized(fn) {
	const map = /* @__PURE__ */ new WeakMap();
	return (arg, ...args) => {
		const val = map.get(arg);
		if (val !== void 0) return val;
		const computed = fn(arg, ...args);
		map.set(arg, computed);
		return computed;
	};
}
// node_modules/@noble/curves/esm/abstract/modular.js
/**
* Utils for modular division and finite fields.
* A finite field over 11 is integer number operations `mod 11`.
* There is no division: it is replaced by modular multiplicative inverse.
* @module
*/
/*! noble-curves - MIT License (c) 2022 Paul Miller (paulmillr.com) */
const _0n$3 = BigInt(0);
const _1n$4 = BigInt(1);
const _2n$2 = /* @__PURE__ */ BigInt(2);
const _3n$1 = /* @__PURE__ */ BigInt(3);
const _4n = /* @__PURE__ */ BigInt(4);
const _5n$1 = /* @__PURE__ */ BigInt(5);
const _8n$2 = /* @__PURE__ */ BigInt(8);
const _9n = /* @__PURE__ */ BigInt(9);
const _16n = /* @__PURE__ */ BigInt(16);
function mod(a, b) {
	const result = a % b;
	return result >= _0n$3 ? result : b + result;
}
/**
* Efficiently raise num to power and do modular division.
* Unsafe in some contexts: uses ladder, so can expose bigint bits.
* @todo use field version && remove
* @example
* pow(2n, 6n, 11n) // 64n % 11n == 9n
*/
function pow(num, power, modulo) {
	if (power < _0n$3) throw new Error("invalid exponent, negatives unsupported");
	if (modulo <= _0n$3) throw new Error("invalid modulus");
	if (modulo === _1n$4) return _0n$3;
	let res = _1n$4;
	while (power > _0n$3) {
		if (power & _1n$4) res = res * num % modulo;
		num = num * num % modulo;
		power >>= _1n$4;
	}
	return res;
}
/** Does `x^(2^power)` mod p. `pow2(30, 4)` == `30^(2^4)` */
function pow2(x, power, modulo) {
	let res = x;
	while (power-- > _0n$3) {
		res *= res;
		res %= modulo;
	}
	return res;
}
/**
* Inverses number over modulo.
* Implemented using [Euclidean GCD](https://brilliant.org/wiki/extended-euclidean-algorithm/).
*/
function invert(number, modulo) {
	if (number === _0n$3) throw new Error("invert: expected non-zero number");
	if (modulo <= _0n$3) throw new Error("invert: expected positive modulus, got " + modulo);
	let a = mod(number, modulo);
	let b = modulo;
	let x = _0n$3, y = _1n$4, u = _1n$4, v = _0n$3;
	while (a !== _0n$3) {
		const q = b / a;
		const r = b % a;
		const m = x - u * q;
		const n = y - v * q;
		b = a, a = r, x = u, y = v, u = m, v = n;
	}
	if (b !== _1n$4) throw new Error("invert: does not exist");
	return mod(x, modulo);
}
/**
* Tonelli-Shanks square root search algorithm.
* 1. https://eprint.iacr.org/2012/685.pdf (page 12)
* 2. Square Roots from 1; 24, 51, 10 to Dan Shanks
* Will start an infinite loop if field order P is not prime.
* @param P field order
* @returns function that takes field Fp (created from P) and number n
*/
function tonelliShanks(P) {
	const legendreC = (P - _1n$4) / _2n$2;
	let Q, S, Z;
	for (Q = P - _1n$4, S = 0; Q % _2n$2 === _0n$3; Q /= _2n$2, S++);
	for (Z = _2n$2; Z < P && pow(Z, legendreC, P) !== P - _1n$4; Z++) if (Z > 1e3) throw new Error("Cannot find square root: likely non-prime P");
	if (S === 1) {
		const p1div4 = (P + _1n$4) / _4n;
		return function tonelliFast(Fp, n) {
			const root = Fp.pow(n, p1div4);
			if (!Fp.eql(Fp.sqr(root), n)) throw new Error("Cannot find square root");
			return root;
		};
	}
	const Q1div2 = (Q + _1n$4) / _2n$2;
	return function tonelliSlow(Fp, n) {
		if (Fp.pow(n, legendreC) === Fp.neg(Fp.ONE)) throw new Error("Cannot find square root");
		let r = S;
		let g = Fp.pow(Fp.mul(Fp.ONE, Z), Q);
		let x = Fp.pow(n, Q1div2);
		let b = Fp.pow(n, Q);
		while (!Fp.eql(b, Fp.ONE)) {
			if (Fp.eql(b, Fp.ZERO)) return Fp.ZERO;
			let m = 1;
			for (let t2 = Fp.sqr(b); m < r; m++) {
				if (Fp.eql(t2, Fp.ONE)) break;
				t2 = Fp.sqr(t2);
			}
			const ge = Fp.pow(g, _1n$4 << BigInt(r - m - 1));
			g = Fp.sqr(ge);
			x = Fp.mul(x, ge);
			b = Fp.mul(b, g);
			r = m;
		}
		return x;
	};
}
/**
* Square root for a finite field. It will try to check if optimizations are applicable and fall back to 4:
*
* 1. P ≡ 3 (mod 4)
* 2. P ≡ 5 (mod 8)
* 3. P ≡ 9 (mod 16)
* 4. Tonelli-Shanks algorithm
*
* Different algorithms can give different roots, it is up to user to decide which one they want.
* For example there is FpSqrtOdd/FpSqrtEven to choice root based on oddness (used for hash-to-curve).
*/
function FpSqrt(P) {
	if (P % _4n === _3n$1) {
		const p1div4 = (P + _1n$4) / _4n;
		return function sqrt3mod4(Fp, n) {
			const root = Fp.pow(n, p1div4);
			if (!Fp.eql(Fp.sqr(root), n)) throw new Error("Cannot find square root");
			return root;
		};
	}
	if (P % _8n$2 === _5n$1) {
		const c1 = (P - _5n$1) / _8n$2;
		return function sqrt5mod8(Fp, n) {
			const n2 = Fp.mul(n, _2n$2);
			const v = Fp.pow(n2, c1);
			const nv = Fp.mul(n, v);
			const i = Fp.mul(Fp.mul(nv, _2n$2), v);
			const root = Fp.mul(nv, Fp.sub(i, Fp.ONE));
			if (!Fp.eql(Fp.sqr(root), n)) throw new Error("Cannot find square root");
			return root;
		};
	}
	if (P % _16n === _9n) {}
	return tonelliShanks(P);
}
const isNegativeLE = (num, modulo) => (mod(num, modulo) & _1n$4) === _1n$4;
const FIELD_FIELDS = [
	"create",
	"isValid",
	"is0",
	"neg",
	"inv",
	"sqrt",
	"sqr",
	"eql",
	"add",
	"sub",
	"mul",
	"pow",
	"div",
	"addN",
	"subN",
	"mulN",
	"sqrN"
];
function validateField(field) {
	return validateObject(field, FIELD_FIELDS.reduce((map, val) => {
		map[val] = "function";
		return map;
	}, {
		ORDER: "bigint",
		MASK: "bigint",
		BYTES: "isSafeInteger",
		BITS: "isSafeInteger"
	}));
}
/**
* Same as `pow` but for Fp: non-constant-time.
* Unsafe in some contexts: uses ladder, so can expose bigint bits.
*/
function FpPow(f, num, power) {
	if (power < _0n$3) throw new Error("invalid exponent, negatives unsupported");
	if (power === _0n$3) return f.ONE;
	if (power === _1n$4) return num;
	let p = f.ONE;
	let d = num;
	while (power > _0n$3) {
		if (power & _1n$4) p = f.mul(p, d);
		d = f.sqr(d);
		power >>= _1n$4;
	}
	return p;
}
/**
* Efficiently invert an array of Field elements.
* `inv(0)` will return `undefined` here: make sure to throw an error.
*/
function FpInvertBatch(f, nums) {
	const tmp = new Array(nums.length);
	const lastMultiplied = nums.reduce((acc, num, i) => {
		if (f.is0(num)) return acc;
		tmp[i] = acc;
		return f.mul(acc, num);
	}, f.ONE);
	const inverted = f.inv(lastMultiplied);
	nums.reduceRight((acc, num, i) => {
		if (f.is0(num)) return acc;
		tmp[i] = f.mul(acc, tmp[i]);
		return f.mul(acc, num);
	}, inverted);
	return tmp;
}
function nLength(n, nBitLength) {
	const _nBitLength = nBitLength !== void 0 ? nBitLength : n.toString(2).length;
	return {
		nBitLength: _nBitLength,
		nByteLength: Math.ceil(_nBitLength / 8)
	};
}
/**
* Initializes a finite field over prime.
* Major performance optimizations:
* * a) denormalized operations like mulN instead of mul
* * b) same object shape: never add or remove keys
* * c) Object.freeze
* Fragile: always run a benchmark on a change.
* Security note: operations don't check 'isValid' for all elements for performance reasons,
* it is caller responsibility to check this.
* This is low-level code, please make sure you know what you're doing.
* @param ORDER prime positive bigint
* @param bitLen how many bits the field consumes
* @param isLE (def: false) if encoding / decoding should be in little-endian
* @param redef optional faster redefinitions of sqrt and other methods
*/
function Field(ORDER, bitLen, isLE = false, redef = {}) {
	if (ORDER <= _0n$3) throw new Error("invalid field: expected ORDER > 0, got " + ORDER);
	const { nBitLength: BITS, nByteLength: BYTES } = nLength(ORDER, bitLen);
	if (BYTES > 2048) throw new Error("invalid field: expected ORDER of <= 2048 bytes");
	let sqrtP;
	const f = Object.freeze({
		ORDER,
		isLE,
		BITS,
		BYTES,
		MASK: bitMask(BITS),
		ZERO: _0n$3,
		ONE: _1n$4,
		create: (num) => mod(num, ORDER),
		isValid: (num) => {
			if (typeof num !== "bigint") throw new Error("invalid field element: expected bigint, got " + typeof num);
			return _0n$3 <= num && num < ORDER;
		},
		is0: (num) => num === _0n$3,
		isOdd: (num) => (num & _1n$4) === _1n$4,
		neg: (num) => mod(-num, ORDER),
		eql: (lhs, rhs) => lhs === rhs,
		sqr: (num) => mod(num * num, ORDER),
		add: (lhs, rhs) => mod(lhs + rhs, ORDER),
		sub: (lhs, rhs) => mod(lhs - rhs, ORDER),
		mul: (lhs, rhs) => mod(lhs * rhs, ORDER),
		pow: (num, power) => FpPow(f, num, power),
		div: (lhs, rhs) => mod(lhs * invert(rhs, ORDER), ORDER),
		sqrN: (num) => num * num,
		addN: (lhs, rhs) => lhs + rhs,
		subN: (lhs, rhs) => lhs - rhs,
		mulN: (lhs, rhs) => lhs * rhs,
		inv: (num) => invert(num, ORDER),
		sqrt: redef.sqrt || ((n) => {
			if (!sqrtP) sqrtP = FpSqrt(ORDER);
			return sqrtP(f, n);
		}),
		invertBatch: (lst) => FpInvertBatch(f, lst),
		cmov: (a, b, c) => c ? b : a,
		toBytes: (num) => isLE ? numberToBytesLE(num, BYTES) : numberToBytesBE(num, BYTES),
		fromBytes: (bytes) => {
			if (bytes.length !== BYTES) throw new Error("Field.fromBytes: expected " + BYTES + " bytes, got " + bytes.length);
			return isLE ? bytesToNumberLE(bytes) : bytesToNumberBE(bytes);
		}
	});
	return Object.freeze(f);
}
// node_modules/@noble/curves/esm/abstract/curve.js
/**
* Methods for elliptic curve multiplication by scalars.
* Contains wNAF, pippenger
* @module
*/
/*! noble-curves - MIT License (c) 2022 Paul Miller (paulmillr.com) */
const _0n$2 = BigInt(0);
const _1n$3 = BigInt(1);
function constTimeNegate(condition, item) {
	const neg = item.negate();
	return condition ? neg : item;
}
function validateW(W, bits) {
	if (!Number.isSafeInteger(W) || W <= 0 || W > bits) throw new Error("invalid window size, expected [1.." + bits + "], got W=" + W);
}
function calcWOpts(W, bits) {
	validateW(W, bits);
	return {
		windows: Math.ceil(bits / W) + 1,
		windowSize: 2 ** (W - 1)
	};
}
function validateMSMPoints(points, c) {
	if (!Array.isArray(points)) throw new Error("array expected");
	points.forEach((p, i) => {
		if (!(p instanceof c)) throw new Error("invalid point at index " + i);
	});
}
function validateMSMScalars(scalars, field) {
	if (!Array.isArray(scalars)) throw new Error("array of scalars expected");
	scalars.forEach((s, i) => {
		if (!field.isValid(s)) throw new Error("invalid scalar at index " + i);
	});
}
const pointPrecomputes = /* @__PURE__ */ new WeakMap();
const pointWindowSizes = /* @__PURE__ */ new WeakMap();
function getW(P) {
	return pointWindowSizes.get(P) || 1;
}
/**
* Elliptic curve multiplication of Point by scalar. Fragile.
* Scalars should always be less than curve order: this should be checked inside of a curve itself.
* Creates precomputation tables for fast multiplication:
* - private scalar is split by fixed size windows of W bits
* - every window point is collected from window's table & added to accumulator
* - since windows are different, same point inside tables won't be accessed more than once per calc
* - each multiplication is 'Math.ceil(CURVE_ORDER / 𝑊) + 1' point additions (fixed for any scalar)
* - +1 window is neccessary for wNAF
* - wNAF reduces table size: 2x less memory + 2x faster generation, but 10% slower multiplication
*
* @todo Research returning 2d JS array of windows, instead of a single window.
* This would allow windows to be in different memory locations
*/
function wNAF(c, bits) {
	return {
		constTimeNegate,
		hasPrecomputes(elm) {
			return getW(elm) !== 1;
		},
		unsafeLadder(elm, n, p = c.ZERO) {
			let d = elm;
			while (n > _0n$2) {
				if (n & _1n$3) p = p.add(d);
				d = d.double();
				n >>= _1n$3;
			}
			return p;
		},
		/**
		* Creates a wNAF precomputation window. Used for caching.
		* Default window size is set by `utils.precompute()` and is equal to 8.
		* Number of precomputed points depends on the curve size:
		* 2^(𝑊−1) * (Math.ceil(𝑛 / 𝑊) + 1), where:
		* - 𝑊 is the window size
		* - 𝑛 is the bitlength of the curve order.
		* For a 256-bit curve and window size 8, the number of precomputed points is 128 * 33 = 4224.
		* @param elm Point instance
		* @param W window size
		* @returns precomputed point tables flattened to a single array
		*/
		precomputeWindow(elm, W) {
			const { windows, windowSize } = calcWOpts(W, bits);
			const points = [];
			let p = elm;
			let base = p;
			for (let window = 0; window < windows; window++) {
				base = p;
				points.push(base);
				for (let i = 1; i < windowSize; i++) {
					base = base.add(p);
					points.push(base);
				}
				p = base.double();
			}
			return points;
		},
		/**
		* Implements ec multiplication using precomputed tables and w-ary non-adjacent form.
		* @param W window size
		* @param precomputes precomputed tables
		* @param n scalar (we don't check here, but should be less than curve order)
		* @returns real and fake (for const-time) points
		*/
		wNAF(W, precomputes, n) {
			const { windows, windowSize } = calcWOpts(W, bits);
			let p = c.ZERO;
			let f = c.BASE;
			const mask = BigInt(2 ** W - 1);
			const maxNumber = 2 ** W;
			const shiftBy = BigInt(W);
			for (let window = 0; window < windows; window++) {
				const offset = window * windowSize;
				let wbits = Number(n & mask);
				n >>= shiftBy;
				if (wbits > windowSize) {
					wbits -= maxNumber;
					n += _1n$3;
				}
				const offset1 = offset;
				const offset2 = offset + Math.abs(wbits) - 1;
				const cond1 = window % 2 !== 0;
				const cond2 = wbits < 0;
				if (wbits === 0) f = f.add(constTimeNegate(cond1, precomputes[offset1]));
				else p = p.add(constTimeNegate(cond2, precomputes[offset2]));
			}
			return {
				p,
				f
			};
		},
		/**
		* Implements ec unsafe (non const-time) multiplication using precomputed tables and w-ary non-adjacent form.
		* @param W window size
		* @param precomputes precomputed tables
		* @param n scalar (we don't check here, but should be less than curve order)
		* @param acc accumulator point to add result of multiplication
		* @returns point
		*/
		wNAFUnsafe(W, precomputes, n, acc = c.ZERO) {
			const { windows, windowSize } = calcWOpts(W, bits);
			const mask = BigInt(2 ** W - 1);
			const maxNumber = 2 ** W;
			const shiftBy = BigInt(W);
			for (let window = 0; window < windows; window++) {
				const offset = window * windowSize;
				if (n === _0n$2) break;
				let wbits = Number(n & mask);
				n >>= shiftBy;
				if (wbits > windowSize) {
					wbits -= maxNumber;
					n += _1n$3;
				}
				if (wbits === 0) continue;
				let curr = precomputes[offset + Math.abs(wbits) - 1];
				if (wbits < 0) curr = curr.negate();
				acc = acc.add(curr);
			}
			return acc;
		},
		getPrecomputes(W, P, transform) {
			let comp = pointPrecomputes.get(P);
			if (!comp) {
				comp = this.precomputeWindow(P, W);
				if (W !== 1) pointPrecomputes.set(P, transform(comp));
			}
			return comp;
		},
		wNAFCached(P, n, transform) {
			const W = getW(P);
			return this.wNAF(W, this.getPrecomputes(W, P, transform), n);
		},
		wNAFCachedUnsafe(P, n, transform, prev) {
			const W = getW(P);
			if (W === 1) return this.unsafeLadder(P, n, prev);
			return this.wNAFUnsafe(W, this.getPrecomputes(W, P, transform), n, prev);
		},
		setWindowSize(P, W) {
			validateW(W, bits);
			pointWindowSizes.set(P, W);
			pointPrecomputes.delete(P);
		}
	};
}
/**
* Pippenger algorithm for multi-scalar multiplication (MSM, Pa + Qb + Rc + ...).
* 30x faster vs naive addition on L=4096, 10x faster with precomputes.
* For N=254bit, L=1, it does: 1024 ADD + 254 DBL. For L=5: 1536 ADD + 254 DBL.
* Algorithmically constant-time (for same L), even when 1 point + scalar, or when scalar = 0.
* @param c Curve Point constructor
* @param fieldN field over CURVE.N - important that it's not over CURVE.P
* @param points array of L curve points
* @param scalars array of L scalars (aka private keys / bigints)
*/
function pippenger(c, fieldN, points, scalars) {
	validateMSMPoints(points, c);
	validateMSMScalars(scalars, fieldN);
	if (points.length !== scalars.length) throw new Error("arrays of points and scalars must have equal length");
	const zero = c.ZERO;
	const wbits = bitLen(BigInt(points.length));
	const windowSize = wbits > 12 ? wbits - 3 : wbits > 4 ? wbits - 2 : wbits ? 2 : 1;
	const MASK = (1 << windowSize) - 1;
	const buckets = new Array(MASK + 1).fill(zero);
	const lastBits = Math.floor((fieldN.BITS - 1) / windowSize) * windowSize;
	let sum = zero;
	for (let i = lastBits; i >= 0; i -= windowSize) {
		buckets.fill(zero);
		for (let j = 0; j < scalars.length; j++) {
			const scalar = scalars[j];
			const wbits = Number(scalar >> BigInt(i) & BigInt(MASK));
			buckets[wbits] = buckets[wbits].add(points[j]);
		}
		let resI = zero;
		for (let j = buckets.length - 1, sumI = zero; j > 0; j--) {
			sumI = sumI.add(buckets[j]);
			resI = resI.add(sumI);
		}
		sum = sum.add(resI);
		if (i !== 0) for (let j = 0; j < windowSize; j++) sum = sum.double();
	}
	return sum;
}
function validateBasic(curve) {
	validateField(curve.Fp);
	validateObject(curve, {
		n: "bigint",
		h: "bigint",
		Gx: "field",
		Gy: "field"
	}, {
		nBitLength: "isSafeInteger",
		nByteLength: "isSafeInteger"
	});
	return Object.freeze({
		...nLength(curve.n, curve.nBitLength),
		...curve,
		p: curve.Fp.ORDER
	});
}
// node_modules/@noble/curves/esm/abstract/edwards.js
/**
* Twisted Edwards curve. The formula is: ax² + y² = 1 + dx²y².
* For design rationale of types / exports, see weierstrass module documentation.
* @module
*/
/*! noble-curves - MIT License (c) 2022 Paul Miller (paulmillr.com) */
const _0n$1 = BigInt(0);
const _1n$2 = BigInt(1);
const _2n$1 = BigInt(2);
const _8n$1 = BigInt(8);
const VERIFY_DEFAULT = { zip215: true };
function validateOpts$1(curve) {
	const opts = validateBasic(curve);
	validateObject(curve, {
		hash: "function",
		a: "bigint",
		d: "bigint",
		randomBytes: "function"
	}, {
		adjustScalarBytes: "function",
		domain: "function",
		uvRatio: "function",
		mapToCurve: "function"
	});
	return Object.freeze({ ...opts });
}
/**
* Creates Twisted Edwards curve with EdDSA signatures.
* @example
* import { Field } from '@noble/curves/abstract/modular';
* // Before that, define BigInt-s: a, d, p, n, Gx, Gy, h
* const curve = twistedEdwards({ a, d, Fp: Field(p), n, Gx, Gy, h })
*/
function twistedEdwards(curveDef) {
	const CURVE = validateOpts$1(curveDef);
	const { Fp, n: CURVE_ORDER, prehash, hash: cHash, randomBytes, nByteLength, h: cofactor } = CURVE;
	const MASK = _2n$1 << BigInt(nByteLength * 8) - _1n$2;
	const modP = Fp.create;
	const Fn = Field(CURVE.n, CURVE.nBitLength);
	const uvRatio = CURVE.uvRatio || ((u, v) => {
		try {
			return {
				isValid: true,
				value: Fp.sqrt(u * Fp.inv(v))
			};
		} catch (e) {
			return {
				isValid: false,
				value: _0n$1
			};
		}
	});
	const adjustScalarBytes = CURVE.adjustScalarBytes || ((bytes) => bytes);
	const domain = CURVE.domain || ((data, ctx, phflag) => {
		abool("phflag", phflag);
		if (ctx.length || phflag) throw new Error("Contexts/pre-hash are not supported");
		return data;
	});
	function aCoordinate(title, n) {
		aInRange("coordinate " + title, n, _0n$1, MASK);
	}
	function assertPoint(other) {
		if (!(other instanceof Point)) throw new Error("ExtendedPoint expected");
	}
	const toAffineMemo = memoized((p, iz) => {
		const { ex: x, ey: y, ez: z } = p;
		const is0 = p.is0();
		if (iz == null) iz = is0 ? _8n$1 : Fp.inv(z);
		const ax = modP(x * iz);
		const ay = modP(y * iz);
		const zz = modP(z * iz);
		if (is0) return {
			x: _0n$1,
			y: _1n$2
		};
		if (zz !== _1n$2) throw new Error("invZ was invalid");
		return {
			x: ax,
			y: ay
		};
	});
	const assertValidMemo = memoized((p) => {
		const { a, d } = CURVE;
		if (p.is0()) throw new Error("bad point: ZERO");
		const { ex: X, ey: Y, ez: Z, et: T } = p;
		const X2 = modP(X * X);
		const Y2 = modP(Y * Y);
		const Z2 = modP(Z * Z);
		const Z4 = modP(Z2 * Z2);
		const aX2 = modP(X2 * a);
		if (modP(Z2 * modP(aX2 + Y2)) !== modP(Z4 + modP(d * modP(X2 * Y2)))) throw new Error("bad point: equation left != right (1)");
		if (modP(X * Y) !== modP(Z * T)) throw new Error("bad point: equation left != right (2)");
		return true;
	});
	class Point {
		constructor(ex, ey, ez, et) {
			this.ex = ex;
			this.ey = ey;
			this.ez = ez;
			this.et = et;
			aCoordinate("x", ex);
			aCoordinate("y", ey);
			aCoordinate("z", ez);
			aCoordinate("t", et);
			Object.freeze(this);
		}
		get x() {
			return this.toAffine().x;
		}
		get y() {
			return this.toAffine().y;
		}
		static fromAffine(p) {
			if (p instanceof Point) throw new Error("extended point not allowed");
			const { x, y } = p || {};
			aCoordinate("x", x);
			aCoordinate("y", y);
			return new Point(x, y, _1n$2, modP(x * y));
		}
		static normalizeZ(points) {
			const toInv = Fp.invertBatch(points.map((p) => p.ez));
			return points.map((p, i) => p.toAffine(toInv[i])).map(Point.fromAffine);
		}
		static msm(points, scalars) {
			return pippenger(Point, Fn, points, scalars);
		}
		_setWindowSize(windowSize) {
			wnaf.setWindowSize(this, windowSize);
		}
		assertValidity() {
			assertValidMemo(this);
		}
		equals(other) {
			assertPoint(other);
			const { ex: X1, ey: Y1, ez: Z1 } = this;
			const { ex: X2, ey: Y2, ez: Z2 } = other;
			const X1Z2 = modP(X1 * Z2);
			const X2Z1 = modP(X2 * Z1);
			const Y1Z2 = modP(Y1 * Z2);
			const Y2Z1 = modP(Y2 * Z1);
			return X1Z2 === X2Z1 && Y1Z2 === Y2Z1;
		}
		is0() {
			return this.equals(Point.ZERO);
		}
		negate() {
			return new Point(modP(-this.ex), this.ey, this.ez, modP(-this.et));
		}
		double() {
			const { a } = CURVE;
			const { ex: X1, ey: Y1, ez: Z1 } = this;
			const A = modP(X1 * X1);
			const B = modP(Y1 * Y1);
			const C = modP(_2n$1 * modP(Z1 * Z1));
			const D = modP(a * A);
			const x1y1 = X1 + Y1;
			const E = modP(modP(x1y1 * x1y1) - A - B);
			const G = D + B;
			const F = G - C;
			const H = D - B;
			const X3 = modP(E * F);
			const Y3 = modP(G * H);
			const T3 = modP(E * H);
			const Z3 = modP(F * G);
			return new Point(X3, Y3, Z3, T3);
		}
		add(other) {
			assertPoint(other);
			const { a, d } = CURVE;
			const { ex: X1, ey: Y1, ez: Z1, et: T1 } = this;
			const { ex: X2, ey: Y2, ez: Z2, et: T2 } = other;
			if (a === BigInt(-1)) {
				const A = modP((Y1 - X1) * (Y2 + X2));
				const B = modP((Y1 + X1) * (Y2 - X2));
				const F = modP(B - A);
				if (F === _0n$1) return this.double();
				const C = modP(Z1 * _2n$1 * T2);
				const D = modP(T1 * _2n$1 * Z2);
				const E = D + C;
				const G = B + A;
				const H = D - C;
				const X3 = modP(E * F);
				const Y3 = modP(G * H);
				const T3 = modP(E * H);
				const Z3 = modP(F * G);
				return new Point(X3, Y3, Z3, T3);
			}
			const A = modP(X1 * X2);
			const B = modP(Y1 * Y2);
			const C = modP(T1 * d * T2);
			const D = modP(Z1 * Z2);
			const E = modP((X1 + Y1) * (X2 + Y2) - A - B);
			const F = D - C;
			const G = D + C;
			const H = modP(B - a * A);
			const X3 = modP(E * F);
			const Y3 = modP(G * H);
			const T3 = modP(E * H);
			const Z3 = modP(F * G);
			return new Point(X3, Y3, Z3, T3);
		}
		subtract(other) {
			return this.add(other.negate());
		}
		wNAF(n) {
			return wnaf.wNAFCached(this, n, Point.normalizeZ);
		}
		multiply(scalar) {
			const n = scalar;
			aInRange("scalar", n, _1n$2, CURVE_ORDER);
			const { p, f } = this.wNAF(n);
			return Point.normalizeZ([p, f])[0];
		}
		multiplyUnsafe(scalar, acc = Point.ZERO) {
			const n = scalar;
			aInRange("scalar", n, _0n$1, CURVE_ORDER);
			if (n === _0n$1) return I;
			if (this.is0() || n === _1n$2) return this;
			return wnaf.wNAFCachedUnsafe(this, n, Point.normalizeZ, acc);
		}
		isSmallOrder() {
			return this.multiplyUnsafe(cofactor).is0();
		}
		isTorsionFree() {
			return wnaf.unsafeLadder(this, CURVE_ORDER).is0();
		}
		toAffine(iz) {
			return toAffineMemo(this, iz);
		}
		clearCofactor() {
			const { h: cofactor } = CURVE;
			if (cofactor === _1n$2) return this;
			return this.multiplyUnsafe(cofactor);
		}
		static fromHex(hex, zip215 = false) {
			const { d, a } = CURVE;
			const len = Fp.BYTES;
			hex = ensureBytes("pointHex", hex, len);
			abool("zip215", zip215);
			const normed = hex.slice();
			const lastByte = hex[len - 1];
			normed[len - 1] = lastByte & -129;
			const y = bytesToNumberLE(normed);
			const max = zip215 ? MASK : Fp.ORDER;
			aInRange("pointHex.y", y, _0n$1, max);
			const y2 = modP(y * y);
			const u = modP(y2 - _1n$2);
			const v = modP(d * y2 - a);
			let { isValid, value: x } = uvRatio(u, v);
			if (!isValid) throw new Error("Point.fromHex: invalid y coordinate");
			const isXOdd = (x & _1n$2) === _1n$2;
			const isLastByteOdd = (lastByte & 128) !== 0;
			if (!zip215 && x === _0n$1 && isLastByteOdd) throw new Error("Point.fromHex: x=0 and x_0=1");
			if (isLastByteOdd !== isXOdd) x = modP(-x);
			return Point.fromAffine({
				x,
				y
			});
		}
		static fromPrivateKey(privKey) {
			return getExtendedPublicKey(privKey).point;
		}
		toRawBytes() {
			const { x, y } = this.toAffine();
			const bytes = numberToBytesLE(y, Fp.BYTES);
			bytes[bytes.length - 1] |= x & _1n$2 ? 128 : 0;
			return bytes;
		}
		toHex() {
			return bytesToHex$1(this.toRawBytes());
		}
	}
	Point.BASE = new Point(CURVE.Gx, CURVE.Gy, _1n$2, modP(CURVE.Gx * CURVE.Gy));
	Point.ZERO = new Point(_0n$1, _1n$2, _1n$2, _0n$1);
	const { BASE: G, ZERO: I } = Point;
	const wnaf = wNAF(Point, nByteLength * 8);
	function modN(a) {
		return mod(a, CURVE_ORDER);
	}
	function modN_LE(hash) {
		return modN(bytesToNumberLE(hash));
	}
	/** Convenience method that creates public key and other stuff. RFC8032 5.1.5 */
	function getExtendedPublicKey(key) {
		const len = Fp.BYTES;
		key = ensureBytes("private key", key, len);
		const hashed = ensureBytes("hashed private key", cHash(key), 2 * len);
		const head = adjustScalarBytes(hashed.slice(0, len));
		const prefix = hashed.slice(len, 2 * len);
		const scalar = modN_LE(head);
		const point = G.multiply(scalar);
		return {
			head,
			prefix,
			scalar,
			point,
			pointBytes: point.toRawBytes()
		};
	}
	function getPublicKey(privKey) {
		return getExtendedPublicKey(privKey).pointBytes;
	}
	function hashDomainToScalar(context = /* @__PURE__ */ new Uint8Array(), ...msgs) {
		const msg = concatBytes(...msgs);
		return modN_LE(cHash(domain(msg, ensureBytes("context", context), !!prehash)));
	}
	/** Signs message with privateKey. RFC8032 5.1.6 */
	function sign(msg, privKey, options = {}) {
		msg = ensureBytes("message", msg);
		if (prehash) msg = prehash(msg);
		const { prefix, scalar, pointBytes } = getExtendedPublicKey(privKey);
		const r = hashDomainToScalar(options.context, prefix, msg);
		const R = G.multiply(r).toRawBytes();
		const s = modN(r + hashDomainToScalar(options.context, R, pointBytes, msg) * scalar);
		aInRange("signature.s", s, _0n$1, CURVE_ORDER);
		return ensureBytes("result", concatBytes(R, numberToBytesLE(s, Fp.BYTES)), Fp.BYTES * 2);
	}
	const verifyOpts = VERIFY_DEFAULT;
	/**
	* Verifies EdDSA signature against message and public key. RFC8032 5.1.7.
	* An extended group equation is checked.
	*/
	function verify(sig, msg, publicKey, options = verifyOpts) {
		const { context, zip215 } = options;
		const len = Fp.BYTES;
		sig = ensureBytes("signature", sig, 2 * len);
		msg = ensureBytes("message", msg);
		publicKey = ensureBytes("publicKey", publicKey, len);
		if (zip215 !== void 0) abool("zip215", zip215);
		if (prehash) msg = prehash(msg);
		const s = bytesToNumberLE(sig.slice(len, 2 * len));
		let A, R, SB;
		try {
			A = Point.fromHex(publicKey, zip215);
			R = Point.fromHex(sig.slice(0, len), zip215);
			SB = G.multiplyUnsafe(s);
		} catch (error) {
			return false;
		}
		if (!zip215 && A.isSmallOrder()) return false;
		const k = hashDomainToScalar(context, R.toRawBytes(), A.toRawBytes(), msg);
		return R.add(A.multiplyUnsafe(k)).subtract(SB).clearCofactor().equals(Point.ZERO);
	}
	G._setWindowSize(8);
	return {
		CURVE,
		getPublicKey,
		sign,
		verify,
		ExtendedPoint: Point,
		utils: {
			getExtendedPublicKey,
			randomPrivateKey: () => randomBytes(Fp.BYTES),
			/**
			* We're doing scalar multiplication (used in getPublicKey etc) with precomputed BASE_POINT
			* values. This slows down first getPublicKey() by milliseconds (see Speed section),
			* but allows to speed-up subsequent getPublicKey() calls up to 20x.
			* @param windowSize 2, 4, 8, 16
			*/
			precompute(windowSize = 8, point = Point.BASE) {
				point._setWindowSize(windowSize);
				point.multiply(BigInt(3));
				return point;
			}
		}
	};
}
// node_modules/@noble/curves/esm/abstract/montgomery.js
/**
* Montgomery curve methods. It's not really whole montgomery curve,
* just bunch of very specific methods for X25519 / X448 from
* [RFC 7748](https://www.rfc-editor.org/rfc/rfc7748)
* @module
*/
/*! noble-curves - MIT License (c) 2022 Paul Miller (paulmillr.com) */
const _0n = BigInt(0);
const _1n$1 = BigInt(1);
function validateOpts(curve) {
	validateObject(curve, { a: "bigint" }, {
		montgomeryBits: "isSafeInteger",
		nByteLength: "isSafeInteger",
		adjustScalarBytes: "function",
		domain: "function",
		powPminus2: "function",
		Gu: "bigint"
	});
	return Object.freeze({ ...curve });
}
function montgomery(curveDef) {
	const CURVE = validateOpts(curveDef);
	const { P } = CURVE;
	const modP = (n) => mod(n, P);
	const montgomeryBits = CURVE.montgomeryBits;
	const montgomeryBytes = Math.ceil(montgomeryBits / 8);
	const fieldLen = CURVE.nByteLength;
	const adjustScalarBytes = CURVE.adjustScalarBytes || ((bytes) => bytes);
	const powPminus2 = CURVE.powPminus2 || ((x) => pow(x, P - BigInt(2), P));
	function cswap(swap, x_2, x_3) {
		const dummy = modP(swap * (x_2 - x_3));
		x_2 = modP(x_2 - dummy);
		x_3 = modP(x_3 + dummy);
		return [x_2, x_3];
	}
	const a24 = (CURVE.a - BigInt(2)) / BigInt(4);
	/**
	*
	* @param pointU u coordinate (x) on Montgomery Curve 25519
	* @param scalar by which the point would be multiplied
	* @returns new Point on Montgomery curve
	*/
	function montgomeryLadder(u, scalar) {
		aInRange("u", u, _0n, P);
		aInRange("scalar", scalar, _0n, P);
		const k = scalar;
		const x_1 = u;
		let x_2 = _1n$1;
		let z_2 = _0n;
		let x_3 = u;
		let z_3 = _1n$1;
		let swap = _0n;
		let sw;
		for (let t = BigInt(montgomeryBits - 1); t >= _0n; t--) {
			const k_t = k >> t & _1n$1;
			swap ^= k_t;
			sw = cswap(swap, x_2, x_3);
			x_2 = sw[0];
			x_3 = sw[1];
			sw = cswap(swap, z_2, z_3);
			z_2 = sw[0];
			z_3 = sw[1];
			swap = k_t;
			const A = x_2 + z_2;
			const AA = modP(A * A);
			const B = x_2 - z_2;
			const BB = modP(B * B);
			const E = AA - BB;
			const C = x_3 + z_3;
			const D = x_3 - z_3;
			const DA = modP(D * A);
			const CB = modP(C * B);
			const dacb = DA + CB;
			const da_cb = DA - CB;
			x_3 = modP(dacb * dacb);
			z_3 = modP(x_1 * modP(da_cb * da_cb));
			x_2 = modP(AA * BB);
			z_2 = modP(E * (AA + modP(a24 * E)));
		}
		sw = cswap(swap, x_2, x_3);
		x_2 = sw[0];
		x_3 = sw[1];
		sw = cswap(swap, z_2, z_3);
		z_2 = sw[0];
		z_3 = sw[1];
		const z2 = powPminus2(z_2);
		return modP(x_2 * z2);
	}
	function encodeUCoordinate(u) {
		return numberToBytesLE(modP(u), montgomeryBytes);
	}
	function decodeUCoordinate(uEnc) {
		const u = ensureBytes("u coordinate", uEnc, montgomeryBytes);
		if (fieldLen === 32) u[31] &= 127;
		return bytesToNumberLE(u);
	}
	function decodeScalar(n) {
		const bytes = ensureBytes("scalar", n);
		const len = bytes.length;
		if (len !== montgomeryBytes && len !== fieldLen) {
			let valid = "" + montgomeryBytes + " or " + fieldLen;
			throw new Error("invalid scalar, expected " + valid + " bytes, got " + len);
		}
		return bytesToNumberLE(adjustScalarBytes(bytes));
	}
	function scalarMult(scalar, u) {
		const pu = montgomeryLadder(decodeUCoordinate(u), decodeScalar(scalar));
		if (pu === _0n) throw new Error("invalid private or public key received");
		return encodeUCoordinate(pu);
	}
	const GuBytes = encodeUCoordinate(CURVE.Gu);
	function scalarMultBase(scalar) {
		return scalarMult(scalar, GuBytes);
	}
	return {
		scalarMult,
		scalarMultBase,
		getSharedSecret: (privateKey, publicKey) => scalarMult(privateKey, publicKey),
		getPublicKey: (privateKey) => scalarMultBase(privateKey),
		utils: { randomPrivateKey: () => CURVE.randomBytes(CURVE.nByteLength) },
		GuBytes
	};
}
// node_modules/@noble/curves/esm/ed25519.js
/**
* ed25519 Twisted Edwards curve with following addons:
* - X25519 ECDH
* - Ristretto cofactor elimination
* - Elligator hash-to-group / point indistinguishability
* @module
*/
/*! noble-curves - MIT License (c) 2022 Paul Miller (paulmillr.com) */
const ED25519_P = BigInt("57896044618658097711785492504343953926634992332820282019728792003956564819949");
const ED25519_SQRT_M1 = /* @__PURE__ */ BigInt("19681161376707505956807079304988542015446066515923890162744021073123829784752");
const _1n = BigInt(1);
const _2n = BigInt(2);
const _3n = BigInt(3);
const _5n = BigInt(5);
const _8n = BigInt(8);
function ed25519_pow_2_252_3(x) {
	const _10n = BigInt(10), _20n = BigInt(20), _40n = BigInt(40), _80n = BigInt(80);
	const P = ED25519_P;
	const b2 = x * x % P * x % P;
	const b5 = pow2(pow2(b2, _2n, P) * b2 % P, _1n, P) * x % P;
	const b10 = pow2(b5, _5n, P) * b5 % P;
	const b20 = pow2(b10, _10n, P) * b10 % P;
	const b40 = pow2(b20, _20n, P) * b20 % P;
	const b80 = pow2(b40, _40n, P) * b40 % P;
	return {
		pow_p_5_8: pow2(pow2(pow2(pow2(b80, _80n, P) * b80 % P, _80n, P) * b80 % P, _10n, P) * b10 % P, _2n, P) * x % P,
		b2
	};
}
function adjustScalarBytes(bytes) {
	bytes[0] &= 248;
	bytes[31] &= 127;
	bytes[31] |= 64;
	return bytes;
}
function uvRatio(u, v) {
	const P = ED25519_P;
	const v3 = mod(v * v * v, P);
	const pow = ed25519_pow_2_252_3(u * mod(v3 * v3 * v, P)).pow_p_5_8;
	let x = mod(u * v3 * pow, P);
	const vx2 = mod(v * x * x, P);
	const root1 = x;
	const root2 = mod(x * ED25519_SQRT_M1, P);
	const useRoot1 = vx2 === u;
	const useRoot2 = vx2 === mod(-u, P);
	const noRoot = vx2 === mod(-u * ED25519_SQRT_M1, P);
	if (useRoot1) x = root1;
	if (useRoot2 || noRoot) x = root2;
	if (isNegativeLE(x, P)) x = mod(-x, P);
	return {
		isValid: useRoot1 || useRoot2,
		value: x
	};
}
const Fp = /* @__PURE__ */ (() => Field(ED25519_P, void 0, true))();
const ed25519Defaults = /* @__PURE__ */ (() => ({
	a: BigInt(-1),
	d: BigInt("37095705934669439343138083508754565189542113879843219016388785533085940283555"),
	Fp,
	n: BigInt("7237005577332262213973186563042994240857116359379907606001950938285454250989"),
	h: _8n,
	Gx: BigInt("15112221349535400772501151409588531511454012693041857206046113283949847762202"),
	Gy: BigInt("46316835694926478169428394003475163141307993866256225615783033603165251855960"),
	hash: sha512,
	randomBytes,
	adjustScalarBytes,
	uvRatio
}))();
/**
* ed25519 curve with EdDSA signatures.
* @example
* import { ed25519 } from '@noble/curves/ed25519';
* const priv = ed25519.utils.randomPrivateKey();
* const pub = ed25519.getPublicKey(priv);
* const msg = new TextEncoder().encode('hello');
* const sig = ed25519.sign(msg, priv);
* ed25519.verify(sig, msg, pub); // Default mode: follows ZIP215
* ed25519.verify(sig, msg, pub, { zip215: false }); // RFC8032 / FIPS 186-5
*/
const ed25519 = /* @__PURE__ */ (() => twistedEdwards(ed25519Defaults))();
/**
* ECDH using curve25519 aka x25519.
* @example
* import { x25519 } from '@noble/curves/ed25519';
* const priv = 'a546e36bf0527c9d3b16154b82465edd62144c0ac1fc5a18506a2244ba449ac4';
* const pub = 'e6db6867583030db3594c1a424b15f7c726624ec26b3353b10a903a6d0ab1c4c';
* x25519.getSharedSecret(priv, pub) === x25519.scalarMult(priv, pub); // aliases
* x25519.getPublicKey(priv) === x25519.scalarMultBase(priv);
* x25519.getPublicKey(x25519.utils.randomPrivateKey());
*/
const x25519 = /* @__PURE__ */ (() => montgomery({
	P: ED25519_P,
	a: BigInt(486662),
	montgomeryBits: 255,
	nByteLength: 32,
	Gu: BigInt(9),
	powPminus2: (x) => {
		const P = ED25519_P;
		const { pow_p_5_8, b2 } = ed25519_pow_2_252_3(x);
		return mod(pow2(pow_p_5_8, _3n, P) * b2, P);
	},
	adjustScalarBytes,
	randomBytes
}))();
// node_modules/@noble/hashes/esm/hmac.js
/**
* HMAC: RFC2104 message authentication code.
* @module
*/
var HMAC = class extends Hash {
	constructor(hash, _key) {
		super();
		this.finished = false;
		this.destroyed = false;
		ahash(hash);
		const key = toBytes$1(_key);
		this.iHash = hash.create();
		if (typeof this.iHash.update !== "function") throw new Error("Expected instance of class which extends utils.Hash");
		this.blockLen = this.iHash.blockLen;
		this.outputLen = this.iHash.outputLen;
		const blockLen = this.blockLen;
		const pad = new Uint8Array(blockLen);
		pad.set(key.length > blockLen ? hash.create().update(key).digest() : key);
		for (let i = 0; i < pad.length; i++) pad[i] ^= 54;
		this.iHash.update(pad);
		this.oHash = hash.create();
		for (let i = 0; i < pad.length; i++) pad[i] ^= 106;
		this.oHash.update(pad);
		pad.fill(0);
	}
	update(buf) {
		aexists(this);
		this.iHash.update(buf);
		return this;
	}
	digestInto(out) {
		aexists(this);
		abytes$1(out, this.outputLen);
		this.finished = true;
		this.iHash.digestInto(out);
		this.oHash.update(out);
		this.oHash.digestInto(out);
		this.destroy();
	}
	digest() {
		const out = new Uint8Array(this.oHash.outputLen);
		this.digestInto(out);
		return out;
	}
	_cloneInto(to) {
		to || (to = Object.create(Object.getPrototypeOf(this), {}));
		const { oHash, iHash, finished, destroyed, blockLen, outputLen } = this;
		to = to;
		to.finished = finished;
		to.destroyed = destroyed;
		to.blockLen = blockLen;
		to.outputLen = outputLen;
		to.oHash = oHash._cloneInto(to.oHash);
		to.iHash = iHash._cloneInto(to.iHash);
		return to;
	}
	destroy() {
		this.destroyed = true;
		this.oHash.destroy();
		this.iHash.destroy();
	}
};
/**
* HMAC: RFC2104 message authentication code.
* @param hash - function that would be used e.g. sha256
* @param key - message key
* @param message - message data
* @example
* import { hmac } from '@noble/hashes/hmac';
* import { sha256 } from '@noble/hashes/sha2';
* const mac1 = hmac(sha256, 'key', 'message');
*/
const hmac = (hash, key, message) => new HMAC(hash, key).update(message).digest();
hmac.create = (hash, key) => new HMAC(hash, key);
// node_modules/@noble/hashes/esm/hkdf.js
/**
* HKDF (RFC 5869): extract + expand in one step.
* See https://soatok.blog/2021/11/17/understanding-hkdf/.
* @module
*/
/**
* HKDF-extract from spec. Less important part. `HKDF-Extract(IKM, salt) -> PRK`
* Arguments position differs from spec (IKM is first one, since it is not optional)
* @param hash - hash function that would be used (e.g. sha256)
* @param ikm - input keying material, the initial key
* @param salt - optional salt value (a non-secret random value)
*/
function extract(hash, ikm, salt) {
	ahash(hash);
	if (salt === void 0) salt = new Uint8Array(hash.outputLen);
	return hmac(hash, toBytes$1(salt), toBytes$1(ikm));
}
const HKDF_COUNTER = /* @__PURE__ */ new Uint8Array([0]);
const EMPTY_BUFFER = /* @__PURE__ */ new Uint8Array();
/**
* HKDF-expand from the spec. The most important part. `HKDF-Expand(PRK, info, L) -> OKM`
* @param hash - hash function that would be used (e.g. sha256)
* @param prk - a pseudorandom key of at least HashLen octets (usually, the output from the extract step)
* @param info - optional context and application specific information (can be a zero-length string)
* @param length - length of output keying material in bytes
*/
function expand(hash, prk, info, length = 32) {
	ahash(hash);
	anumber$1(length);
	if (length > 255 * hash.outputLen) throw new Error("Length should be <= 255*HashLen");
	const blocks = Math.ceil(length / hash.outputLen);
	if (info === void 0) info = EMPTY_BUFFER;
	const okm = new Uint8Array(blocks * hash.outputLen);
	const HMAC = hmac.create(hash, prk);
	const HMACTmp = HMAC._cloneInto();
	const T = new Uint8Array(HMAC.outputLen);
	for (let counter = 0; counter < blocks; counter++) {
		HKDF_COUNTER[0] = counter + 1;
		HMACTmp.update(counter === 0 ? EMPTY_BUFFER : T).update(info).update(HKDF_COUNTER).digestInto(T);
		okm.set(T, hash.outputLen * counter);
		HMAC._cloneInto(HMACTmp);
	}
	HMAC.destroy();
	HMACTmp.destroy();
	T.fill(0);
	HKDF_COUNTER.fill(0);
	return okm.slice(0, length);
}
/**
* HKDF (RFC 5869): derive keys from an initial input.
* Combines hkdf_extract + hkdf_expand in one step
* @param hash - hash function that would be used (e.g. sha256)
* @param ikm - input keying material, the initial key
* @param salt - optional salt value (a non-secret random value)
* @param info - optional context and application specific information (can be a zero-length string)
* @param length - length of output keying material in bytes
* @example
* import { hkdf } from '@noble/hashes/hkdf';
* import { sha256 } from '@noble/hashes/sha2';
* import { randomBytes } from '@noble/hashes/utils';
* const inputKey = randomBytes(32);
* const salt = randomBytes(32);
* const info = 'application-key';
* const hk1 = hkdf(sha256, inputKey, salt, info, 32);
*/
const hkdf = (hash, ikm, salt, info, length) => expand(hash, extract(hash, ikm, salt), info, length);
// node_modules/@noble/hashes/esm/sha256.js
/**
* SHA2-256 a.k.a. sha256. In JS, it is the fastest hash, even faster than Blake3.
*
* To break sha256 using birthday attack, attackers need to try 2^128 hashes.
* BTC network is doing 2^70 hashes/sec (2^95 hashes/year) as per 2025.
*
* Check out [FIPS 180-4](https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.180-4.pdf).
* @module
*/
/** Round constants: first 32 bits of fractional parts of the cube roots of the first 64 primes 2..311). */
const SHA256_K = /* @__PURE__ */ new Uint32Array([
	1116352408,
	1899447441,
	3049323471,
	3921009573,
	961987163,
	1508970993,
	2453635748,
	2870763221,
	3624381080,
	310598401,
	607225278,
	1426881987,
	1925078388,
	2162078206,
	2614888103,
	3248222580,
	3835390401,
	4022224774,
	264347078,
	604807628,
	770255983,
	1249150122,
	1555081692,
	1996064986,
	2554220882,
	2821834349,
	2952996808,
	3210313671,
	3336571891,
	3584528711,
	113926993,
	338241895,
	666307205,
	773529912,
	1294757372,
	1396182291,
	1695183700,
	1986661051,
	2177026350,
	2456956037,
	2730485921,
	2820302411,
	3259730800,
	3345764771,
	3516065817,
	3600352804,
	4094571909,
	275423344,
	430227734,
	506948616,
	659060556,
	883997877,
	958139571,
	1322822218,
	1537002063,
	1747873779,
	1955562222,
	2024104815,
	2227730452,
	2361852424,
	2428436474,
	2756734187,
	3204031479,
	3329325298
]);
/** Initial state: first 32 bits of fractional parts of the square roots of the first 8 primes 2..19. */
const SHA256_IV = /* @__PURE__ */ new Uint32Array([
	1779033703,
	3144134277,
	1013904242,
	2773480762,
	1359893119,
	2600822924,
	528734635,
	1541459225
]);
/**
* Temporary buffer, not used to store anything between runs.
* Named this way because it matches specification.
*/
const SHA256_W = /* @__PURE__ */ new Uint32Array(64);
var SHA256 = class extends HashMD {
	constructor() {
		super(64, 32, 8, false);
		this.A = SHA256_IV[0] | 0;
		this.B = SHA256_IV[1] | 0;
		this.C = SHA256_IV[2] | 0;
		this.D = SHA256_IV[3] | 0;
		this.E = SHA256_IV[4] | 0;
		this.F = SHA256_IV[5] | 0;
		this.G = SHA256_IV[6] | 0;
		this.H = SHA256_IV[7] | 0;
	}
	get() {
		const { A, B, C, D, E, F, G, H } = this;
		return [
			A,
			B,
			C,
			D,
			E,
			F,
			G,
			H
		];
	}
	set(A, B, C, D, E, F, G, H) {
		this.A = A | 0;
		this.B = B | 0;
		this.C = C | 0;
		this.D = D | 0;
		this.E = E | 0;
		this.F = F | 0;
		this.G = G | 0;
		this.H = H | 0;
	}
	process(view, offset) {
		for (let i = 0; i < 16; i++, offset += 4) SHA256_W[i] = view.getUint32(offset, false);
		for (let i = 16; i < 64; i++) {
			const W15 = SHA256_W[i - 15];
			const W2 = SHA256_W[i - 2];
			const s0 = rotr(W15, 7) ^ rotr(W15, 18) ^ W15 >>> 3;
			const s1 = rotr(W2, 17) ^ rotr(W2, 19) ^ W2 >>> 10;
			SHA256_W[i] = s1 + SHA256_W[i - 7] + s0 + SHA256_W[i - 16] | 0;
		}
		let { A, B, C, D, E, F, G, H } = this;
		for (let i = 0; i < 64; i++) {
			const sigma1 = rotr(E, 6) ^ rotr(E, 11) ^ rotr(E, 25);
			const T1 = H + sigma1 + Chi(E, F, G) + SHA256_K[i] + SHA256_W[i] | 0;
			const T2 = (rotr(A, 2) ^ rotr(A, 13) ^ rotr(A, 22)) + Maj(A, B, C) | 0;
			H = G;
			G = F;
			F = E;
			E = D + T1 | 0;
			D = C;
			C = B;
			B = A;
			A = T1 + T2 | 0;
		}
		A = A + this.A | 0;
		B = B + this.B | 0;
		C = C + this.C | 0;
		D = D + this.D | 0;
		E = E + this.E | 0;
		F = F + this.F | 0;
		G = G + this.G | 0;
		H = H + this.H | 0;
		this.set(A, B, C, D, E, F, G, H);
	}
	roundClean() {
		SHA256_W.fill(0);
	}
	destroy() {
		this.set(0, 0, 0, 0, 0, 0, 0, 0);
		this.buffer.fill(0);
	}
};
/** SHA2-256 hash function */
const sha256 = /* @__PURE__ */ wrapConstructor(() => new SHA256());
// node_modules/@scure/base/lib/esm/index.js
/*! scure-base - MIT License (c) 2022 Paul Miller (paulmillr.com) */
function isBytes(a) {
	return a instanceof Uint8Array || ArrayBuffer.isView(a) && a.constructor.name === "Uint8Array";
}
function isArrayOf(isString, arr) {
	if (!Array.isArray(arr)) return false;
	if (arr.length === 0) return true;
	if (isString) return arr.every((item) => typeof item === "string");
	else return arr.every((item) => Number.isSafeInteger(item));
}
function afn(input) {
	if (typeof input !== "function") throw new Error("function expected");
	return true;
}
function astr(label, input) {
	if (typeof input !== "string") throw new Error(`${label}: string expected`);
	return true;
}
function anumber(n) {
	if (!Number.isSafeInteger(n)) throw new Error(`invalid integer: ${n}`);
}
function aArr(input) {
	if (!Array.isArray(input)) throw new Error("array expected");
}
function astrArr(label, input) {
	if (!isArrayOf(true, input)) throw new Error(`${label}: array of strings expected`);
}
function anumArr(label, input) {
	if (!isArrayOf(false, input)) throw new Error(`${label}: array of numbers expected`);
}
/**
* @__NO_SIDE_EFFECTS__
*/
function chain(...args) {
	const id = (a) => a;
	const wrap = (a, b) => (c) => a(b(c));
	return {
		encode: args.map((x) => x.encode).reduceRight(wrap, id),
		decode: args.map((x) => x.decode).reduce(wrap, id)
	};
}
/**
* Encodes integer radix representation to array of strings using alphabet and back.
* Could also be array of strings.
* @__NO_SIDE_EFFECTS__
*/
function alphabet(letters) {
	const lettersA = typeof letters === "string" ? letters.split("") : letters;
	const len = lettersA.length;
	astrArr("alphabet", lettersA);
	const indexes = new Map(lettersA.map((l, i) => [l, i]));
	return {
		encode: (digits) => {
			aArr(digits);
			return digits.map((i) => {
				if (!Number.isSafeInteger(i) || i < 0 || i >= len) throw new Error(`alphabet.encode: digit index outside alphabet "${i}". Allowed: ${letters}`);
				return lettersA[i];
			});
		},
		decode: (input) => {
			aArr(input);
			return input.map((letter) => {
				astr("alphabet.decode", letter);
				const i = indexes.get(letter);
				if (i === void 0) throw new Error(`Unknown letter: "${letter}". Allowed: ${letters}`);
				return i;
			});
		}
	};
}
/**
* @__NO_SIDE_EFFECTS__
*/
function join(separator = "") {
	astr("join", separator);
	return {
		encode: (from) => {
			astrArr("join.decode", from);
			return from.join(separator);
		},
		decode: (to) => {
			astr("join.decode", to);
			return to.split(separator);
		}
	};
}
/**
* Pad strings array so it has integer number of bits
* @__NO_SIDE_EFFECTS__
*/
function padding(bits, chr = "=") {
	anumber(bits);
	astr("padding", chr);
	return {
		encode(data) {
			astrArr("padding.encode", data);
			while (data.length * bits % 8) data.push(chr);
			return data;
		},
		decode(input) {
			astrArr("padding.decode", input);
			let end = input.length;
			if (end * bits % 8) throw new Error("padding: invalid, string should have whole number of bytes");
			for (; end > 0 && input[end - 1] === chr; end--) if ((end - 1) * bits % 8 === 0) throw new Error("padding: invalid, string has too much padding");
			return input.slice(0, end);
		}
	};
}
/**
* @__NO_SIDE_EFFECTS__
*/
function normalize(fn) {
	afn(fn);
	return {
		encode: (from) => from,
		decode: (to) => fn(to)
	};
}
const gcd = (a, b) => b === 0 ? a : gcd(b, a % b);
const radix2carry = /* @__NO_SIDE_EFFECTS__ */ (from, to) => from + (to - gcd(from, to));
const powers = /* @__PURE__ */ (() => {
	let res = [];
	for (let i = 0; i < 40; i++) res.push(2 ** i);
	return res;
})();
/**
* Implemented with numbers, because BigInt is 5x slower
*/
function convertRadix2(data, from, to, padding) {
	aArr(data);
	if (from <= 0 || from > 32) throw new Error(`convertRadix2: wrong from=${from}`);
	if (to <= 0 || to > 32) throw new Error(`convertRadix2: wrong to=${to}`);
	if (/* @__PURE__ */ radix2carry(from, to) > 32) throw new Error(`convertRadix2: carry overflow from=${from} to=${to} carryBits=${/* @__PURE__ */ radix2carry(from, to)}`);
	let carry = 0;
	let pos = 0;
	const max = powers[from];
	const mask = powers[to] - 1;
	const res = [];
	for (const n of data) {
		anumber(n);
		if (n >= max) throw new Error(`convertRadix2: invalid data word=${n} from=${from}`);
		carry = carry << from | n;
		if (pos + from > 32) throw new Error(`convertRadix2: carry overflow pos=${pos} from=${from}`);
		pos += from;
		for (; pos >= to; pos -= to) res.push((carry >> pos - to & mask) >>> 0);
		const pow = powers[pos];
		if (pow === void 0) throw new Error("invalid carry");
		carry &= pow - 1;
	}
	carry = carry << to - pos & mask;
	if (!padding && pos >= from) throw new Error("Excess padding");
	if (!padding && carry > 0) throw new Error(`Non-zero padding: ${carry}`);
	if (padding && pos > 0) res.push(carry >>> 0);
	return res;
}
/**
* If both bases are power of same number (like `2**8 <-> 2**64`),
* there is a linear algorithm. For now we have implementation for power-of-two bases only.
* @__NO_SIDE_EFFECTS__
*/
function radix2(bits, revPadding = false) {
	anumber(bits);
	if (bits <= 0 || bits > 32) throw new Error("radix2: bits should be in (0..32]");
	if (/* @__PURE__ */ radix2carry(8, bits) > 32 || /* @__PURE__ */ radix2carry(bits, 8) > 32) throw new Error("radix2: carry overflow");
	return {
		encode: (bytes) => {
			if (!isBytes(bytes)) throw new Error("radix2.encode input should be Uint8Array");
			return convertRadix2(Array.from(bytes), 8, bits, !revPadding);
		},
		decode: (digits) => {
			anumArr("radix2.decode", digits);
			return Uint8Array.from(convertRadix2(digits, bits, 8, revPadding));
		}
	};
}
function unsafeWrapper(fn) {
	afn(fn);
	return function(...args) {
		try {
			return fn.apply(null, args);
		} catch (e) {}
	};
}
chain(radix2(4), alphabet("0123456789ABCDEF"), join(""));
chain(radix2(5), alphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"), padding(5), join(""));
chain(radix2(5), alphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"), join(""));
chain(radix2(5), alphabet("0123456789ABCDEFGHIJKLMNOPQRSTUV"), padding(5), join(""));
chain(radix2(5), alphabet("0123456789ABCDEFGHIJKLMNOPQRSTUV"), join(""));
chain(radix2(5), alphabet("0123456789ABCDEFGHJKMNPQRSTVWXYZ"), join(""), normalize((s) => s.toUpperCase().replace(/O/g, "0").replace(/[IL]/g, "1")));
/**
* base64 with padding. For no padding, use `base64nopad`.
* @example
* const b = base64.decode('A951'); // Uint8Array.from([ 3, 222, 117 ])
* base64.encode(b); // 'A951'
*/
const base64 = chain(radix2(6), alphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"), padding(6), join(""));
chain(radix2(6), alphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"), join(""));
const base64url = chain(radix2(6), alphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"), padding(6), join(""));
chain(radix2(6), alphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"), join(""));
const BECH_ALPHABET = chain(alphabet("qpzry9x8gf2tvdw0s3jn54khce6mua7l"), join(""));
const POLYMOD_GENERATORS = [
	996825010,
	642813549,
	513874426,
	1027748829,
	705979059
];
function bech32Polymod(pre) {
	const b = pre >> 25;
	let chk = (pre & 33554431) << 5;
	for (let i = 0; i < POLYMOD_GENERATORS.length; i++) if ((b >> i & 1) === 1) chk ^= POLYMOD_GENERATORS[i];
	return chk;
}
function bechChecksum(prefix, words, encodingConst = 1) {
	const len = prefix.length;
	let chk = 1;
	for (let i = 0; i < len; i++) {
		const c = prefix.charCodeAt(i);
		if (c < 33 || c > 126) throw new Error(`Invalid prefix (${prefix})`);
		chk = bech32Polymod(chk) ^ c >> 5;
	}
	chk = bech32Polymod(chk);
	for (let i = 0; i < len; i++) chk = bech32Polymod(chk) ^ prefix.charCodeAt(i) & 31;
	for (let v of words) chk = bech32Polymod(chk) ^ v;
	for (let i = 0; i < 6; i++) chk = bech32Polymod(chk);
	chk ^= encodingConst;
	return BECH_ALPHABET.encode(convertRadix2([chk % powers[30]], 30, 5, false));
}
/**
* @__NO_SIDE_EFFECTS__
*/
function genBech32(encoding) {
	const ENCODING_CONST = encoding === "bech32" ? 1 : 734539939;
	const _words = radix2(5);
	const fromWords = _words.decode;
	const toWords = _words.encode;
	const fromWordsUnsafe = unsafeWrapper(fromWords);
	function encode(prefix, words, limit = 90) {
		astr("bech32.encode prefix", prefix);
		if (isBytes(words)) words = Array.from(words);
		anumArr("bech32.encode", words);
		const plen = prefix.length;
		if (plen === 0) throw new TypeError(`Invalid prefix length ${plen}`);
		const actualLength = plen + 7 + words.length;
		if (limit !== false && actualLength > limit) throw new TypeError(`Length ${actualLength} exceeds limit ${limit}`);
		const lowered = prefix.toLowerCase();
		const sum = bechChecksum(lowered, words, ENCODING_CONST);
		return `${lowered}1${BECH_ALPHABET.encode(words)}${sum}`;
	}
	function decode(str, limit = 90) {
		astr("bech32.decode input", str);
		const slen = str.length;
		if (slen < 8 || limit !== false && slen > limit) throw new TypeError(`invalid string length: ${slen} (${str}). Expected (8..${limit})`);
		const lowered = str.toLowerCase();
		if (str !== lowered && str !== str.toUpperCase()) throw new Error(`String must be lowercase or uppercase`);
		const sepIndex = lowered.lastIndexOf("1");
		if (sepIndex === 0 || sepIndex === -1) throw new Error(`Letter "1" must be present between prefix and data only`);
		const prefix = lowered.slice(0, sepIndex);
		const data = lowered.slice(sepIndex + 1);
		if (data.length < 6) throw new Error("Data must be at least 6 characters long");
		const words = BECH_ALPHABET.decode(data).slice(0, -6);
		const sum = bechChecksum(prefix, words, ENCODING_CONST);
		if (!data.endsWith(sum)) throw new Error(`Invalid checksum in ${str}: expected "${sum}"`);
		return {
			prefix,
			words
		};
	}
	const decodeUnsafe = unsafeWrapper(decode);
	function decodeToBytes(str) {
		const { prefix, words } = decode(str, false);
		return {
			prefix,
			words,
			bytes: fromWords(words)
		};
	}
	function encodeFromBytes(prefix, bytes) {
		return encode(prefix, toWords(bytes));
	}
	return {
		encode,
		decode,
		encodeFromBytes,
		decodeToBytes,
		decodeUnsafe,
		fromWords,
		fromWordsUnsafe,
		toWords
	};
}
genBech32("bech32");
genBech32("bech32m");
chain(radix2(4), alphabet("0123456789abcdef"), join(""), normalize((s) => {
	if (typeof s !== "string" || s.length % 2 !== 0) throw new TypeError(`hex.decode: expected string, got ${typeof s} with length ${s.length}`);
	return s.toLowerCase();
}));
// src/lib/native.ts
const host = typeof window !== "undefined" ? window.UnionHost : void 0;
if (typeof document !== "undefined" && Boolean(host)) {
	document.documentElement.classList.add("union-native");
	window.__UNION_NATIVE__ = true;
}
// src/lib/util.ts
const now = () => (/* @__PURE__ */ new Date()).toISOString();
const uid = (prefix = "r") => `${prefix}_${crypto.randomUUID().replace(/-/g, "").slice(0, 20)}`;
function bytesToHex(bytes) {
	let s = "";
	for (const b of bytes) s += b.toString(16).padStart(2, "0");
	return s;
}
// src/lib/core.ts
/** Loads the Rust trust core and exposes a SHA-256 primitive.
*  The JavaScript fallback exists so the app still opens if a WebView refuses
*  WebAssembly; the UI reports which core is active. */
let core = null;
let loading = null;
async function initCore() {
	if (core) return true;
	if (loading) return loading;
	loading = (async () => {
		try {
			const base = location.href.replace(/#.*$/, "").replace(/[^/]*$/, "");
			const url = new URL("union_core.wasm", base);
			const response = await fetch(url, { cache: "no-cache" });
			if (!response.ok) throw new Error(`HTTP ${response.status}`);
			const bytes = await response.arrayBuffer();
			const { instance } = await WebAssembly.instantiate(bytes, {});
			const exports = instance.exports;
			if (exports.union_self_test() !== 1) throw new Error("core self-test failed");
			core = exports;
			return true;
		} catch (error) {
			error instanceof Error && error.message;
			return false;
		}
	})();
	return loading;
}
async function sha256Bytes(data) {
	if (!core) await initCore();
	if (!core) return sha256(data);
	const ptr = core.union_alloc(data.length);
	const out = core.union_alloc(32);
	try {
		new Uint8Array(core.memory.buffer, ptr, data.length).set(data);
		core.union_sha256(ptr, data.length, out);
		return new Uint8Array(new Uint8Array(core.memory.buffer, out, 32));
	} finally {
		core.union_free(ptr, data.length);
		core.union_free(out, 32);
	}
}
async function sha256Hex(data) {
	return bytesToHex(await sha256Bytes(typeof data === "string" ? new TextEncoder().encode(data) : data));
}
// src/lib/crypto.ts
/** Union cryptography.
*
*  - Identity: Ed25519 signatures (portable, verifiable).
*  - Key agreement: X25519, ephemeral per message.
*  - Message encryption: HKDF-SHA256 -> AES-256-GCM (WebCrypto).
*  - Backup encryption: PBKDF2-SHA256 (310k) -> AES-256-GCM.
*
*  Established implementations only: @noble/curves, @noble/hashes and the
*  platform WebCrypto. Nothing in this file invents a protocol.
*/
const encoder = new TextEncoder();
const decoder = new TextDecoder();
const toBytes = (text) => encoder.encode(text);
const fromBytes = (bytes) => decoder.decode(bytes);
const b64 = (bytes) => base64.encode(bytes);
const b64u = (bytes) => base64url.encode(bytes);
const fromB64 = (text) => new Uint8Array(base64.decode(text));
const fromB64u = (text) => new Uint8Array(base64url.decode(text));
const COLORS = [
	"#5b5bd6",
	"#0d9488",
	"#c2410c",
	"#7c3aed",
	"#0369a1",
	"#b45309",
	"#be123c",
	"#15803d",
	"#4f46e5",
	"#0e7490"
];
function colorFor(seed) {
	let h = 0;
	for (let i = 0; i < seed.length; i++) h = h * 31 + seed.charCodeAt(i) >>> 0;
	return COLORS[h % COLORS.length];
}
function newIdentity(name, handle, color) {
	const edPriv = ed25519.utils.randomPrivateKey();
	const xPriv = x25519.utils.randomPrivateKey();
	const edPub = ed25519.getPublicKey(edPriv);
	const xPub = x25519.getPublicKey(xPriv);
	const id = `did:union:${b64u(edPub)}`;
	const createdAt = now();
	return {
		identity: {
			type: "identity",
			id,
			handle,
			name: name.trim() || handle,
			about: "",
			edPub: b64(edPub),
			xPub: b64(xPub),
			color: color || colorFor(id),
			createdAt,
			updatedAt: createdAt,
			experience: "special",
			activation: { status: "inactive" }
		},
		keyring: {
			type: "keyring",
			id: `key_${uid("k").slice(2)}`,
			identityId: id,
			edPriv: b64(edPriv),
			xPriv: b64(xPriv),
			createdAt
		}
	};
}
function sign(keys, message) {
	const bytes = typeof message === "string" ? toBytes(message) : message;
	return b64(ed25519.sign(bytes, fromB64(keys.edPriv)));
}
function verify(edPub, signature, message) {
	try {
		const bytes = typeof message === "string" ? toBytes(message) : message;
		return ed25519.verify(fromB64(signature), bytes, fromB64(edPub));
	} catch {
		return false;
	}
}
function publicFromPrivate(keys) {
	return {
		edPub: b64(ed25519.getPublicKey(fromB64(keys.edPriv))),
		xPub: b64(x25519.getPublicKey(fromB64(keys.xPriv)))
	};
}
/** Deterministic JSON with sorted keys: the input to every signature/hash. */
function canonical(value) {
	if (value === null || typeof value !== "object") return JSON.stringify(value) ?? "null";
	if (Array.isArray(value)) return `[${value.map(canonical).join(",")}]`;
	const obj = value;
	return `{${Object.keys(obj).filter((k) => obj[k] !== void 0).sort().map((k) => `${JSON.stringify(k)}:${canonical(obj[k])}`).join(",")}}`;
}
function makeCard(identity, keys) {
	const card = {
		v: 1,
		k: "union-card",
		id: identity.id,
		handle: identity.handle,
		name: identity.name,
		about: identity.about,
		ed: identity.edPub,
		x: identity.xPub,
		color: identity.color,
		ts: now()
	};
	card.sig = sign(keys, canonical({
		...card,
		sig: void 0
	}));
	return card;
}
function encodeCard(card) {
	return `union-card:${b64u(toBytes(JSON.stringify(card)))}`;
}
function decodeCard(text) {
	try {
		const raw = text.trim().replace(/^union-card:/, "");
		const card = JSON.parse(fromBytes(fromB64u(raw)));
		if (card.v !== 1 || card.k !== "union-card" || !card.id || !card.ed || !card.x) return null;
		if (!card.sig) return null;
		const copy = { ...card };
		delete copy.sig;
		return verify(card.ed, card.sig, canonical(copy)) ? card : null;
	} catch {
		return null;
	}
}
async function aesKey(shared, info) {
	const keyBytes = new Uint8Array(hkdf(sha256, shared, void 0, toBytes(info), 32));
	return crypto.subtle.importKey("raw", keyBytes, "AES-GCM", false, ["encrypt", "decrypt"]);
}
async function aesEncrypt(key, text) {
	const nonce = crypto.getRandomValues(/* @__PURE__ */ new Uint8Array(12));
	const cipher = await crypto.subtle.encrypt({
		name: "AES-GCM",
		iv: nonce
	}, key, toBytes(text));
	return {
		n: b64(nonce),
		c: b64(new Uint8Array(cipher))
	};
}
async function aesDecrypt(key, box) {
	const plain = await crypto.subtle.decrypt({
		name: "AES-GCM",
		iv: fromB64(box.n)
	}, key, fromB64(box.c));
	return fromBytes(new Uint8Array(plain));
}
async function sealMessage(plaintext, sender, recipient) {
	const ephPriv = x25519.utils.randomPrivateKey();
	const epk = b64(x25519.getPublicKey(ephPriv));
	const toShared = x25519.getSharedSecret(ephPriv, fromB64(recipient.xPub));
	const selfShared = x25519.getSharedSecret(ephPriv, fromB64(sender.xPub));
	const toKey = await aesKey(toShared, `union/v1/message|${sender.id}|recipient`);
	const selfKey = await aesKey(selfShared, `union/v1/message|${sender.id}|self`);
	return {
		v: 1,
		alg: "x25519-hkdf-sha256-aes256gcm",
		epk,
		from: sender.id,
		to: recipient.id,
		body: await aesEncrypt(toKey, plaintext),
		self: await aesEncrypt(selfKey, plaintext)
	};
}
async function openSealed(sealed, myId, myXPriv) {
	const isSenderCopy = sealed.from === myId;
	return aesDecrypt(await aesKey(x25519.getSharedSecret(fromB64(myXPriv), fromB64(sealed.epk)), isSenderCopy ? `union/v1/message|${sealed.from}|self` : `union/v1/message|${sealed.from}|recipient`), isSenderCopy ? sealed.self : sealed.body);
}
async function passKey(passphrase, salt, iterations) {
	const base = await crypto.subtle.importKey("raw", toBytes(passphrase), "PBKDF2", false, ["deriveKey"]);
	return crypto.subtle.deriveKey({
		name: "PBKDF2",
		salt,
		iterations,
		hash: "SHA-256"
	}, base, {
		name: "AES-GCM",
		length: 256
	}, false, ["encrypt", "decrypt"]);
}
async function encryptBackup(payload, passphrase) {
	if (passphrase.length < 8) throw new Error("Use a passphrase of at least 8 characters.");
	const salt = new Uint8Array(randomBytes(16));
	const iv = new Uint8Array(randomBytes(12));
	const iterations = 31e4;
	const key = await passKey(passphrase, salt, iterations);
	const ct = await crypto.subtle.encrypt({
		name: "AES-GCM",
		iv
	}, key, toBytes(JSON.stringify(payload)));
	const envelope = {
		v: 1,
		kdf: "PBKDF2-SHA256",
		iterations,
		salt: b64(salt),
		iv: b64(iv),
		ct: b64(new Uint8Array(ct)),
		createdAt: now()
	};
	return JSON.stringify(envelope, null, 2);
}
async function decryptBackup(text, passphrase) {
	const envelope = JSON.parse(text);
	if (envelope.v !== 1 || envelope.kdf !== "PBKDF2-SHA256") throw new Error("Unknown backup format.");
	const key = await passKey(passphrase, fromB64(envelope.salt), envelope.iterations);
	const plain = await crypto.subtle.decrypt({
		name: "AES-GCM",
		iv: fromB64(envelope.iv)
	}, key, fromB64(envelope.ct));
	return JSON.parse(fromBytes(new Uint8Array(plain)));
}
/** Hash an arbitrary value for the audit/ledger chain. */
async function hashValue(value) {
	return sha256Hex(canonical(value));
}
function randomToken(bytes = 16) {
	return b64u(randomBytes(bytes));
}
function encodeToken(payload) {
	return b64u(toBytes(JSON.stringify(payload)));
}
function decodeToken(token) {
	try {
		return JSON.parse(fromBytes(fromB64u(token)));
	} catch {
		return null;
	}
}
export { COLORS, canonical, colorFor, decodeCard, decodeToken, decryptBackup, encodeCard, encodeToken, encryptBackup, hashValue, makeCard, newIdentity, openSealed, publicFromPrivate, randomToken, sealMessage, sign, verify };
