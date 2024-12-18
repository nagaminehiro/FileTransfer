package io.grpc.examples.filetransfer;

import io.grpc.examples.filetransfer.FileTransferProto.FileChunk;
import io.grpc.examples.filetransfer.FileTransferProto.FileRequest;
import io.grpc.examples.filetransfer.FileTransferProto.UploadStatus;
import io.grpc.stub.StreamObserver;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileTransferServiceImpl extends FileTransferServiceGrpc.FileTransferServiceImplBase {
    @Override
    public StreamObserver<FileChunk> uploadFile(StreamObserver<UploadStatus> responseObserver) {
        return new StreamObserver<FileChunk>() {
            private FileOutputStream fos;

            @Override
            public void onNext(FileChunk chunk) {
                try {
                    if (fos == null) {
                        fos = new FileOutputStream(chunk.getFileName());
                    }
                    fos.write(chunk.getContent().toByteArray());
                } catch (IOException e) {
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                    responseObserver.onNext(UploadStatus.newBuilder().setSuccess(true).setMessage("File uploaded successfully").build());
                    responseObserver.onCompleted();
                } catch (IOException e) {
                    responseObserver.onError(e);
                }
            }
        };
    }

    @Override
    public void downloadFile(FileRequest request, StreamObserver<FileChunk> responseObserver) {
        try (FileInputStream fis = new FileInputStream(request.getFileName())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                FileChunk chunk = FileChunk.newBuilder().setContent(com.google.protobuf.ByteString.copyFrom(buffer, 0, bytesRead)).setFileName(request.getFileName()).build();
                responseObserver.onNext(chunk);
            }
            responseObserver.onCompleted();
        } catch (IOException e) {
            responseObserver.onError(e);
        }
    }
}