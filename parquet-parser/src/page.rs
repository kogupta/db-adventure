use anyhow::Result;
use bytes::Bytes;

use crate::format::{ColumnMetaData, CompressionCodec, Encoding, PageHeader};

#[derive(Debug)]
pub enum Page {
    /// A data page stores the actual data for a single column. It contains 4 pieces of information.
    ///
    /// ```text
    /// PageHeader
    /// repetition_levels (optional)
    /// definition_levels (optional)
    /// encoded_values
    /// ```
    ///
    /// There are two assumptions at the moment that make the implementation simpler:
    /// - no nested data types support: `repetition_levels` **must not** be present.
    /// - all columns can contain nulls: `definition_levels` **must always** be present.
    ///
    /// The `definition_levels` value includes the length itself in the bytes. This is for compatible with the spec.
    /// And its data is encoded using RLE bit-packed encoding.
    ///
    /// [data page file format]: https://parquet.apache.org/docs/file-format/data-pages/
    DataPage {
        page_header: PageHeader,
        definition_levels: Bytes,
        encoded_values: Bytes,
    },
    /// A dictionary page contains the actual values for a column chunk, the actual indexes are store
    /// in the data page itself.
    ///
    /// [dictionary page for column chunk]: https://parquet.apache.org/docs/file-format/data-pages/columnchunks/
    /// [dictionary encoding]: https://parquet.apache.org/docs/file-format/data-pages/encodings/#DICTIONARY
    DictionaryPage {
        page_header: PageHeader,
        encoded_values: Bytes,
    },
}

impl Page {
    pub fn num_values(&self) -> usize {
        match self {
            Page::DataPage { page_header, .. } => {
                page_header.data_page_header.as_ref().unwrap().num_values as usize
            }
            Page::DictionaryPage { page_header, .. } => {
                page_header
                    .dictionary_page_header
                    .as_ref()
                    .unwrap()
                    .num_values as usize
            }
        }
    }

    pub fn encoding(&self) -> Encoding {
        match self {
            Page::DataPage { page_header, .. } => {
                page_header.data_page_header.as_ref().unwrap().encoding
            }
            Page::DictionaryPage { page_header, .. } => {
                page_header
                    .dictionary_page_header
                    .as_ref()
                    .unwrap()
                    .encoding
            }
        }
    }

    pub fn encoded_values(&self) -> Bytes {
        match self {
            Page::DataPage { encoded_values, .. } => encoded_values.clone(),
            Page::DictionaryPage { encoded_values, .. } => encoded_values.clone(),
        }
    }

    pub fn definition_levels(&self) -> Option<Bytes> {
        match self {
            Page::DataPage {
                definition_levels, ..
            } => Some(definition_levels.clone()),
            Page::DictionaryPage { .. } => None,
        }
    }

    pub fn is_dictionary(&self) -> bool {
        matches!(self, Page::DictionaryPage { .. })
    }
}

/// Read a page data into [`Page`].
///
/// This function receives `data` at page boundary,  and `codec` (to handle compression),
/// and returns a [`Page`] and remaining bytes.
#[allow(unused_variables)]
pub fn read_page(data: Bytes, codec: CompressionCodec) -> Result<(Page, Bytes)> {
    todo!("step03: read a single page data")
}

/// Pages for a column chunk
#[derive(Debug)]
pub struct Pages {
    pub data_pages: Vec<Page>,
    pub dictionary_page: Option<Page>,
}

/// Read all [`Page`] for a given column chunk.
///
/// All pages for a given column chunk are written back to back.
#[allow(unused_variables)]
pub fn read_pages(data: Bytes, column_metadata: &ColumnMetaData) -> Result<Pages> {
    todo!("step04: read all pages for a given column chunk")
}
