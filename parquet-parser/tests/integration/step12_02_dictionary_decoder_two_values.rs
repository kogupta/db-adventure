use anyhow::Result;
use insta::assert_snapshot;
use parquet_parser::reader::read_parquet;

use crate::make_parquet_file;

#[test]
fn two_values() -> Result<()> {
    let parquet_file = make_parquet_file(
        r#"
my_col
one
one
one
one
one
one
one
one
two
one
two
one
two
one
two
one
"#,
        &[&["--dictionary"]],
    )?;

    assert_snapshot!(read_parquet(parquet_file)?, @"
    shape: (16, 1)
    ┌────────┐
    │ my_col │
    │ ---    │
    │ str    │
    ╞════════╡
    │ one    │
    │ one    │
    │ one    │
    │ one    │
    │ one    │
    │ one    │
    │ one    │
    │ one    │
    │ two    │
    │ one    │
    │ two    │
    │ one    │
    │ two    │
    │ one    │
    │ two    │
    │ one    │
    └────────┘
    ");
    Ok(())
}
