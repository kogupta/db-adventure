use std::collections::HashMap;
use std::fs::{File, OpenOptions};
use std::io::{Read, Write};
use std::path::PathBuf;

use crate::format::Type;
use crate::writer::write_parquet;
use anyhow::{Context, Result, bail};
use clap::{Parser, ValueEnum};
use parquet::basic::{Compression, Encoding};

#[derive(Debug, Clone, ValueEnum)]
enum WriteCompression {
    Uncompressed,
    Snappy,
}

/// Write a csv file to a parquet file.
#[derive(Parser)]
pub struct WriteCommand {
    /// The input csv file.
    csv: PathBuf,

    /// The output parquet file.
    parquet: PathBuf,

    /// The author.
    #[arg(long, default_value = "Hello parquet!")]
    author: String,

    /// Whether to enable dictionary encoding.
    #[arg(long, default_value_t = false)]
    dictionary: bool,

    /// Encoding for each column. Syntax: `--encodings <column_name>=<encoding>`. Supported encodings: [rle].
    #[arg(long, value_enum, value_parser = parse_column_encoding)]
    encodings: Vec<(String, Encoding)>,

    /// Compression for the output parquet.
    #[arg(long, value_enum, default_value_t = WriteCompression::Uncompressed)]
    compression: WriteCompression,

    /// The number of row per page in a column chunk.
    #[arg(long)]
    rows_per_page: Option<usize>,

    /// The number of row per groups in a row group.
    #[arg(long)]
    rows_per_group: Option<usize>,

    /// Data type for each column. Syntax: `--dtypes <column_name>=<data_type>`. Supported data types: [boolean, int32, int64, float, double, string].
    #[arg(long, value_parser = parse_column_dtype)]
    dtypes: Vec<(String, Type)>,
}

impl WriteCommand {
    pub fn execute(self) -> Result<()> {
        let mut file = File::open(&self.csv)?;
        let mut data = Vec::new();
        file.read_to_end(&mut data)?;

        let data_types_override: HashMap<String, Type> = self.dtypes.clone().into_iter().collect();

        let out = write_parquet(
            data,
            self.dictionary,
            &self.encodings,
            self.parquet_compression(),
            self.rows_per_page,
            self.rows_per_group,
            Some(data_types_override),
        )?;

        let mut file = OpenOptions::new()
            .create(true)
            .truncate(true)
            .write(true)
            .open(self.parquet)?;
        file.write_all(&out)?;
        Ok(())
    }

    fn parquet_compression(&self) -> Compression {
        match self.compression {
            WriteCompression::Uncompressed => Compression::UNCOMPRESSED,
            WriteCompression::Snappy => Compression::SNAPPY,
        }
    }
}

fn parse_column_encoding(s: &str) -> Result<(String, Encoding)> {
    let (column, encoding) = s
        .split_once('=')
        .with_context(|| "Invalid column=encoding: no `=` found in {s}")?;
    let encoding = match encoding {
        "rle" => Encoding::RLE,
        _ => {
            bail!("Unsupported encoding, expected: [rle]")
        }
    };
    Ok((column.to_string(), encoding))
}

fn parse_column_dtype(s: &str) -> Result<(String, Type)> {
    let (column, data_type) = s
        .split_once('=')
        .with_context(|| "Invalid column=data_type: no `=` found in {s}")?;
    let data_type = match data_type {
        "boolean" => Type::BOOLEAN,
        "int32" => Type::INT32,
        "int64" => Type::INT64,
        "float" => Type::FLOAT,
        "double" => Type::DOUBLE,
        "string" => Type::BYTE_ARRAY,
        _ => {
            bail!("Unsupported data type, expected: [boolean, int32, int64, float, double, string]")
        }
    };
    Ok((column.to_string(), data_type))
}
