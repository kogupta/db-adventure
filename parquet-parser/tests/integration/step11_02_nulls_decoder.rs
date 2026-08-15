use anyhow::Result;
use insta::assert_snapshot;
use parquet_parser::reader::read_parquet;

use crate::make_parquet_file;

#[test]
fn nulls() -> Result<()> {
    let parquet_file = make_parquet_file(
        r#"
my_col
one
two
""
""
three
"#,
        &[],
    )?;

    assert_snapshot!(read_parquet(parquet_file)?, @"
    shape: (5, 1)
    ┌────────┐
    │ my_col │
    │ ---    │
    │ str    │
    ╞════════╡
    │ one    │
    │ two    │
    │ null   │
    │ null   │
    │ three  │
    └────────┘
    ");
    Ok(())
}
