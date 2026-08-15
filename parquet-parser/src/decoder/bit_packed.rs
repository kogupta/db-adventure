use anyhow::Result;
use bytes::Bytes;
use polars::prelude::*;

use crate::format::Type;

/// Decode bit-packed encoded data.
///
/// This function bit-packed encoded data into a vector of [`Scalar`].
/// It only supports 2 type of data type: BOOLEAN and INT32.
///
/// [parquet encoding spec](https://parquet.apache.org/docs/file-format/data-pages/encodings/#RLE)
#[allow(unused)]
pub fn bit_packed_decode(
    encoded_data: Bytes,
    parquet_type: Type,
    bit_width: u8,
    num_values: usize,
) -> Result<Vec<Scalar>> {
    todo!("step09: implement the boolean data decoder")
}
