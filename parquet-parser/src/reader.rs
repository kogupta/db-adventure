use std::path::Path;

use anyhow::Result;
use polars::prelude::*;

/// Read a parquet file into [`DataFrame`].
///
/// This function verifies if the magic number is correct,
/// reads the file metadata, then parses all row groups into the [`DataFrame`].
#[allow(unused_variables)]
pub fn read_parquet(file_path: impl AsRef<Path>) -> Result<DataFrame> {
    todo!("step08: implement read parquet")
}
