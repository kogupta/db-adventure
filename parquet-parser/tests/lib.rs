mod integration;

use std::io::Read;
use std::io::Write;
use std::process::Command;

use anyhow::Result;
use anyhow::bail;
use bytes::Bytes;
use ctor::ctor;
use tempfile::NamedTempFile;

#[ctor(unsafe)]
fn init_polars_env() {
    unsafe { std::env::set_var("POLARS_FMT_MAX_ROWS", "1000") };
}

pub fn make_parquet_file(data: &str, args: &[&[&'static str]]) -> Result<NamedTempFile> {
    let mut csv_file = NamedTempFile::new()?;
    csv_file.write_all(data.trim().as_bytes())?;

    let parquet_file = NamedTempFile::new()?;

    let mut cmd = Command::new(env!("CARGO_BIN_EXE_parquet-parser"));
    let mut cmd = cmd
        .arg("write")
        .arg(csv_file.path().to_str().unwrap())
        .arg(parquet_file.path().to_str().unwrap());
    for args in args {
        cmd = cmd.args(*args);
    }
    let output = cmd.output().expect("write must success");
    if !output.status.success() {
        let stderr = String::from_utf8(output.stderr)?;
        bail!("Error write to parquet file: {stderr}");
    }

    Ok(parquet_file)
}

pub fn make_parquet_bytes(data: &str, args: &[&[&'static str]]) -> Result<Bytes> {
    let mut output_file = make_parquet_file(data, args)?;
    let mut output = Vec::new();
    output_file.read_to_end(&mut output)?;
    Ok(Bytes::from(output))
}
