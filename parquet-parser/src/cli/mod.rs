mod metadata;
mod read;
mod verify;
mod write;

use crate::cli::verify::VerifyCommand;
use crate::cli::write::WriteCommand;
use crate::cli::{metadata::MetadataCommand, read::ReadCommand};
use anyhow::Result;
use clap::{Parser, Subcommand};

#[derive(Parser)]
pub struct Cli {
    #[command(subcommand)]
    commands: Commands,
}

#[derive(Subcommand)]
enum Commands {
    Read(ReadCommand),
    Write(WriteCommand),
    Metadata(MetadataCommand),
    Verify(VerifyCommand),
}

impl Cli {
    pub fn execute(self) -> Result<()> {
        match self.commands {
            Commands::Read(read_command) => read_command.execute(),
            Commands::Metadata(metadata_command) => metadata_command.execute(),
            Commands::Write(write_command) => write_command.execute(),
            Commands::Verify(verify_command) => verify_command.execute(),
        }
    }
}
