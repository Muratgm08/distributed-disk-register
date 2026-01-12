# 🛡️ HaToKuSe

### *Hata-Tolere Kuyruk Servisi*

HaToKuSe, **Sistem Programlama** dersi kapsamında geliştirilmiş; **Java**, **gRPC** ve **Protocol Buffers** tabanlı, dağıtık, ölçeklenebilir ve hata toleranslı bir mesajlaşma / abonelik sistemidir.

Proje, ilkel soket programlama yerine modern RPC mimarileri kullanılarak gerçekçi bir dağıtık sistem simülasyonu sunar.

---

## 👥 Proje Ekibi

* **Adı Soyadı:** Murat GÜMÜŞ
* **Öğrenci No:** 22060674

---

## 🚀 Proje Özellikleri

Bu sistem aşağıdaki temel yetenekleri sunar:

### 1️⃣ Lider–Takipçi Mimarisi

* Sistem, **5555 portu** üzerinden çalışan bir **Lider Sunucu** ve ona dinamik olarak bağlanan **Üye Sunucular**dan oluşur.
* İlk başlatılan sunucu lider olur, sonraki sunucular otomatik olarak lidere bağlanır.

### 2️⃣ Hata Toleransı (Fault Tolerance)

* `tolerance.conf` dosyasında belirtilen **TOLERANCE** değeri kadar kopya oluşturulur.
* Gelen her mesaj, **N farklı sunucuya** yedeklenerek veri kaybı önlenir.

### 3️⃣ Yük Dengeleme (Load Balancing)

* Lider, mesajları üyeler arasında **rastgele ve dengeli** şekilde dağıtır.
* Sistem, artan yük altında ölçeklenebilir yapıdadır.

### 4️⃣ IO Performans Yönetimi

Disk yazma işlemleri için iki farklı mod desteklenir:

* **Buffered IO:** Yüksek performans (RAM tabanlı)
* **Unbuffered IO:** Yüksek güvenlik (fsync ile disk senkronizasyonu)

### 5️⃣ Periyodik Raporlama

* Lider ve Üyeler, belirli aralıklarla:

    * İşlenen mesaj sayısını
    * Bağlı üye durumlarını
      konsola raporlar.

---

## ⚙️ Kurulum ve Çalıştırma

Proje **Maven** tabanlıdır.

### 🔧 1. Derleme (Build)

Proje dizininde aşağıdaki komutu çalıştırın:

```bash
mvn clean package
```

---

### 🛠️ 2. Yapılandırma (`tolerance.conf`)

Proje dizininde veya JAR dosyasının yanında bulunan `tolerance.conf` dosyasını düzenleyin:

```properties
TOLERANCE=2
USE_BUFFERED=true
```

| Ayar           | Açıklama                                                                  |
| -------------- | ------------------------------------------------------------------------- |
| `TOLERANCE`    | Mesajın kaç farklı sunucuda yedekleneceğini belirler                      |
| `USE_BUFFERED` | `true`: Yüksek performans (RAM) <br> `false`: Yüksek güvenlik (disk sync) |

---

### 🖥️ 3. Sunucuları Başlatma

Sistemi test etmek için birden fazla terminal açın ve `target` dizinine gidin.

#### Terminal 1 – Lider Sunucu

> 5555 portu boşsa ilk başlatılan sunucu otomatik olarak **Lider** olur.

```bash
java -jar family-grpc-1.0-SNAPSHOT.jar
```

#### Terminal 2, 3, ... – Üye Sunucular

> Sonradan başlatılan sunucular otomatik olarak lidere bağlanır ve **Üye** statüsüne geçer.

```bash
java -jar family-grpc-1.0-SNAPSHOT.jar
```

---

### 🧪 4. İstemci Testi

HaToKuSe test istemcisi kullanılarak sisteme **SET komutları** gönderilir.

* Lider, mesajları `tolerance.conf` ayarlarına göre üyelere dağıtır.
* Sistem yük testi ve dayanıklılık senaryoları için uygundur.

---

## 📊 Performans Analizi

### *Buffered vs Unbuffered IO*

Ödev kapsamında disk yazma yöntemlerinin sistem performansına etkisi ölçülmüştür.

| Yazma Modu        | Config Ayarı         | Ortalama RTT | Açıklama                                                             |
| ----------------- | -------------------- | ------------ | -------------------------------------------------------------------- |
| **Buffered IO**   | `USE_BUFFERED=true`  | ~0 ms        | Veriler RAM üzerinde tamponlanır, çok yüksek performans              |
| **Unbuffered IO** | `USE_BUFFERED=false` | ~12 ms       | `fsync` ile fiziksel diske zorlanır, veri güvenliği maksimize edilir |

> Mekanik disk gecikmesini simüle etmek için Unbuffered modda yapay gecikme eklenmiştir.

---

## 🧪 Test Senaryoları

### 🔹 Senaryo 1: Hata Toleransı

* `TOLERANCE=2`
* 1 Lider + 2 Üye çalıştırıldı
* Bir üye kapatılsa bile verinin diğer sunucularda korunduğu gözlemlendi

### 🔹 Senaryo 2: Yük Dengeleme

* `TOLERANCE=1`
* 1 Lider + 4 Üye çalıştırıldı
* Mesajların üyeler arasında rastgele ve dengeli dağıtıldığı raporlandı

---

## 📂 Proje Yapısı

```text
src/
 └── main/
     ├── proto/
     │   └── family.proto        # gRPC protokol tanımları
     └── java/
         ├── NodeMain.java       # Lider/Üye mantığı ve konfigürasyon
         └── FamilyServiceImpl.java  # gRPC servisleri ve disk IO işlemleri
```

---

## 📌 Kullanılan Teknolojiler

* Java
* gRPC
* Protocol Buffers
* Maven
* Dağıtık Sistem Mimarileri

---

## ✅ Sonuç

HaToKuSe, modern RPC teknolojileri kullanılarak geliştirilmiş; hata toleransı, yük dengeleme ve IO performans farklarını başarıyla gösteren bir dağıtık sistem projesidir.

---

🛡️ **HaToKuSe – Hata Toleranslı Kuyruk Servisi**
