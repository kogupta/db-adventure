package intro.db.storage;

import java.nio.file.Path;

public record FileManager(Path directory, int blockSize) {
    public record BlockId(String file, int blockNum) {}

    static void main() {

    }

    public static final class Page {

    }
}
