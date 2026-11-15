//! Union trust core.
//!
//! The only job of this crate is to make the bytes that matter unambiguous:
//! SHA-256 digests for the append-only audit chain and the Digital Giving
//! ledger. Hashing uses the maintained RustCrypto `sha2` implementation; no
//! cryptography is invented here. The core has no network, clock, filesystem
//! or randomness imports.

use sha2::{Digest, Sha256};
use std::slice;

pub const VERSION: u32 = 1;

/// Allocate `len` bytes for a caller (JavaScript) to write into.
#[no_mangle]
pub extern "C" fn union_alloc(len: usize) -> *mut u8 {
    let mut buf = Vec::<u8>::with_capacity(len);
    let ptr = buf.as_mut_ptr();
    std::mem::forget(buf);
    ptr
}

/// Release a buffer previously returned by `union_alloc`.
///
/// # Safety
/// `ptr` must come from `union_alloc` with the same `len`.
#[no_mangle]
pub unsafe extern "C" fn union_free(ptr: *mut u8, len: usize) {
    if ptr.is_null() {
        return;
    }
    drop(Vec::from_raw_parts(ptr, 0, len));
}

/// Write the 32-byte SHA-256 digest of `input` into `out`.
///
/// # Safety
/// `input`/`out` must be valid for `len`/32 bytes respectively.
#[no_mangle]
pub unsafe extern "C" fn union_sha256(input: *const u8, len: usize, out: *mut u8) {
    if out.is_null() || (input.is_null() && len != 0) {
        return;
    }
    let bytes = if len == 0 {
        &[][..]
    } else {
        slice::from_raw_parts(input, len)
    };
    let digest = Sha256::digest(bytes);
    std::ptr::copy_nonoverlapping(digest.as_ptr(), out, 32);
}

#[no_mangle]
pub extern "C" fn union_version() -> u32 {
    VERSION
}

/// Self-test: hashes the ASCII bytes "union" and compares with the published
/// digest. Returns 1 when this build is correct.
#[no_mangle]
pub extern "C" fn union_self_test() -> i32 {
    let digest = Sha256::digest(b"union");
    let expected = [
        0xef, 0x3f, 0xf7, 0x03, 0xd7, 0x56, 0x5a, 0xb5, 0x21, 0x5c, 0xdb, 0xe0, 0x43, 0x85, 0x8f,
        0x2b, 0x8a, 0x04, 0x15, 0xb2, 0xb3, 0x5d, 0xd1, 0x7a, 0x3d, 0xa1, 0x35, 0x4f, 0x86, 0x00,
        0x9b, 0x98,
    ];
    if digest.as_slice() == expected {
        1
    } else {
        0
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn known_vector() {
        let digest = Sha256::digest(b"union");
        assert_eq!(
            format!("{:x}", digest),
            "ef3ff703d7565ab5215cdbe043858f2b8a0415b2b35dd17a3da1354f86009b98"
        );
    }

    #[test]
    fn self_test_passes() {
        assert_eq!(union_self_test(), 1);
    }
}
