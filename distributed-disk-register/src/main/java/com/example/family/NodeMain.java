package com.example.family;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class NodeMain {
    public static int MY_PORT;
    private static final int START_PORT = 5555;
    private static final int PRINT_INTERVAL_SECONDS = 10;

    // Config Değişkenleri
    public static int TOLERANCE = 1;
    public static boolean USE_BUFFERED = true;

    // Haritalar ve Listeler
    private static final NodeRegistry registry = new NodeRegistry();
    private static final Map<Integer, List<NodeInfo>> messageLocations = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException, InterruptedException {
        // 1. Config Yükle
        loadToleranceConfig();

        // 2. Port Bul ve Başlat
        int port = findAvailablePort(START_PORT);
        NodeInfo self = new NodeInfo("127.0.0.1", port);
        MY_PORT = port;
        System.out.println("Node started on " + self.getHost() + ":" + self.getPort() + " (TOLERANCE=" + TOLERANCE + ")");

        // 3. gRPC Sunucusunu Başlat (Aile İçi Haberleşme)
        Server server = ServerBuilder.forPort(port)
                .addService(new FamilyServiceImpl(registry, self))
                .build()
                .start();

        // 4. Eğer Lidersek (5555), İstemciyi (TCP 6666) Dinle
        if (port == START_PORT) {
            // Kendimizi listeye ekleyelim ki sayımız doğru çıksın
            registry.addNode(self);

            new Thread(() -> startLeaderTCPServer()).start();
            startFamilyPrinter(registry, self);
        } else {
            // Üye isek Lidere bağlan (GERÇEK BAĞLANTI)
            joinFamily(self);
            startFamilyPrinter(registry, self);
        }

        server.awaitTermination();
    }

    // --- LİDER TCP SUNUCUSU (CLIENT İÇİN) ---
    private static void startLeaderTCPServer() {
        try (ServerSocket serverSocket = new ServerSocket(6666)) {
            System.out.println("Leader listening for text on TCP 127.0.0.1:6666");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split(" ", 3);
                String command = parts[0];

                if ("SET".equalsIgnoreCase(command)) {
                    int id = Integer.parseInt(parts[1]);
                    String data = parts[2];

                    // 1. Önce Lider Kendine Yazar
                    saveToDisk(id, data);

                    // 2. Sonra Üyelere Dağıtır (GERÇEK REPLİKASYON)
                    List<NodeInfo> savedNodes = replicateToFamily(id, data);
                    savedNodes.add(new NodeInfo("127.0.0.1", START_PORT)); // Kendimizi de ekle

                    messageLocations.put(id, savedNodes); // Kayıt defterine işle

                    out.println("OK");
                }
                else if ("GET".equalsIgnoreCase(command)) {
                    int id = Integer.parseInt(parts[1]);
                    boolean found = false;
                    String resultData = "";

                    // 1. Önce Lider Kendi Diskine Baksın
                    File f = new File("node_" + MY_PORT + "/" + id + ".msg");
                    if (f.exists()) {
                        resultData = new String(java.nio.file.Files.readAllBytes(f.toPath()));
                        out.println("FOUND local: " + resultData);
                        found = true;
                    }

                    // 2. Kendinde Yoksa (veya Crash Testi için) Listeden Ara
                    if (!found) {
                        List<NodeInfo> holders = messageLocations.getOrDefault(id, new ArrayList<>());
                        System.out.println("🔍 GET " + id + " requested. Locations: " + holders);

                        for (NodeInfo holder : holders) {
                            if (holder.getPort() == MY_PORT) continue; // Kendine zaten baktı

                            System.out.println("👉 Asking " + holder.getPort() + "...");

                            ManagedChannel channel = ManagedChannelBuilder.forAddress(holder.getHost(), holder.getPort())
                                    .usePlaintext()
                                    .build();
                            try {
                                FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

                                // 2 Saniye Bekle (Crash olmuşsa hemen anla)
                                GetValueResponse response = stub.withDeadlineAfter(2, TimeUnit.SECONDS)
                                        .getValue(GetValueRequest.newBuilder().setId(id).build());

                                if (response.getFound()) {
                                    out.println("FOUND from " + holder.getPort() + ": " + response.getData());
                                    found = true;
                                    channel.shutdown();
                                    break; // Bulduk, döngüden çık!
                                }
                            } catch (Exception e) {
                                System.err.println("❌ MEMBER CRASH DETECTED: " + holder.getPort() + " (Trying next replica...)");
                                // CRASH SENARYOSU BURADA YAKALANIYOR
                            } finally {
                                channel.shutdown();
                            }
                        }
                    }

                    if (!found) {
                        out.println("NOT FOUND (All replicas lost)");
                    }
                }
            }
        } catch (Exception e) {
            // Sessizce kapat
        }
    }

    // --- GERÇEK REPLİKASYON MANTIĞI ---
    private static List<NodeInfo> replicateToFamily(int id, String data) {
        List<NodeInfo> family = registry.getNodes();
        // Kendimizi listeden çıkaralım
        family.removeIf(n -> n.getPort() == START_PORT);
        Collections.shuffle(family); // Rastgele seç

        List<NodeInfo> successNodes = new ArrayList<>();
        int successCount = 0;

        for (NodeInfo member : family) {
            // Yeterince kopyaladıysak dur
            if (successCount >= TOLERANCE) break;

            // gRPC ile üyeye gönder
            ManagedChannel channel = ManagedChannelBuilder.forAddress(member.getHost(), member.getPort())
                    .usePlaintext()
                    .build();
            try {
                FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);
                StoreResult result = stub.store(StoredMessage.newBuilder().setId(id).setData(data).build());

                if (result.getSuccess()) {
                    successNodes.add(member);
                    successCount++;
                }
            } catch (Exception e) {
                System.err.println("⚠️ Replication failed to " + member.getPort());
            } finally {
                channel.shutdown();
            }
        }
        return successNodes;
    }

    // --- GERÇEK KATILIM (JOIN) MANTIĞI ---
    private static void joinFamily(NodeInfo self) {
        System.out.println("Attempting to join family at 127.0.0.1:" + START_PORT);

        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", START_PORT)
                .usePlaintext()
                .build();
        try {
            FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);
            JoinResponse response = stub.joinFamily(JoinRequest.newBuilder()
                    .setHost(self.getHost())
                    .setPort(self.getPort())
                    .build());

            if (response.getSuccess()) {
                System.out.println("✅ Joined successfully! Leader said: " + response.getMessage());
            } else {
                System.err.println("❌ Join failed: " + response.getMessage());
            }
        } catch (Exception e) {
            System.err.println("❌ Could not connect to Leader: Is it running?");
        }
        // Kanalı açık tutmuyoruz, sadece kayıt olduk
    }

    // --- DİSKE YAZMA (FSYNC + GECİKME SİMÜLASYONU) ---
    public static void saveToDisk(int id, String data) throws IOException {
        // ARTIK HERKESİN KENDİ KLASÖRÜ VAR: node_5555, node_5556...
        File dir = new File("node_" + MY_PORT);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, id + ".msg");

        if (USE_BUFFERED) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(data);
            }
        } else {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data.getBytes());
                fos.flush();
                fos.getFD().sync();
                try { Thread.sleep(10); } catch (InterruptedException e) {}
            }
        }
    }

    // --- YAPILANDIRMA VE PORT BULMA ---
    private static void loadToleranceConfig() {
        File configFile = new File("tolerance.conf");
        if (!configFile.exists()) configFile = new File("../tolerance.conf");

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("TOLERANCE=")) {
                    TOLERANCE = Integer.parseInt(line.split("=")[1].trim());
                } else if (line.startsWith("USE_BUFFERED=")) {
                    USE_BUFFERED = Boolean.parseBoolean(line.split("=")[1].trim());
                }
            }
            System.out.println("✅ Config loaded: TOLERANCE=" + TOLERANCE + ", USE_BUFFERED=" + USE_BUFFERED);
        } catch (Exception e) {
            System.err.println("Using defaults.");
        }
    }

    private static int findAvailablePort(int startPort) {
        int port = startPort;
        while (true) {
            try (ServerSocket ss = new ServerSocket(port)) { return port; }
            catch (IOException e) { port++; }
        }
    }

    // --- RAPORLAMA ---
    private static void startFamilyPrinter(NodeRegistry registry, NodeInfo self) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            File dir = new File("node_" + self.getPort());
            int localCount = 0;
            if (dir.exists()) {
                String[] files = dir.list((d, name) -> name.endsWith(".msg"));
                if (files != null) localCount = files.length;
            }

            if (self.getPort() == START_PORT) {
                System.out.println("👑 [LEADER REPORT] I hold " + localCount + " messages. Family size: " + registry.getNodes().size());
            } else {
                System.out.println("📊 [MEMBER STATS] I hold " + localCount + " messages.");
            }
        }, 5, PRINT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }
}