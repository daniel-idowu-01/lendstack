package ng.lendstack.storage;

import java.io.InputStream;


public interface StorageService {


    String store(String directory, String originalFileName, InputStream content);

    InputStream retrieve(String storagePath);
}
