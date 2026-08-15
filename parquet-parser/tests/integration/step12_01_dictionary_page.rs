use anyhow::Result;
use parquet_parser::{
    dictionary::dictionary_entries, file_metadata::read_file_metadata,
    page::read_pages,
};
use polars::prelude::*;

use crate::make_parquet_bytes;

#[test]
fn dictionary_enabled() -> Result<()> {
    let parquet_data = make_parquet_bytes(
        r#"
my_col
one
two
three
one
two
three
"#,
        &[&["--dictionary"]],
    )?;

    let file_metadata = read_file_metadata(parquet_data.clone())?;
    let column_metadata = file_metadata.row_groups[0].columns[0]
        .meta_data
        .as_ref()
        .unwrap();

    let pages = read_pages(parquet_data.clone(), column_metadata)?;

    assert!(pages.dictionary_page.is_some());
    assert_eq!(pages.data_pages.len(), 1);

    let dictionary_entries = dictionary_entries(&pages, column_metadata.type_)?.unwrap();
    assert_eq!(
        dictionary_entries,
        [
            Scalar::from(PlSmallStr::from("one")),
            Scalar::from(PlSmallStr::from("two")),
            Scalar::from(PlSmallStr::from("three")),
        ]
    );

    Ok(())
}
