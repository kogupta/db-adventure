use anyhow::Result;
use insta::assert_snapshot;
use parquet_parser::reader::read_parquet;
use rstest::rstest;

use crate::make_parquet_file;

#[rstest]
#[case::rows_per_page_8("8")]
#[case::rows_per_page_all("100")]
fn boolean_column(#[case] rows_per_page: &'static str) -> Result<()> {
    let parquet_file = make_parquet_file(
        r#"
boolean
true
true
false
true
true
false
true
true
false
true
"#,
        &[&["--rows-per-page", rows_per_page]],
    )?;

    assert_snapshot!(read_parquet(parquet_file)?, @"
    shape: (10, 1)
    ┌─────────┐
    │ boolean │
    │ ---     │
    │ bool    │
    ╞═════════╡
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    └─────────┘
    ");
    Ok(())
}

#[rstest]
#[case::rows_per_page_8("8")]
#[case::rows_per_page_all("100")]
fn boolean_column_all_true_false(#[case] rows_per_page: &'static str) -> Result<()> {
    let parquet_file = make_parquet_file(
        r#"
boolean_true,boolean_false
true,false
true,false
true,false
true,false
true,false
true,false
true,false
true,false
true,false
true,false
"#,
        &[&["--rows-per-page", rows_per_page]],
    )?;

    assert_snapshot!(read_parquet(parquet_file)?, @"
    shape: (10, 2)
    ┌──────────────┬───────────────┐
    │ boolean_true ┆ boolean_false │
    │ ---          ┆ ---           │
    │ bool         ┆ bool          │
    ╞══════════════╪═══════════════╡
    │ true         ┆ false         │
    │ true         ┆ false         │
    │ true         ┆ false         │
    │ true         ┆ false         │
    │ true         ┆ false         │
    │ true         ┆ false         │
    │ true         ┆ false         │
    │ true         ┆ false         │
    │ true         ┆ false         │
    │ true         ┆ false         │
    └──────────────┴───────────────┘
    ");
    Ok(())
}

#[rstest]
#[case::rows_per_page_8("8")]
#[case::rows_per_page_all("100")]
fn boolean_column_long(#[case] rows_per_page: &'static str) -> Result<()> {
    let parquet_file = make_parquet_file(
        r#"
boolean
true
true
false
true
true
false
true
true
false
true
true
true
false
true
true
false
true
true
false
true
true
true
false
true
true
false
true
true
false
true
true
true
false
true
true
false
true
true
false
true
true
true
false
true
true
false
true
true
false
true
true
true
false
true
true
false
true
true
false
true
true
true
false
true
true
false
true
true
false
"#,
        &[&["--rows-per-page", rows_per_page]],
    )?;
    assert_snapshot!(read_parquet(parquet_file)?, @"
    shape: (69, 1)
    ┌─────────┐
    │ boolean │
    │ ---     │
    │ bool    │
    ╞═════════╡
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    │ true    │
    │ true    │
    │ false   │
    └─────────┘
    ");
    Ok(())
}
