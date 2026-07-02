package ng.lendstack.storage;

import java.io.InputStream;

/**
 * Document blob storage. Local-disk implementation for now; swap in an
 * S3/GCS-backed bean later without touching business logic.
 */
public interface StorageService {

    /** Stores the stream and returns an opaque storage path for later retrieval. */
    String store(String directory, String originalFileName, InputStream content);

    InputStream retrieve(String storagePath);
}
