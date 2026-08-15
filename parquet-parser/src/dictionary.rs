use anyhow::Result;
use polars::prelude::*;

use crate::{format::Type, page::Pages};

/// Extracting dictionary entries from [`Pages`].
///
/// Return `None` if [`Pages`] doesn't contain dictionary page (no dictionary encoding used).
/// Otherwise, return the decoded vector of [`Scalar`].
#[allow(unused_variables)]
pub fn dictionary_entries(pages: &Pages, parquet_type: Type) -> Result<Option<Vec<Scalar>>> {
    todo!("step12-01: extract dictionary entries from dictionary page")
}

/// Try to map the decoded data from data page to the actual values using dictionary entries.
///
/// The dictionary entries might not exist if the page doesn't use dictionary encoding.
/// In that case `dictionary_entries` is `None` and `indexes_or_values` is the actual column values.
#[allow(unused_variables)]
pub fn map_dictionary_entries(
    dictionary_entries: &Option<Vec<Scalar>>,
    indexes_or_values: Vec<Scalar>,
) -> Result<Vec<Scalar>> {
    todo!("step12-02: map indexes in data page to the exact values")
}
