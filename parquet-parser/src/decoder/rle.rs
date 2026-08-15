use anyhow::Result;
use bytes::Bytes;
use polars::prelude::*;

use crate::format::Type;

/// RLE decoding.
///
/// A RLE encoded data includes the number of repeated values and the actual values.
/// This function takes the repeated value as an encoded data and
/// returns a vector contains the repeated values.
///
/// *Yes, this is very inefficient!*
#[allow(unused)]
pub fn rle_decode(
    encoded_data: Bytes,
    parquet_type: Type,
    bit_width: u8,
    num_values: usize,
) -> Result<Vec<Scalar>> {
    todo!("step10-04: decode a rle run")
}
