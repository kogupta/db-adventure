use anyhow::Result;
use bytes::Bytes;
use polars::prelude::*;

/// Dictionary decoding for data page.
///
/// The data page in dictionary encoding use RLE Bit-packing hybrid encoding.
#[allow(unused_variables)]
pub fn dictionary_decode(encoded_data: Bytes, num_values: usize) -> Result<Vec<Scalar>> {
    todo!("step12-02: dictionary decoder")
}
