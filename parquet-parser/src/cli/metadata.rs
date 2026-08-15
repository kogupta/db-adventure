use std::fs::File;

use anyhow::Result;
use clap::Parser;
use parquet::{
    file::reader::{FileReader, SerializedFileReader},
    schema::printer::print_parquet_metadata,
};

/// Print the metadata for a parquet file
#[derive(Parser)]
pub struct MetadataCommand {
    /// The input parquet file
    pub input: String,
}

impl MetadataCommand {
    pub fn execute(self) -> Result<()> {
        let file = File::open(&self.input)?;
        let reader = SerializedFileReader::new(file)?;
        let meta = reader.metadata();
        print_parquet_metadata(&mut std::io::stdout(), meta);
        Ok(())
    }
}
