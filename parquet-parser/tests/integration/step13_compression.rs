use anyhow::Result;
use insta::assert_snapshot;
use parquet_parser::reader::read_parquet;

use crate::make_parquet_file;

#[test]
fn snappy() -> Result<()> {
    let parquet_file = make_parquet_file(
        r#"
boolean_col,i64_col,double_col,string_col
true,1,1.1,one
true,2,2.2,two
false,3,3.3,three
true,4,4.4,four
false,5,5.5,five
false,6,6.6,six
"#,
        &[&["--compression", "snappy"]],
    )?;

    assert_snapshot!(read_parquet(parquet_file)?, @"
    shape: (6, 4)
    ┌─────────────┬─────────┬────────────┬────────────┐
    │ boolean_col ┆ i64_col ┆ double_col ┆ string_col │
    │ ---         ┆ ---     ┆ ---        ┆ ---        │
    │ bool        ┆ i64     ┆ f64        ┆ str        │
    ╞═════════════╪═════════╪════════════╪════════════╡
    │ true        ┆ 1       ┆ 1.1        ┆ one        │
    │ true        ┆ 2       ┆ 2.2        ┆ two        │
    │ false       ┆ 3       ┆ 3.3        ┆ three      │
    │ true        ┆ 4       ┆ 4.4        ┆ four       │
    │ false       ┆ 5       ┆ 5.5        ┆ five       │
    │ false       ┆ 6       ┆ 6.6        ┆ six        │
    └─────────────┴─────────┴────────────┴────────────┘
    ");

    Ok(())
}
