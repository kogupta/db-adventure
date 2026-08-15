use anyhow::Result;
use insta::assert_snapshot;
use parquet_parser::{
    file_metadata::read_file_metadata,
    row_group::{read_row_group, read_row_groups},
};

use crate::make_parquet_bytes;

#[test]
fn one_group() -> Result<()> {
    let data = make_parquet_bytes(
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

    let file_metadata = read_file_metadata(data.clone())?;
    let row_group = &file_metadata.row_groups[0];
    let df = read_row_group(data, row_group)?;
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

#[test]
fn many_groups() -> Result<()> {
    let data = make_parquet_bytes(
        r#"
i32,i64,float,double,string
1,1,1.1,1.1,one
2,2,2.2,2.2,two
3,3,3.3,3.3,three
4,4,4.4,4.4,four
5,5,5.5,5.5,five
6,6,6.6,6.6,six
7,7,7.7,7.7,seven
8,8,8.8,8.8,eight
9,9,9.9,9.9,nine
"#,
        &[
            &["--rows-per-page", "2"],
            &["--rows-per-group", "2"],
            &["--dtypes", "i32=int32"],
            &["--dtypes", "i64=int64"],
            &["--dtypes", "float=float"],
            &["--dtypes", "double=double"],
            &["--dtypes", "string=string"],
        ],
    )?;

    let file_metadata = read_file_metadata(data.clone())?;
    let df = read_row_groups(data, &file_metadata)?;
    assert_snapshot!(df, @"
    shape: (9, 5)
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
    │ 7   ┆ 7   ┆ 7.7   ┆ 7.7    ┆ seven  │
    │ 8   ┆ 8   ┆ 8.8   ┆ 8.8    ┆ eight  │
    │ 9   ┆ 9   ┆ 9.9   ┆ 9.9    ┆ nine   │
    └─────┴─────┴───────┴────────┴────────┘
    ");

    Ok(())
}
