use anyhow::Result;
use bytes::Bytes;
use polars::prelude::*;

use crate::format::{FileMetaData, RowGroup};

/// Read a row group into [`DataFrame`].
///
/// A row group contains multiple column chunks.
/// This function reads all the column chunks into a single [`DataFrame`].
#[allow(unused_variables)]
pub fn read_row_group(data: Bytes, row_group: &RowGroup) -> Result<DataFrame> {
    todo!("step07: implement read row group")
}

/// Read row groups into [`DataFrame`].
///
/// A file contains multiple row groups.
/// This function reads all the row groups, and
/// concatenate all the returned [`DataFrame`]s into a single [`DataFrame`].
#[allow(unused_variables)]
pub fn read_row_groups(data: Bytes, file_metadata: &FileMetaData) -> Result<DataFrame> {
    todo!("step07: implement read row groups")
}
