# WA Mesaj Geçmişi (WhatsApp Bildirim Kaydedici)

WhatsApp'tan gelen bildirimleri (grup ve özel sohbet) anında yakalayıp local
veritabanına kaydeden bir Android uygulaması. Amaç: bir mesaj sohbette
silindiğinde bile, bildirim üzerinden alınan kopyasının uygulamada kalması.

## Nasıl Çalışır?

Android'in `NotificationListenerService` API'si kullanılıyor. WhatsApp yeni
bir mesaj geldiğinde sistem bildirimi oluşturur; bu bildirim genelde
`MessagingStyle` formatındadır ve içinde o ana kadarki mesajların listesi
(gönderen + metin + zaman) bulunur. Uygulama bu bilgiyi bildirim
oluşturulduğu anda okuyup Room veritabanına yazıyor.

**Önemli sınırlama:** Bu yöntem yalnızca *bildirim tetiklendikten sonra*
silinen mesajlar için işe yarar. Karşı taraf mesajı gönderir göndermez
(bildirim oluşmadan / senin telefonuna ulaşmadan) silerse, bu yöntemle
görülemez. Ayrıca medya dosyaları (foto/video) genelde bildirimde önizleme
metni olarak gelmediği için sadece metin mesajları güvenilir şekilde
yakalanıyor.

## Medya (Fotoğraf / Video) Yedekleme

Metin mesajlarının yanı sıra uygulama artık WhatsApp'a gelen fotoğraf ve
videoları da otomatik yedekliyor. Bunu bildirim üzerinden değil, dosya
sistemi üzerinden yapıyor:

- Android 11+ üzerinde WhatsApp medyayı `Android/media/com.whatsapp/WhatsApp/Media/`
  altına indiriyor; bu klasör sisteme (`MediaStore`) otomatik taranıyor.
- Uygulama bir arka plan servisiyle (`MediaWatcherService`) bu klasörü
  izliyor, yeni bir dosya düştüğü an kendi private klasörüne kopyalıyor.
- Ana ekrandaki "📷 Medya Galerisi" butonundan yedeklenen tüm fotoğraf/
  videoları grid görünümünde görebilirsin.
- Servis, telefon yeniden başladığında da otomatik ayağa kalkıyor
  (`BootReceiver`).

**Sınırlama:** Karşı taraf medyayı gönderir göndermez, WhatsApp henüz
indirmeden (senin telefonuna tam olarak inmeden) silerse yakalanamaz —
yakalama, dosya telefonuna fiziksel olarak indiği ana dayanıyor. Ayrıca
"Otomatik indir" ayarın kapalıysa (Ayarlar > Depolama ve Veri) medya
telefonuna hiç inmez, bu durumda uygulama da yakalayamaz — WhatsApp
ayarlarından fotoğraf/video için otomatik indirmeyi açık tutman gerekiyor.

**Kapsam dışı bırakılan bir konu:** Kilitli/gizli sohbetler (Chat Lock)
WhatsApp tarafından bilerek bildirimde gizleniyor (içerik gösterilmiyor).
Bu, telefon sahibinin bilerek uyguladığı bir gizlilik önlemi olduğu için
bu özelliği aşan bir yöntem eklemedim.

## Kurulum (Android Studio ile)

1. Bu klasörü Android Studio ile aç (`File > Open`).
2. Gradle senkronizasyonunun bitmesini bekle.
3. Telefonunu USB ile bağla (Geliştirici Seçenekleri > USB Hata Ayıklama açık
   olmalı) veya bir emülatör kullan.
4. `Run` butonuna bas, uygulama telefona kurulacak.

## İlk Çalıştırma Sonrası

1. Uygulamayı aç, üstte turuncu bir uyarı göreceksin: "Bildirim erişim izni
   verilmedi".
2. "İzin Ver" butonuna bas, açılan sistem ayarlarında **WA Mesaj Geçmişi**
   uygulamasını bul ve bildirim erişimine izin ver.
3. Uygulama ayrıca fotoğraf/video izni isteyecek — bunu da onayla, medya
   yedekleme servisi bu izinle başlıyor.
4. Ana ekrana dön. Artık WhatsApp'a gelen her mesaj otomatik olarak
   listede birikmeye başlayacak.
5. Üstteki arama kutusundan sohbet adına, kişi adına veya mesaj içeriğine
   göre filtreleme yapabilirsin.
6. "📷 Medya Galerisi" butonundan yedeklenen fotoğraf/videoları görebilirsin.
7. "Tüm Kayıtları Temizle" / "Tüm Medyayı Temizle" butonları ilgili
   geçmişi tamamen siler (geri alınamaz).

## Gizlilik Notu

Tüm veriler yalnızca telefonun kendi local veritabanında (`notiflog.db`)
tutulur, hiçbir sunucuya gönderilmez. Uygulamayı kaldırırsan veritabanı da
silinir.

## Genişletme Fikirleri (ileride eklenebilir)

- Konuşma bazlı gruplama (her sohbet ayrı ekranda)
- Belirli bir süre sonra otomatik silme (örn. 30 gün)
- Yedekleme / dışa aktarma (CSV, JSON)
- Karanlık tema desteği
