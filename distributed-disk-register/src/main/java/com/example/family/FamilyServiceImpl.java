package com.example.family;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.io.*;
import java.util.concurrent.TimeUnit;

import static com.example.family.NodeMain.USE_BUFFERED;

public class FamilyServiceImpl extends FamilyServiceGrpc.FamilyServiceImplBase {

    private final NodeRegistry registry;
    private final NodeInfo self;

    public FamilyServiceImpl(NodeRegistry registry, NodeInfo self) {
        this.registry = registry;
        this.self = self;
    }

    @Override
    public void joinFamily(JoinRequest request, StreamObserver<JoinResponse> responseObserver) {
        NodeInfo newNode = new NodeInfo(request.getHost(), request.getPort());
        registry.addNode(newNode);
        System.out.println("New family member joined: " + newNode.getHost() + ":" + newNode.getPort());

        JoinResponse response = JoinResponse.newBuilder()
                .setMessage("Welcome to the family!")
                .setSuccess(true)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void store(StoredMessage request, StreamObserver<StoreResult> responseObserver) {
        int id = request.getId();
        String data = request.getData();

        // Liderden gelen mesajı alıyoruz
        System.out.println("📥 [gRPC STORE] Received -> ID: " + id);

        try {
            // DİSKE YAZ (Liderdeki Fsync mantığının aynısını kullan)
            saveToDisk(id, data);

            // Eğer Lider bizsek (5555), diğerlerine de yaymamız (Replicate) gerekebilir.
            // Ama burası basit tutulmuştur. Lider NodeMain içinde yönetir.
            if (self.getPort() == 5555) {
                replicateToOthers(id, data);
            }

            StoreResult result = StoreResult.newBuilder()
                    .setSuccess(true)
                    .setMessage("Saved successfully to " + self.getPort())
                    .build();
            responseObserver.onNext(result);
            responseObserver.onCompleted();

        } catch (IOException e) {
            System.err.println("❌ Write Error: " + e.getMessage());
            responseObserver.onError(e);
        }
    }

    // --- DİSKE YAZMA  ---
    private void saveToDisk(int id, String data) throws IOException {
        File dir = new File("node_" + self.getPort());
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, id + ".msg");

        if (NodeMain.USE_BUFFERED) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(data);
            }
        } else {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data.getBytes());
                fos.flush();
                fos.getFD().sync();

                // SİMÜLASYON GECİKMESİ
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // --- LİDERİN ÜYELERE DAĞITMASI ---
    private void replicateToOthers(int id, String data) {
        int sentCount = 0;
        for (NodeInfo node : registry.getNodes()) {
            if (node.getPort() == self.getPort()) continue; // Kendine atma
            if (sentCount >= NodeMain.TOLERANCE) break;

            try {
                ManagedChannel channel = ManagedChannelBuilder.forAddress(node.getHost(), node.getPort())
                        .usePlaintext()
                        .build();
                FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

                stub.store(StoredMessage.newBuilder().setId(id).setData(data).build());

                System.out.println("✅ Replicated ID " + id + " to " + node.getPort());
                sentCount++;

                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("⚠️ Warning: Could not replicate to " + node.getPort());
            }
        }
    }
    public void getValue(GetValueRequest request, io.grpc.stub.StreamObserver<GetValueResponse> responseObserver) {
        int id = request.getId();
        // Kendi klasörümden okumaya çalışıyorum
        File file = new File("node_" + self.getPort() + "/" + id + ".msg");

        GetValueResponse.Builder response = GetValueResponse.newBuilder();

        if (file.exists()) {
            try {
                // Dosya varsa içeriğini oku
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                response.setFound(true).setData(content);
            } catch (IOException e) {
                response.setFound(false);
            }
        } else {
            response.setFound(false);
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
}