# Write you a parquet parser

This book introduces you how to implement a parquet parser from scratch.
Read it at <https://dqkqd.github.io/parquet-parser/>.

## How to start

To start, clone [this repository](https://github.com/dqkqd/parquet-parser/), and checkout the
`starter` branch.

```bash
git clone https://github.com/dqkqd/parquet-parser.git

cd parquet-parser

git checkout starter
```

## How to test

Each step has several test cases in `tests/integration/mod.rs`, all of them are disabled by default.
You should uncomment the correct tests when implementing a specific step.

```rust,ignore
// mod step01_magic;
// mod step02_file_metadata;
// mod step03_data_page;
// mod step04_data_pages;
// mod step05_plain_decoder;
// mod step06_column;
// mod step07_row_group;
// mod step08_parquet_file;
// mod step09_boolean_column;
// mod step10_01_uleb128_decoder;
// mod step10_02_run;
// mod step10_03_runs;
// mod step10_04_run_decoder;
// mod step10_05_runs_decoder;
// mod step11_01_definition_levels_decoder;
// mod step11_02_nulls_decoder;
// mod step12_01_dictionary_page;
// mod step12_02_dictionary_decoder_two_values;
// mod step12_03_dictionary_decoder_one_value;
// mod step12_04_dictionary_decoder_bit_packed;
// mod step12_05_dictionary_decoder_rle;
// mod step13_compression;
```

## Tips

- The codebase relies heavily on external crates such as
  [bytes](https://docs.rs/bytes/latest/bytes/); consider checking their docs when implementing.
- Having a look at the corresponding tests before implementation is always a good idea to understand
  what they actually test for.
