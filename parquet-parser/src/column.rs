use anyhow::Result;
use bytes::Bytes;
use polars::prelude::*;

use crate::format::{ColumnChunk, ColumnMetaData};

/// Convert a vector of [`Scalar`] to a [`Column`].
#[allow(unused)]
fn column_from_scalars(scalars: Vec<Scalar>, column_metadata: &ColumnMetaData) -> Result<Column> {
    let values: Vec<AnyValue<'_>> = scalars
        .into_iter()
        .map(|scalar| scalar.into_value())
        .collect();

    let column_name = column_metadata.path_in_schema.join(".");
    let series = Series::from_any_values(column_name.into(), &values, true)?;
    let column = Column::from(series);

    Ok(column)
}

/// Read [`Column`] from a parquet file based on [`ColumnChunk`]'s metadata.
///
/// A column chunk contains multiple pages, this function extract all the pages,
/// decodes them and returns the correct [`Column`] for a chunk.
#[allow(unused_variables)]
pub fn read_column(data: Bytes, column_chunk: &ColumnChunk) -> Result<Column> {
    todo!("step06: implement read column")
}
