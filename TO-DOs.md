# 📝 Proje İlerleme ve Görev Takibi (TO-DOs)

Bu dosya, **HaToKuSe (Hata-Tolere Kuyruk Servisi)** projesinin geliştirilme süreçlerini, tamamlanan görevleri ve test senaryolarını içerir.

## 👨‍💻 Grup Bilgisi
* **Adı Soyadı:** Murat GÜMÜŞ
* **Öğrenci No:** 22060674

---

## ✅ 1. Aşama – Başlangıç ve Temel Yapı
- [x] **GitHub & Organizasyon:** Şablon repo fork edildi, proje iskeleti oluşturuldu.
- [x] **TCP Sunucu (Lider):** Lider sunucunun (Port 5555) dış dünyadan (Port 6666) gelen TCP isteklerini dinlemesi sağlandı.
- [x] **Komut Ayrıştırma (Parsing):** İstemciden gelen `SET` ve `GET` komutlarını işleyen yapı kuruldu.

## ✅ 2. Aşama – Disk IO ve Performans Yönetimi
- [x] **Dosya Yapısı:** Her sunucunun kendi portuna özel klasör (`node_5555` vb.) oluşturması sağlandı.
- [x] **Buffered IO:** `BufferedWriter` kullanılarak yüksek performanslı (RAM tabanlı) yazma modu eklendi.
- [x] **Unbuffered IO (Sync):** `FileOutputStream` ve `fsync` kullanılarak, veri güvenliği odaklı (yavaş) yazma modu eklendi.
- [x] **Mekanik Disk Simülasyonu:** Unbuffered modda, donanım gecikmesini simüle etmek için yapay `sleep` eklendi.
- [x] **Config:** `tolerance.conf` dosyasına `USE_BUFFERED` parametresi eklendi.

## ✅ 3. Aşama – gRPC ve Protobuf Modellemesi
- [x] **Protobuf Tanımı:** `family.proto` dosyası oluşturuldu; `StoredMessage`, `JoinRequest`, `GetValueRequest` mesajları tanımlandı.
- [x] **Service Implementasyonu:** `FamilyServiceImpl` sınıfı ile gRPC fonksiyonları (`store`, `joinFamily`, `getValue`) yazıldı.
- [x] **RPC Entegrasyonu:** Lider ve üyeler arası haberleşme tamamen gRPC üzerine taşındı.

## ✅ 4. Aşama – Dağıtık Kayıt ve Tolerans
- [x] **Tolerans Yapılandırması:** `tolerance.conf` dosyasından `TOLERANCE` değerinin okunması sağlandı.
- [x] **Replikasyon Mantığı:** Liderin, gelen mesajı kendine yazdıktan sonra `N` adet üyeye daha kopyalaması sağlandı.
- [x] **Üye Takibi:** Liderin hangi mesajın hangi üyelerde olduğunu takip ettiği `messageLocations` haritası (Map) oluşturuldu.

## ✅ 6. Aşama – Yük Dengeleme (Load Balancing)
- [x] **Dinamik Tolerans:** Sistem `TOLERANCE=1..7` aralığında dinamik çalışabilir hale getirildi.
- [x] **Yük Dağıtımı:** Mesajların üyelere dağıtılırken `Collections.shuffle` kullanılarak rastgele ve eşit (dengeli) dağıtılması sağlandı.
- [x] **Raporlama:** Lider ve üyelerin konsola "Ben şu kadar mesaj tutuyorum" (`I hold X messages`) raporu basması sağlandı.
- [x] **Test:** 1000 mesajlık yük testinde, üyelerin yükü eşit (~%50) paylaştığı gözlemlendi.

## ✅ 7. Aşama – Crash Senaryoları ve Recovery (Hata Kurtarma)
- [x] **Crash Simülasyonu:** Çalışan bir üye terminalinin kapatılması durumunda sistemin çökmediği doğrulandı.
- [x] **Failover (Hata Telafisi):** Liderin `GET` isteği sırasında bir üyeye ulaşamazsa (Crash), listedeki diğer üyeyi (Replica) denemesi sağlandı.
- [x] **Fallback (Yayın):** Liderin mesajın yerini bilmediği durumlarda tüm üyelere sorarak (Broadcast) veriyi bulma yeteneği eklendi.
- [x] **Test:** Liderden veri silinip, ana yedek kapatıldığında bile verinin 3. üyeden başarıyla çekildiği kanıtlandı.