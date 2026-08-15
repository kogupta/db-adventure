use anyhow::Result;
use bytes::Bytes;

/// A ULEB123 decoder: https://en.wikipedia.org/wiki/LEB128#Unsigned_LEB128
#[allow(unused)]
pub fn uleb128_decode(encoded_data: Bytes) -> Result<(u64, Bytes)> {
    todo!("step10-01: implement uleb128 decoder")
}
