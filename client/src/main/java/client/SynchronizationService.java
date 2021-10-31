package client;

import files.SimpleFile;
import files.ClientFile;
import files.ClientDirectory;
import files.FileTreeSnapshot;
import handlers.MessageHandler;
import operations.FileOperation;

import java.nio.file.Path;
import java.util.*;

public class SynchronizationService {
    private static Map<Path, SimpleFile> serverFileList;
    private static Map<Long, ArrayList<SimpleFile>> serverFileHashes;


    public static void synchronizeWithServer(FileTreeSnapshot clientFTS, List<SimpleFile> serverSideUserFilesList) {
        System.out.println("Start synchronization...");
        parseServerSideList(serverSideUserFilesList);
        System.out.println("\nFILES ON SERVER LIST:");
        for (SimpleFile sp : serverFileList.values()) {
            System.out.println(sp);
        }
        System.out.println();
        compare(clientFTS.getInitialDirectory());
        deleteRemainingFilesOnServer();
        System.out.println("Synchronization complete.");
    }

    private static void parseServerSideList(List<SimpleFile> filesList) {
        serverFileList = new TreeMap<>();
        serverFileHashes = new HashMap<>();
        for (SimpleFile f : filesList) {
            serverFileList.put(Path.of(f.getAbsolutePath()), f);
            ArrayList<SimpleFile> currPathListForHash = serverFileHashes.getOrDefault(f.getCrc32Hash(), new ArrayList<>());
            if (currPathListForHash.isEmpty()) {
                serverFileHashes.put(f.getCrc32Hash(), currPathListForHash);
            } else {
                currPathListForHash.add(f);
            }
        }
    }

    private static void compare(ClientDirectory startDir) {
        for (ClientDirectory dir : startDir.getSubdirectories()) {
            compare(dir);
        }
        for (ClientFile f : startDir.getFiles()) {
            SimpleFile onServerSimpleFile = serverFileList.remove(f.getPathWithoutRootPart());
            FileOperation fo = null;
            boolean isFileExistsOnServerSide = false;
            System.out.println("Current client file: " + f);

            if (onServerSimpleFile != null) {
                System.out.println("File's path on server and client are the same");
                if (!onServerSimpleFile.compare(f)) {
                    System.out.println("but files are different");
                    for (SimpleFile sp : serverFileHashes.getOrDefault(f.getCrc32Hash(), new ArrayList<>())) {
                        System.out.println("Find file on server with same hashsum: " + sp.getAbsolutePath());
                        if (sp.compareBySizeAndExtension(f)) {
                            System.out.println("File has same size and extension. Try to copy...");
                            fo = FileOperation.copy(FileOperation.Entity.FILE, Path.of(sp.getAbsolutePath()), f.getFilePath());
                            isFileExistsOnServerSide = true;
                            break;
                        }
                    }
                    if (!isFileExistsOnServerSide)
                        fo = FileOperation.modify(FileOperation.Entity.FILE, f.getFilePath());
                }
            } else {
                System.out.println("File's path on server and client aren't the same");

                for (SimpleFile sp : serverFileHashes.getOrDefault(f.getCrc32Hash(), new ArrayList<>())) {
                    System.out.println("Find file on server with same hashsum: " + sp.getAbsolutePath());
                    if (sp.compareBySizeAndExtension(f)) {
                        System.out.println("File has same size and extension. Try to copy...");
                        fo = FileOperation.copy(FileOperation.Entity.FILE, Path.of(sp.getAbsolutePath()), f.getFilePath());
                        isFileExistsOnServerSide = true;
                        break;
                    }
                }
                if (!isFileExistsOnServerSide) fo = FileOperation.create(FileOperation.Entity.FILE, f.getFilePath());
            }
            if (fo != null) new MessageHandler().send(fo, Client.getCurrentClientSession());
            System.out.println();
        }
    }

    private static void deleteRemainingFilesOnServer() {
        for (SimpleFile sp : serverFileList.values()) {
            FileOperation fo = FileOperation.delete(FileOperation.Entity.FILE, Path.of(sp.getAbsolutePath()));
            new MessageHandler().send(fo, Client.getCurrentClientSession());
        }
    }
}
