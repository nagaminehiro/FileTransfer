package io.grpc.examples.filetransfer;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.filetransfer.FileTransferProto.FileChunk;
import io.grpc.examples.filetransfer.FileTransferProto.FileRequest;
import io.grpc.examples.filetransfer.FileTransferProto.UploadStatus;
import io.grpc.stub.StreamObserver;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileTransferClient {
    private static final Logger logger = Logger.getLogger(FileTransferClient.class.getName());
    private final FileTransferServiceGrpc.FileTransferServiceStub asyncStub;

    public FileTransferClient(ManagedChannel channel) {
        asyncStub = FileTransferServiceGrpc.newStub(channel);
    }

    public void uploadFile(String filePath) throws InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<UploadStatus> responseObserver = new StreamObserver<UploadStatus>() {
            @Override
            public void onNext(UploadStatus status) {
                logger.info("Upload status: " + status.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.SEVERE, "Upload failed: {0}", t);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                logger.info("File upload completed.");
                finishLatch.countDown();
            }
        };

        StreamObserver<FileChunk> requestObserver = asyncStub.uploadFile(responseObserver);
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                FileChunk chunk = FileChunk.newBuilder()
                        .setContent(com.google.protobuf.ByteString.copyFrom(buffer, 0, bytesRead))
                        .setFileName(filePath)
                        .build();
                requestObserver.onNext(chunk);
            }
        } catch (IOException e) {
            requestObserver.onError(e);
        }
        requestObserver.onCompleted();
        finishLatch.await(1, TimeUnit.MINUTES);
    }

    public void downloadFile(String fileName, String savePath) {
        FileRequest request = FileRequest.newBuilder().setFileName(fileName).build();
        StreamObserver<FileChunk> responseObserver = new StreamObserver<FileChunk>() {
            private FileOutputStream fos;

            @Override
            public void onNext(FileChunk chunk) {
                try {
                    if (fos == null) {
                        fos = new FileOutputStream(savePath);
                    }
                    fos.write(chunk.getContent().toByteArray());
                } catch (IOException e) {
                    onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.SEVERE, "Download failed: {0}", t);
            }

            @Override
            public void onCompleted() {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                    logger.info("File download completed.");
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Failed to close file output stream: {0}", e);
                }
            }
        };

        asyncStub.downloadFile(request, responseObserver);
    }

    public static void main(String[] args) throws Exception {
        String target = "localhost:50051";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();
        try {
            FileTransferClient client = new FileTransferClient(channel);
            client.uploadFile("path/to/upload/file.txt");
            client.downloadFile("file.txt", "path/to/save/file.txt");
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}