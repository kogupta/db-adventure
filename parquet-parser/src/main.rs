
use anyhow::Result;
use clap::Parser;
use parquet_parser::cli::Cli;

fn main() -> Result<()> {
    let cli = Cli::parse();
    cli.execute()?;
    Ok(())
}
