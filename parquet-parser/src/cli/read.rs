use crate::reader::read_parquet;
use anyhow::Result;
use clap::Parser;

/// Read parquet file
#[derive(Parser)]
pub struct ReadCommand {
    /// The input parquet file
    pub input: String,
}

impl ReadCommand {
    pub fn execute(self) -> Result<()> {
        let df = read_parquet(&self.input)?;
        println!("{}", df);
        Ok(())
    }
}
