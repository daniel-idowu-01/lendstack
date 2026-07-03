package ng.lendstack.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(@Value("${lendstack.storage.document-dir}") String documentDir) {
        this.root = Path.of(documentDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(String directory, String originalFileName, InputStream content) {
        String extension = "";
        int dot = originalFileName.lastIndexOf('.');
        if (dot >= 0 && dot < originalFileName.length() - 1) {
            extension = originalFileName.substring(dot).toLowerCase();
        }
        String relative = "%s/%s%s".formatted(directory, UUID.randomUUID(), extension);
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store document", e);
        }
        return relative;
    }

    @Override
    public InputStream retrieve(String storagePath) {
        try {
            return Files.newInputStream(resolve(storagePath));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read document " + storagePath, e);
        }
    }


    private Path resolve(String relative) {
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Illegal storage path");
        }
        return resolved;
    }
}
