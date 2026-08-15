use anyhow::Result;
use bytes::Bytes;
use polars::prelude::*;

use crate::format::Type;

/// Plain decoding.
///
/// The parser currently supports these data type types. For [`Type::BYTE_ARRAY`] we always treat it as string.
///
/// - BOOLEAN: Bit Packed, LSB first
/// - INT32: 4 bytes little endian
/// - INT64: 8 bytes little endian
/// - FLOAT: 4 bytes IEEE little endian
/// - DOUBLE: 8 bytes IEEE little endian
/// - BYTE_ARRAY: length in 4 bytes little endian followed by the bytes contained in the array
///
/// This function decode the data into a vector of [`Scalar`].
///
/// [plain encoding]: https://parquet.apache.org/docs/file-format/data-pages/encodings/#PLAIN
#[allow(unused)]
pub fn plain_decode(
    encoded_data: Bytes,
    parquet_type: Type,
    num_values: usize,
) -> Result<Vec<Scalar>> {
    match parquet_type {
        Type::INT32 => todo!("step05: decode int32"),
        Type::INT64 => todo!("step05: decode int64"),
        Type::FLOAT => todo!("step05: decode float"),
        Type::DOUBLE => todo!("step05: decode double"),
        Type::BYTE_ARRAY => todo!("step05: decode string"),
        Type::BOOLEAN => todo!("step09: decode boolean"),
        _ => unimplemented!("plain_decode: unsupported data type {:?}", parquet_type),
    }
}
