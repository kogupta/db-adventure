use std::fs::File;

use crate::reader::read_parquet;
use anyhow::Result;
use clap::Parser;
use polars::{io::SerReader, prelude::ParquetReader};

/// Verify the current parser output with the official parquet parser.
#[derive(Parser)]
pub struct VerifyCommand {
    /// The input parquet file
    pub input: String,
}

impl VerifyCommand {
    pub fn execute(self) -> Result<()> {
        let df = read_parquet(&self.input)?;

        let df_from_polars = {
            let file = File::open(&self.input)?;
            let reader = ParquetReader::new(file);
            reader.finish()?
        };

        assert_eq!(df, df_from_polars);

        Ok(())
    }
}
