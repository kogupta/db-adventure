use anyhow::Result;
use polars::prelude::*;

use crate::{format::Type, page::Page};

/// Decode a definition levels.
///
/// This function decode the definition levels in the data page and return a null map.
/// Each entry in the null map is a boolean where:
/// - `true`: the value for this column exists.
/// - `false`: the value for this column is missing (null).
#[allow(unused_variables)]
pub fn decode_definition_levels(page: &Page) -> Result<Vec<bool>> {
    todo!("step11-01: decode definition levels")
}

/// Add null entries to a vector of [`Scalar`] decoded from page data.
#[allow(unused_variables)]
pub fn add_nulls_entries(
    is_present: &[bool],
    scalars: Vec<Scalar>,
    parquet_type: Type,
) -> Result<Vec<Scalar>> {
    todo!("step11-02: handle nulls in a column")
}

/// Convert parquet type to polar's [`DataType`].
///
/// This allows creating a null value using [`Scalar::null`] for a specific [`Type`].
#[allow(unused)]
fn scalar_null(parquet_type: Type) -> Scalar {
    let data_type = match parquet_type {
        Type::BOOLEAN => DataType::Boolean,
        Type::INT32 => DataType::Int32,
        Type::INT64 => DataType::Int64,
        Type::FLOAT => DataType::Float32,
        Type::DOUBLE => DataType::Float64,
        Type::BYTE_ARRAY => DataType::String,
        _ => unimplemented!("Unsupported"),
    };
    Scalar::null(data_type)
}
