use anyhow::Result;
use insta::assert_snapshot;
use parquet_parser::reader::read_parquet;
use polars::prelude::*;
use rstest::rstest;

use crate::make_parquet_file;

#[test]
fn bit_width_2() -> Result<()> {
    let parquet_file = make_parquet_file(
        r#"
my_col
one
two
three
"#,
        &[&["--dictionary"]],
    )?;

    assert_snapshot!(read_parquet(parquet_file)?, @"
    shape: (3, 1)
    ┌────────┐
    │ my_col │
    │ ---    │
    │ str    │
    ╞════════╡
    │ one    │
    │ two    │
    │ three  │
    └────────┘
    ");
    Ok(())
}

#[test]
fn bit_width_3() -> Result<()> {
    let parquet_file = make_parquet_file(
        r#"
my_col
one
two
three
four
five
"#,
        &[&["--dictionary"]],
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
    │ three  │
    │ four   │
    │ five   │
    └────────┘
    ");
    Ok(())
}

#[rstest]
#[case::ten(10)]
#[case::sixteen(16)]
fn many_bit_width(#[case] bit_width: usize) -> Result<()> {
    let length = 1 << ((bit_width - 1) + 1);
    let data: Vec<String> = (0..length).map(|v| v.to_string()).collect();
    let parquet_data: Vec<String> = vec!["my_col".to_string()]
        .into_iter()
        .chain(data.clone())
        .collect();

    let parquet_file = make_parquet_file(
        &parquet_data.join("\n"),
        &[&["--dictionary"], &["--dtypes", "my_col=string"]],
    )?;
    let df = read_parquet(parquet_file)?;
    assert_eq!(df.height(), length);

    let column = df.column("my_col")?;
    for (i, expected) in data.iter().enumerate() {
        let value = column.get(i)?;
        assert_eq!(value, AnyValue::String(expected));
    }

    Ok(())
}
