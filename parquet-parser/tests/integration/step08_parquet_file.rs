use anyhow::Result;
use insta::assert_snapshot;
use parquet_parser::reader::read_parquet;

use crate::make_parquet_file;

#[test]
fn parquet_file() -> Result<()> {
    let parquet_file = make_parquet_file(
        r#"
i32,i64,float,double,string
1,1,1.1,1.1,one
2,2,2.2,2.2,two
3,3,3.3,3.3,three
4,4,4.4,4.4,four
5,5,5.5,5.5,five
6,6,6.6,6.6,six
"#,
        &[
            &["--dtypes", "i32=int32"],
            &["--dtypes", "i64=int64"],
            &["--dtypes", "float=float"],
            &["--dtypes", "double=double"],
            &["--dtypes", "string=string"],
        ],
    )?;

    let df = read_parquet(parquet_file)?;
    assert_snapshot!(df, @"
    shape: (6, 5)
    ┌─────┬─────┬───────┬────────┬────────┐
    │ i32 ┆ i64 ┆ float ┆ double ┆ string │
    │ --- ┆ --- ┆ ---   ┆ ---    ┆ ---    │
    │ i32 ┆ i64 ┆ f32   ┆ f64    ┆ str    │
    ╞═════╪═════╪═══════╪════════╪════════╡
    │ 1   ┆ 1   ┆ 1.1   ┆ 1.1    ┆ one    │
    │ 2   ┆ 2   ┆ 2.2   ┆ 2.2    ┆ two    │
    │ 3   ┆ 3   ┆ 3.3   ┆ 3.3    ┆ three  │
    │ 4   ┆ 4   ┆ 4.4   ┆ 4.4    ┆ four   │
    │ 5   ┆ 5   ┆ 5.5   ┆ 5.5    ┆ five   │
    │ 6   ┆ 6   ┆ 6.6   ┆ 6.6    ┆ six    │
    └─────┴─────┴───────┴────────┴────────┘
    ");
    Ok(())
}
